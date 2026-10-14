// Parcours 22-order-quotes.cy.ts : 22 - Devis fournisseur versionné.
// La boutique demande SANS remise ; le fournisseur modifie les lignes, pose les
// remises (lignes % + globale %) et envoie le devis (notification shop).
// Prérequis : services live, utilisateurs seedés via cy.ensureTestUsers(), API live.
// Ne pas lancer sans go explicite : suite e2e live uniquement.
// @ts-nocheck
const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

function hdr(token: string) {
  return { Authorization: `Bearer ${token}` };
}

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
      name: `Produit Devis ${Date.now()}`, sku: `DEV-E2E-${Date.now()}`,
      description: 'E2E devis', unitPrice: 1000, currency: 'TND',
      quantity: 100, minQuantity: 1, vatRate: 19,
    })).then((r) => {
      expect(r.status).to.be.oneOf([200, 201]);
      ctx.productId = r.body.id;
    })
    .then(() => cy.wrap(ctx));
}

function draftOrder(ctx: any) {
  return cy.apiPost(ctx.shopToken, '/api/orders', {
    supplierId: ctx.covaleId, shopId: ctx.shopAbdelslamId,
    asapPayment: false, currency: 'TND', notes: 'E2E devis sans remise',
    items: [{ productId: ctx.productId, quantity: 2, discount: 0 }],
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201]);
    return cy.wrap(r.body.id as string);
  });
}

function postQuote(token: string, orderId: string, body: any) {
  return cy.request({
    method: 'POST',
    url: `${API()}/api/orders/${orderId}/quote`,
    headers: hdr(token),
    body,
    failOnStatusCode: false,
  });
}

describe('22 - Devis fournisseur', () => {
  before(() => cy.ensureTestUsers());

  it('fournisseur envoie un devis v1 avec remises (lignes % + globale %)', () => {
    setupCtx().then((ctx) => draftOrder(ctx).then((id) =>
      postQuote(ctx.supplierToken, id, {
        items: [{ productId: ctx.productId, quantity: 2, discount: 10 }],
        globalDiscount: 5,
      }).then((r) => {
        expect(r.status).to.eq(200, JSON.stringify(r.body));
        expect(r.body.version).to.eq(1);
        expect(r.body.globalDiscount).to.eq(5);
        // 2x1000=2000, -10% ligne=1800, -5% global=1710 HT, TVA 19%=324.90
        expect(r.body.subtotal).to.eq(1710);
        expect(r.body.total).to.eq(2034.9);
      })));
  });

  it('deuxième devis incrémente la version, historique complet', () => {
    setupCtx().then((ctx) => draftOrder(ctx).then((id) =>
      postQuote(ctx.supplierToken, id, {
        items: [{ productId: ctx.productId, quantity: 1, discount: 0 }],
        globalDiscount: 0,
      }).then(() => postQuote(ctx.supplierToken, id, {
        items: [{ productId: ctx.productId, quantity: 1, discount: 50 }],
        globalDiscount: 0,
      })).then((r) => {
        expect(r.body.version).to.eq(2);
        return cy.apiGet(ctx.shopToken, `/api/orders/${id}/quotes`);
      }).then((h) => {
        expect(h.status).to.eq(200);
        expect(h.body.length).to.eq(2);
        expect(h.body[0].version).to.eq(1);
        expect(h.body[1].version).to.eq(2);
      })));
  });

  it('boutique ne peut pas envoyer de devis (403)', () => {
    setupCtx().then((ctx) => draftOrder(ctx).then((id) =>
      postQuote(ctx.shopToken, id, {
        items: [{ productId: ctx.productId, quantity: 1, discount: 0 }],
        globalDiscount: 0,
      }).then((r) => {
        expect(r.status).to.eq(403);
      })));
  });

  it('remise hors borne refusée (400/409)', () => {
    setupCtx().then((ctx) => draftOrder(ctx).then((id) =>
      postQuote(ctx.supplierToken, id, {
        items: [{ productId: ctx.productId, quantity: 1, discount: 120 }],
        globalDiscount: 0,
      }).then((r) => {
        expect(r.status).to.be.oneOf([400, 409]);
      })));
  });

  it('boutique voit ses devis sur le détail commande', () => {
    setupCtx().then((ctx) => draftOrder(ctx).then((id) =>
      postQuote(ctx.supplierToken, id, {
        items: [{ productId: ctx.productId, quantity: 1, discount: 0 }],
        globalDiscount: 0,
      }).then(() => {
        cy.loginAsShopAdmin();
        cy.visit(`/dashboard/shop/orders/${id}`);
        cy.dismissOverlays();
        cy.contains('Devis reçus', { timeout: 15000 }).should('exist');
        cy.contains('Devis n°1').should('exist');
      })));
  });
});
