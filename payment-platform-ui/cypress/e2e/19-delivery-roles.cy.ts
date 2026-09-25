// Livraison multi-rôles :
// - l'admin fournisseur voit et traite les livraisons (page + accept + deliver) ;
// - le sélecteur « Personne qui a reçu la livraison » propose aussi les ADMINS
//   boutique (pas seulement les agents) — fini le blocage « Aucun agent disponible ».
describe('19 - Delivery: supplier admin delivers, shop admin receives', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  before(() => cy.ensureTestUsers());

  it('supplier admin delivers to shop admin via UI modal', () => {
    cy.getTestCtx().then((ctx) => {
      const shopId = ctx.shops.ali.id;
      const supplierId = ctx.suppliers.covale.id;
      cy.apiLogin(ctx.users.supplierAdmin.username, 'test1234').then((supplierToken) => {
        const authSup = { Authorization: `Bearer ${supplierToken}` };
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
                name: `Roles E2E ${Date.now()}`, sku: `ROLE-${Date.now()}`,
                description: 'Produit test roles', unitPrice: 10,
                currency: 'TND', quantity: 500, minQuantity: 5,
              },
            }).then((r) => r.body);
          ensureProduct.then((product: any) => {
            cy.apiLogin(ctx.users.shopAli.username, 'test1234').then((shopToken) => {
              const authShop = { Authorization: `Bearer ${shopToken}` };
              cy.request({
                method: 'POST', url: `${API()}/api/orders`,
                headers: authShop,
                body: {
                  supplierId, shopId, asapPayment: false, currency: 'TND',
                  notes: 'Delivery roles test',
                  items: [{ productId: product.id, quantity: 2, discount: 0 }],
                },
              }).then((created) => {
                expect(created.status).to.be.oneOf([200, 201]);
                const orderId = created.body.id as string;
                const reference = created.body.reference as string;
                // Fournisseur : confirm -> prepare -> ready -> assign -> accept (par l'admin).
                cy.request({ method: 'POST', url: `${API()}/api/orders/${orderId}/confirm`, headers: authSup, body: {} });
                cy.request({ method: 'POST', url: `${API()}/api/orders/${orderId}/prepare`, headers: authSup, body: {} });
                cy.request({ method: 'POST', url: `${API()}/api/orders/${orderId}/ready`, headers: authSup, body: {} });
                cy.apiLogin(ctx.users.supplierAgent1.username, 'test1234').then((agentToken) => {
                  cy.request({
                    method: 'GET', url: `${API()}/api/auth/me`,
                    headers: { Authorization: `Bearer ${agentToken}` },
                  }).then((me) => {
                    const agentId = me.body.id as string;
                    cy.request({
                      method: 'POST', url: `${API()}/api/orders/${orderId}/assign-delivery`,
                      headers: authSup, body: { agentId, plannedDeliveryDate: '2026-09-15' },
                    });
                    // Accept PAR L'ADMIN (nouveau droit).
                    cy.request({
                      method: 'POST', url: `${API()}/api/orders/${orderId}/accept-delivery`,
                      headers: authSup, body: { accepted: true },
                    }).then((r) => expect(r.body.status).to.eq('DELIVERY_ACCEPTED'));
                    cy.request({
                      method: 'POST', url: `${API()}/api/orders/${orderId}/confirm-delivery`,
                      headers: authSup, body: { confirmedDate: '2026-09-14' },
                    });
                    // Id de l'admin DE LA BOUTIQUE COMMANDITAIRE (futur destinataire).
                    cy.apiLogin(ctx.users.shopAli.username, 'test1234').then((shopAdminToken) => {
                      cy.request({
                        method: 'GET', url: `${API()}/api/auth/me`,
                        headers: { Authorization: `Bearer ${shopAdminToken}` },
                      }).then((shopMe) => {
                        const shopAdminId = shopMe.body.id as string;
                        // UI admin fournisseur : la livraison est visible…
                        cy.login(ctx.users.supplierAdmin.username, 'test1234').then(() => {
                          cy.visit('/dashboard/supplier/deliveries');
                          cy.contains(reference, { timeout: 15000 }).should('exist');
                          // …le modal propose l'admin boutique…
                          cy.contains('tr', reference).within(() => {
                            cy.contains('button', 'Marquer livré').click({ force: true });
                          });
                          cy.contains('h3', 'Confirmer la livraison').should('be.visible');
                          cy.get('select[name="receivedBy"]').should('be.visible');
                          cy.get('select[name="receivedBy"] option')
                            .contains('(admin boutique)')
                            .should('exist');
                          // …sélection + confirmation.
                          cy.get('select[name="receivedBy"]').select(shopAdminId);
                          cy.contains('button', 'Confirmer la livraison').click();
                          cy.contains('Livraison confirmée', { timeout: 10000 }).should('exist');
                          // Preuve API : receivedBy = admin boutique.
                          cy.request({
                            method: 'GET', url: `${API()}/api/orders/${orderId}`,
                            headers: authSup,
                          }).then((r) => {
                            expect(r.body.status).to.eq('DELIVERED');
                            expect(r.body.receivedBy).to.eq(shopAdminId);
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
});
