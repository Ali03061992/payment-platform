// Parcours 22-order-quotes.cy.ts : 22 - Demandes de devis découplées.
// La boutique crée une DEMANDE (zéro remise, pas une commande) ; le fournisseur
// chiffre via l'application (versions) ; la boutique accepte (crée la commande).
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
      name: `Produit Demande ${Date.now()}`, sku: `DEM-E2E-${Date.now()}`,
      description: 'E2E demande devis', unitPrice: 1000, currency: 'TND',
      quantity: 100, minQuantity: 1, vatRate: 19,
    })).then((r) => {
      expect(r.status).to.be.oneOf([200, 201]);
      ctx.productId = r.body.id;
    })
    .then(() => cy.wrap(ctx));
}

function createDemande(ctx: any) {
  return cy.request({
    method: 'POST',
    url: `${API()}/api/quotes/requests`,
    headers: hdr(ctx.shopToken),
    body: {
      supplierId: ctx.covaleId, notes: 'E2E demande sans remise',
      items: [{ productId: ctx.productId, quantity: 2 }],
    },
    failOnStatusCode: false,
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201], JSON.stringify(r.body));
    return cy.wrap(r.body);
  });
}

function sendDevis(ctx: any, requestId: string, discount: number, globalDiscount: number) {
  return cy.request({
    method: 'POST',
    url: `${API()}/api/quotes/requests/${requestId}/quotes`,
    headers: hdr(ctx.supplierToken),
    body: { items: [{ productId: ctx.productId, quantity: 2, discount }], globalDiscount },
    failOnStatusCode: false,
  });
}

describe('22 - Demandes de devis découplées', () => {
  before(() => cy.ensureTestUsers());

  it('boutique crée une demande SANS remise (pas de commande)', () => {
    setupCtx().then((ctx) => createDemande(ctx).then((d: any) => {
      expect(d.reference).to.match(/^DEV-/);
      expect(d.status).to.eq('PENDING');
      // Aucune commande créée : la liste boutique ne contient pas de brouillon lié.
      cy.apiGet(ctx.shopToken, '/api/orders').then((o: any) => {
        const items = Array.isArray(o.body) ? o.body : (o.body?.items || o.body?.content || []);
        expect(items.find((x: any) => x.notes === 'E2E demande sans remise')).to.be.undefined;
      });
    }));
  });

  it('fournisseur chiffre v1 (lignes % + globale %), demande QUOTED', () => {
    setupCtx().then((ctx) => createDemande(ctx).then((d: any) =>
      sendDevis(ctx, d.id, 10, 5).then((r) => {
        expect(r.status).to.eq(200, JSON.stringify(r.body));
        expect(r.body.version).to.eq(1);
        // 2x1000=2000, -10% ligne=1800, -5% global=1710 HT, TVA 19%=324.90
        expect(r.body.subtotal).to.eq(1710);
        expect(r.body.total).to.eq(2034.9);
        return cy.apiGet(ctx.shopToken, `/api/quotes/requests/${d.id}`);
      }).then((g: any) => {
        expect(g.body.status).to.eq('QUOTED');
        expect(g.body.quotes.length).to.eq(1);
      })));
  });

  it('deuxième devis incrémente, boutique accepte → commande CONFIRMÉE', () => {
    setupCtx().then((ctx) => createDemande(ctx).then((d: any) =>
      sendDevis(ctx, d.id, 0, 0).then(() => sendDevis(ctx, d.id, 50, 0)).then((r) => {
        expect(r.body.version).to.eq(2);
        return cy.request({
          method: 'POST',
          url: `${API()}/api/quotes/quotes/${r.body.id}/accept`,
          headers: hdr(ctx.shopToken),
          failOnStatusCode: false,
        });
      }).then((a: any) => {
        expect(a.status).to.eq(200, JSON.stringify(a.body));
        expect(a.body.status).to.eq('CONFIRMED');
        expect(a.body.total).to.eq(1190);
        return cy.apiGet(ctx.shopToken, `/api/quotes/requests/${d.id}`);
      }).then((g: any) => {
        expect(g.body.status).to.eq('ACCEPTED');
      })));
  });

  it('boutique tierce : acceptation refusée (403)', () => {
    setupCtx().then((ctx) => createDemande(ctx).then((d: any) =>
      sendDevis(ctx, d.id, 0, 0).then((r) =>
        cy.apiLogin('ali.e2e').then((aliToken) => cy.request({
          method: 'POST',
          url: `${API()}/api/quotes/quotes/${r.body.id}/accept`,
          headers: hdr(aliToken),
          failOnStatusCode: false,
        })).then((a: any) => {
          expect(a.status).to.eq(403);
        }))));
  });

  it('remise hors borne refusée (400/409)', () => {
    setupCtx().then((ctx) => createDemande(ctx).then((d: any) =>
      sendDevis(ctx, d.id, 120, 0).then((r) => {
        expect(r.status).to.be.oneOf([400, 409]);
      })));
  });

  it('boutique annule sa demande', () => {
    setupCtx().then((ctx) => createDemande(ctx).then((d: any) =>
      cy.request({
        method: 'PATCH',
        url: `${API()}/api/quotes/requests/${d.id}/cancel`,
        headers: hdr(ctx.shopToken),
        failOnStatusCode: false,
      }).then((r) => {
        expect(r.status).to.eq(200);
        expect(r.body.status).to.eq('CANCELLED');
      })));
  });
});
