// ASAP : création commande (asapPayment=true) -> confirmation fournisseur ->
// livraison créée (assign/accept/confirm/deliver) -> à la livraison, UN paiement
// auto est créé (pendingTotal/count +delta du total commande) ; le second appel
// (accept-asap) ne crée PAS de doublon (idempotence asap-<orderId>).
describe('18 - ASAP: order -> delivery -> auto-payment', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  before(() => cy.ensureTestUsers());

  it('creates ASAP order, delivers it, and verifies exactly one auto-payment', () => {
    cy.getTestCtx().then((ctx) => {
      const shopId = ctx.shops.ali.id;
      const supplierId = ctx.suppliers.covale.id;
      const summaryUrl = `${API()}/api/payments/supplier-summary?supplierId=${supplierId}`;

      cy.apiLogin(ctx.users.supplierAdmin.username, 'test1234').then((supplierToken) => {
        const authSup = { Authorization: `Bearer ${supplierToken}` };
        // Produit avec stock (ou création).
        cy.request({
          method: 'GET', url: `${API()}/api/suppliers/${supplierId}/products`,
          headers: authSup,
        }).then((plist) => {
          const body = plist.body;
          const products = Array.isArray(body) ? body : body.items || [];
          const available = products.find((p: any) => (p.quantity - (p.reservedQty || 0)) >= 2);
          const ensureProduct = available
            ? cy.wrap(available)
            : cy.request({
              method: 'POST', url: `${API()}/api/suppliers/${supplierId}/products`,
              headers: authSup,
              body: {
                name: `ASAP E2E ${Date.now()}`, sku: `ASAP-${Date.now()}`,
                description: 'Produit test ASAP', unitPrice: 25.50,
                currency: 'TND', quantity: 500, minQuantity: 5,
              },
            }).then((r) => {
              expect(r.status).to.be.oneOf([200, 201]);
              return r.body;
            });
          ensureProduct.then((product: any) => {
            // Baseline summary.
            cy.request({ method: 'GET', url: summaryUrl, headers: authSup }).then((before) => {
              expect(before.status).to.eq(200);
              // 1. Création commande ASAP par la boutique.
              cy.apiLogin(ctx.users.shopAli.username, 'test1234').then((shopToken) => {
                const authShop = { Authorization: `Bearer ${shopToken}` };
                cy.request({
                  method: 'POST', url: `${API()}/api/orders`,
                  headers: authShop,
                  body: {
                    supplierId, shopId, asapPayment: true, currency: 'TND',
                    notes: 'ASAP auto-payment test',
                    items: [{ productId: product.id, quantity: 2, discount: 0 }],
                  },
                }).then((created) => {
                  expect(created.status).to.be.oneOf([200, 201]);
                  expect(created.body.asapPayment, 'asap flag').to.be.true;
                  const orderId = created.body.id as string;
                  // Référence = total calculé par le backend (remises incluses).
                  const orderTotal = Number(created.body.total);
                  expect(orderTotal, 'order total').to.be.greaterThan(0);
                  // 2. Confirmation fournisseur : CONFIRMED -> PREPARING -> READY.
                  cy.request({
                    method: 'POST', url: `${API()}/api/orders/${orderId}/confirm`,
                    headers: authSup, body: {},
                  }).then((r) => expect(r.body.status).to.eq('CONFIRMED'));
                  cy.request({
                    method: 'POST', url: `${API()}/api/orders/${orderId}/prepare`,
                    headers: authSup, body: {},
                  });
                  cy.request({
                    method: 'POST', url: `${API()}/api/orders/${orderId}/ready`,
                    headers: authSup, body: {},
                  }).then((r) => expect(r.body.status).to.eq('READY_FOR_DELIVERY'));
                  // 3. Livraison créée : assign -> accept -> confirm -> deliver.
                  cy.apiLogin(ctx.users.supplierAgent1.username, 'test1234').then((agentToken) => {
                    const authAgent = { Authorization: `Bearer ${agentToken}` };
                    cy.request({
                      method: 'GET', url: `${API()}/api/auth/me`, headers: authAgent,
                    }).then((me) => {
                      const agentId = me.body.id as string;
                      cy.request({
                        method: 'POST', url: `${API()}/api/orders/${orderId}/assign-delivery`,
                        headers: authSup,
                        body: { agentId, plannedDeliveryDate: '2026-09-15' },
                      });
                      cy.request({
                        method: 'POST', url: `${API()}/api/orders/${orderId}/accept-delivery`,
                        headers: authAgent, body: { accepted: true },
                      });
                      cy.request({
                        method: 'POST', url: `${API()}/api/orders/${orderId}/confirm-delivery`,
                        headers: authAgent, body: { confirmedDate: '2026-09-14' },
                      });
                      cy.request({
                        method: 'POST', url: `${API()}/api/orders/${orderId}/deliver`,
                        headers: authAgent, body: { receivedBy: agentId },
                      }).then((r) => expect(r.body.status).to.eq('DELIVERED'));
                      // 4. À la livraison : UN paiement auto créé.
                      cy.request({ method: 'GET', url: summaryUrl, headers: authSup }).then((after) => {
                        expect(after.status).to.eq(200);
                        expect(after.body.pendingTotal - before.body.pendingTotal).to.eq(orderTotal);
                        expect(after.body.pendingCount - before.body.pendingCount).to.eq(1);
                        // 5. accept-asap ne crée PAS de doublon (idempotence).
                        cy.request({
                          method: 'POST', url: `${API()}/api/orders/${orderId}/accept-asap`,
                          headers: authShop, body: {},
                        }).then((r) => expect(r.body.status).to.eq('ACCEPTED'));
                        cy.request({ method: 'GET', url: summaryUrl, headers: authSup }).then((final) => {
                          expect(final.body.pendingTotal - before.body.pendingTotal).to.eq(orderTotal);
                          expect(final.body.pendingCount - before.body.pendingCount).to.eq(1);
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
    });
  });
});
