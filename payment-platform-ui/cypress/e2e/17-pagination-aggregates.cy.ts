// B5 : listes bornées (page/size, max 100) + supplier-summary en agrégats SQL.
// - Les enveloppes {items, totalElements, ...} sont vérifiées ici (le plafonnement
//   à 100 est prouvé côté backend : 105 users seedés -> items <= 100).
// - Le summary est vérifié par DELTAS (avant/après) pour rester insensible aux
//   données créées par les autres specs.
describe('17 - B5: Pagination envelopes', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  before(() => cy.ensureTestUsers());

  it('GET /api/users returns a bounded page envelope', () => {
    cy.apiLogin('system.admin', '@PAssword012345').then((token) => {
      cy.request({
        method: 'GET',
        url: `${API()}/api/users?page=0&size=1`,
        headers: { Authorization: `Bearer ${token}` },
      }).then((resp) => {
        expect(resp.status).to.eq(200);
        expect(resp.body.items, 'items').to.be.an('array').with.lengthOf(1);
        expect(resp.body.totalElements, 'totalElements').to.be.a('number');
      });
    });
  });

  it('GET /api/admin/suppliers honors size and stays bounded', () => {
    cy.apiLogin('system.admin', '@PAssword012345').then((token) => {
      cy.request({
        method: 'GET',
        url: `${API()}/api/admin/suppliers?page=0&size=500`,
        headers: { Authorization: `Bearer ${token}` },
      }).then((resp) => {
        expect(resp.status).to.eq(200);
        expect(resp.body.items, 'items').to.be.an('array');
        expect(resp.body.items.length, 'bounded page').to.be.at.most(100);
      });
    });
  });
});

describe('17 - B5: Supplier summary via SQL aggregates', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  before(() => cy.ensureTestUsers());

  it('summary deltas match created payments (no full load)', () => {
    cy.getTestCtx().then((ctx) => {
      const shopId = ctx.shops.ali.id;
      const supplierId = ctx.suppliers.covale.id;
      cy.apiLogin(ctx.users.supplierAdmin.username, 'test1234').then((supplierToken) => {
        const summaryUrl = `${API()}/api/payments/supplier-summary?supplierId=${supplierId}`;
        const authSup = { Authorization: `Bearer ${supplierToken}` };
        cy.request({ method: 'GET', url: summaryUrl, headers: authSup }).then((before) => {
          expect(before.status).to.eq(200);
          cy.apiLogin(ctx.users.shopAli.username, 'test1234').then((shopToken) => {
            const authShop = { Authorization: `Bearer ${shopToken}` };
            const payload = { shopId, supplierId, currency: 'TND' };
            // Paiement 1 (sera confirmé) + paiement 2 (restera pending).
            cy.request({
              method: 'POST', url: `${API()}/api/payments`,
              headers: authShop, body: { ...payload, amount: 100 },
            }).then((r1) => {
              expect(r1.status).to.eq(201);
              cy.request({
                method: 'POST', url: `${API()}/api/payments`,
                headers: authShop, body: { ...payload, amount: 50 },
              }).then((r2) => {
                expect(r2.status).to.eq(201);
                cy.request({
                  method: 'POST', url: `${API()}/api/payments/${r1.body.id}/confirm`,
                  headers: authSup,
                }).then((rc) => {
                  expect(rc.status).to.eq(200);
                  cy.request({ method: 'GET', url: summaryUrl, headers: authSup }).then((after) => {
                    expect(after.status).to.eq(200);
                    expect(after.body.confirmedTotal - before.body.confirmedTotal).to.eq(100);
                    expect(after.body.confirmedCount - before.body.confirmedCount).to.eq(1);
                    expect(after.body.pendingTotal - before.body.pendingTotal).to.eq(50);
                    expect(after.body.pendingCount - before.body.pendingCount).to.eq(1);
                  });
                });
              });
            });
          });
        });
      });
    });
  });
});
