// Parcours 23-quote-interfaces.cy.ts : 23 - Interfaces dédiées devis.
// Boutique : "Demander un devis" + "Mes devis" (accepter/télécharger/annuler).
// Fournisseur : page "Devis" (demandes à chiffrer + envoi via l'application).
// Prérequis : services live, utilisateurs seedés via cy.ensureTestUsers(), API live.
// Ne pas lancer sans go explicite : suite e2e live uniquement.
// @ts-nocheck
const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

function setupCtx() {
  const ctx: any = {};
  return cy.apiLogin('system.admin').then((t) => { ctx.adminToken = t; })
    .then(() => cy.apiLogin('covale.admin.e2e')).then((t) => { ctx.supplierToken = t; })
    .then(() => cy.apiLogin('abdelslam.e2e')).then((t) => { ctx.shopToken = t; })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/suppliers')).then((r) => {
      const body: any = r.body;
      const list = Array.isArray(body) ? body : (body?.items || body?.content || []);
      ctx.covaleId = list.find((s: any) => s.name === 'Covale E2E')?.id;
    })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/shops')).then((r) => {
      const body: any = r.body;
      const list = Array.isArray(body) ? body : (body?.items || body?.content || []);
      ctx.shopAbdelslamId = list.find((s: any) => s.name === 'Abdelslam Tunis E2E')?.id;
    })
    .then(() => cy.apiPost(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`, {
      name: `Produit UI ${Date.now()}`, sku: `UI-E2E-${Date.now()}`,
      description: 'E2E interfaces devis', unitPrice: 500, currency: 'TND',
      quantity: 100, minQuantity: 1, vatRate: 19,
    })).then((r) => {
      expect(r.status).to.be.oneOf([200, 201]);
      ctx.productId = r.body.id;
    })
    .then(() => cy.wrap(ctx));
}

describe('23 - Interfaces dédiées devis', () => {
  before(() => cy.ensureTestUsers());

  it('boutique : page demande sans remise, envoi crée une demande', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/quotes/request');
    cy.dismissOverlays();
    cy.get('input[name^="discount_"]').should('not.exist');
    cy.get('#globalDiscount').should('not.exist');
    cy.get('#supplier', { timeout: 15000 }).find('option').its('length').should('be.gt', 1);
    cy.get('#supplier').select(1);
    cy.contains('button', 'Ajouter', { timeout: 15000 }).first().click({ force: true });
    cy.contains('button', 'Envoyer la demande').click();
    cy.url({ timeout: 15000 }).should('include', '/dashboard/shop/quotes');
    cy.contains('DEV-', { timeout: 15000 }).should('exist');
  });

  it('fournisseur : page devis, chiffrage et envoi via modal', () => {
    setupCtx().then((ctx) => {
      cy.request({
        method: 'POST',
        url: `${API()}/api/quotes/requests`,
        headers: { Authorization: `Bearer ${ctx.shopToken}` },
        body: {
          supplierId: ctx.covaleId, notes: 'E2E UI chiffrage',
          items: [{ productId: ctx.productId, quantity: 1 }],
        },
        failOnStatusCode: false,
      }).then((d) => {
        expect(d.status).to.be.oneOf([200, 201]);
        cy.loginAsSupplierAdmin();
        cy.visit('/dashboard/supplier/quotes');
        cy.dismissOverlays();
        cy.contains('Demandes de devis', { timeout: 15000 }).should('exist');
        cy.contains(d.body.reference).should('exist');
        cy.contains('button', 'Chiffrer').first().click();
        cy.get('#quote-modal-title', { timeout: 15000 }).should('exist');
        cy.get('input[name="quoteDiscount_0"]').clear().type('10');
        cy.get('#quoteGlobalDiscount').clear().type('5');
        cy.contains('button', 'Envoyer le devis').click();
        cy.get('#quote-modal-title').should('not.exist');
      });
    });
  });

  it('boutique : mes devis, acceptation crée la commande', () => {
    setupCtx().then((ctx) => {
      cy.request({
        method: 'POST',
        url: `${API()}/api/quotes/requests`,
        headers: { Authorization: `Bearer ${ctx.shopToken}` },
        body: {
          supplierId: ctx.covaleId, notes: 'E2E UI acceptation',
          items: [{ productId: ctx.productId, quantity: 1 }],
        },
        failOnStatusCode: false,
      }).then((d) => cy.request({
        method: 'POST',
        url: `${API()}/api/quotes/requests/${d.body.id}/quotes`,
        headers: { Authorization: `Bearer ${ctx.supplierToken}` },
        body: { items: [{ productId: ctx.productId, quantity: 1, discount: 20 }], globalDiscount: 0 },
        failOnStatusCode: false,
      })).then(() => {
        cy.loginAsShopAdmin();
        cy.visit('/dashboard/shop/quotes');
        cy.dismissOverlays();
        cy.contains('Mes devis', { timeout: 15000 }).should('exist');
        cy.contains('Devis n°1').should('exist');
        cy.contains('button', "J'accepte ce devis").first().click();
        cy.contains('button', 'Valider', { timeout: 15000 }).first().click({ force: true });
        cy.url({ timeout: 20000 }).should('match', /\/dashboard\/shop\/orders\/[\w-]+/);
      });
    });
  });
});
