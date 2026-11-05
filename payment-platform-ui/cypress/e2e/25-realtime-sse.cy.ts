// Parcours 25-realtime-sse.cy.ts : 25 - Temps réel SSE sur les pages.
// Une mutation effectuée par un autre utilisateur doit rafraîchir la page
// ouverte via le flux SSE + invalidation du store, SANS rechargement.
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
    .then(() => cy.apiPost(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`, {
      name: `Produit SSE ${Date.now()}`, sku: `SSE-E2E-${Date.now()}`,
      description: 'E2E temps réel SSE', unitPrice: 250, currency: 'TND',
      quantity: 100, minQuantity: 1, vatRate: 19,
    })).then((r) => {
      expect(r.status).to.be.oneOf([200, 201]);
      ctx.productId = r.body.id;
    })
    .then(() => cy.wrap(ctx));
}

describe('25 - Temps réel SSE', () => {
  before(() => cy.ensureTestUsers());

  it('une mutation sans notification (catégorie) apparaît aussi sans reload', () => {
    setupCtx().then((ctx) => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/supplier/categories');
      cy.dismissOverlays();
      cy.get('table', { timeout: 15000 }).should('exist');
      cy.get('.sortable-th').first().click();
      cy.window().then((win) => { (win as any).__sseNoReload = true; });
      cy.wait(1500);
      const name = `000 SSE ${Date.now()}`;
      cy.request({
        method: 'POST',
        url: `${API()}/api/supplier/catalog/categories`,
        headers: { Authorization: `Bearer ${ctx.supplierToken}` },
        body: { supplierId: ctx.covaleId, name, code: `SSE${Date.now()}` },
        failOnStatusCode: false,
      }).then((r) => {
        expect(r.status).to.be.oneOf([200, 201], JSON.stringify(r.body));
        cy.contains('strong', name, { timeout: 30000 }).should('exist');
        cy.window().its('__sseNoReload').should('eq', true);
      });
    });
  });

  it('une demande de devis créée par la boutique apparaît sans reload chez le fournisseur', () => {
    setupCtx().then((ctx) => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/supplier/quotes');
      cy.dismissOverlays();
      cy.contains('Demandes de devis', { timeout: 15000 }).should('exist');
      cy.window().then((win) => { (win as any).__sseNoReload = true; });
      // Laisse le temps au flux SSE de s'ouvrir avant la mutation distante.
      cy.wait(1500);
      cy.request({
        method: 'POST',
        url: `${API()}/api/quotes/requests`,
        headers: { Authorization: `Bearer ${ctx.shopToken}` },
        body: {
          supplierId: ctx.covaleId, notes: 'E2E SSE sans reload',
          items: [{ productId: ctx.productId, quantity: 1 }],
        },
        failOnStatusCode: false,
      }).then((d) => {
        expect(d.status).to.be.oneOf([200, 201], JSON.stringify(d.body));
        cy.contains(d.body.reference, { timeout: 30000 }).should('exist');
        cy.window().its('__sseNoReload').should('eq', true);
      });
    });
  });
});
