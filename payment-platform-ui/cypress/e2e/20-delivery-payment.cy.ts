// Parcours 20-delivery-payment.cy.ts : 20 - Delivery: reception -> paiement auto; rejet -> annulation sans paiement; popup Valider/Non.
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), API live (Cypress.env apiUrl).
// Regle metier : commande assignee -> livraison ; reception confirmee (agent/boutique) -> paiement auto ; rejet -> commande CANCELLED, aucun paiement.
// Jamais d'alert() native : les confirmations passent par la popup [data-testid="confirm-dialog"] (Valider/Non).
const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

function createOrder(ctx: any, notes: string) {
  const items = [{ productId: ctx.productId, quantity: 2, discount: 0 }];
  return cy.apiPost(ctx.shopToken, '/api/orders', {
    supplierId: ctx.covaleId, shopId: ctx.shopAbdelslamId,
    asapPayment: false, currency: 'TND', notes, items,
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201], `Order creation failed: ${JSON.stringify(r.body)}`);
    return cy.wrap({ id: r.body.id as string, reference: r.body.reference as string, total: r.body.total as number });
  });
}

function advanceToReady(ctx: any, orderId: string) {
  return cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/confirm`, {}).then(() =>
    cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/prepare`, {})
  ).then(() =>
    cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/ready`, {})
  );
}

function advanceToInDelivery(ctx: any, orderId: string) {
  return advanceToReady(ctx, orderId).then(() =>
    cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/assign-delivery`, {
      agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
    })
  ).then(() =>
    cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/accept-delivery`, { accepted: true })
  ).then(() =>
    cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/confirm-delivery`, { confirmedDate: '2026-09-14' })
  );
}

function paymentsForOrder(token: string, orderId: string) {
  return cy.apiGet(token, '/api/payments').then((r) => {
    const body = r.body;
    const items = Array.isArray(body) ? body
      : Array.isArray(body?.items) ? body.items
      : Array.isArray(body?.content) ? body.content : [];
    return cy.wrap(items.filter((p: any) => p.orderId === orderId));
  });
}

function setupTestData() {
  const ctx: any = {};
  return cy.apiLogin('system.admin').then((t) => { ctx.adminToken = t; })
    .then(() => cy.apiLogin('covale.admin.e2e')).then((t) => { ctx.supplierToken = t; })
    .then(() => cy.apiLogin('covale.agent1.e2e')).then((t) => { ctx.agentToken = t; })
    .then(() => cy.apiLogin('abdelslam.e2e')).then((t) => { ctx.shopToken = t; })
    .then(() => {
      const me = (token: string) => cy.request({
        method: 'GET', url: `${API()}/api/auth/me`, headers: { Authorization: `Bearer ${token}` },
      }).then((r) => r.body);
      return me(ctx.agentToken).then((u) => { ctx.agentId = u.id; });
    })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/suppliers')).then((r) => {
      const body = r.body;
      const list = Array.isArray(body) ? body
        : Array.isArray(body?.content) ? body.content
        : Array.isArray(body?.items) ? body.items : [];
      ctx.covaleId = list.find((s: any) => s.name.includes('Covale E2E'))?.id;
    })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/shops')).then((r) => {
      const body = r.body;
      const list = Array.isArray(body) ? body
        : Array.isArray(body?.content) ? body.content
        : Array.isArray(body?.items) ? body.items : [];
      ctx.shopAbdelslamId = list.find((s: any) => s.name.includes('Abdelslam'))?.id;
    })
    .then(() => {
      if (!ctx.covaleId) return;
      return cy.apiGet(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`).then((r) => {
        const body = r.body;
        const products = Array.isArray(body) ? body
          : Array.isArray(body?.content) ? body.content
          : Array.isArray(body?.items) ? body.items : [];
        const available = products.find((p: any) => (p.quantity - (p.reservedQty || 0)) >= 10);
        ctx.productId = available ? available.id : null;
        if (!ctx.productId) {
          return cy.apiPost(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`, {
            name: `Produit E2E Pay ${Date.now()}`, sku: `PAY-E2E-${Date.now()}`,
            description: 'Produit pour tests delivery-payment', unitPrice: 25.50,
            currency: 'TND', quantity: 500, minQuantity: 5,
          }).then((r2) => {
            ctx.productId = r2.body?.id || r2.body?.productId || null;
          });
        }
      });
    })
    .then(() => cy.wrap(ctx));
}

describe('20 - Delivery: reception confirmee cree le paiement', () => {
  before(() => cy.ensureTestUsers());

  it('deliver (agent) cree un paiement PENDING du montant commande', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Delivery creates payment').then((order) => {
          return advanceToInDelivery(ctx, order.id).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${order.id}/deliver`, { receivedBy: ctx.agentId })
          ).then((r) => {
            expect(r.body.status).to.eq('DELIVERED');
          }).then(() =>
            paymentsForOrder(ctx.supplierToken, order.id)
          ).then((found: any) => {
            expect(found.length).to.eq(1);
            expect(found[0].status).to.eq('PENDING');
            expect(Number(found[0].amount)).to.eq(Number(order.total));
          });
        });
      });
    });
  });

  it('deliver + accept boutique garde un seul paiement (idempotence)', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Single payment check').then((order) => {
          return advanceToInDelivery(ctx, order.id).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${order.id}/deliver`, { receivedBy: ctx.agentId })
          ).then(() =>
            cy.apiPost(ctx.shopToken, `/api/orders/${order.id}/accept`, {})
          ).then((r) => {
            expect(r.body.status).to.eq('ACCEPTED');
          }).then(() =>
            paymentsForOrder(ctx.supplierToken, order.id)
          ).then((found: any) => {
            expect(found.length).to.eq(1);
          });
        });
      });
    });
  });

  it('rejet livraison annule commande, montant solde, aucun paiement', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Reject cancels all').then((order) => {
          return advanceToInDelivery(ctx, order.id).then(() =>
            cy.apiPost(ctx.supplierToken, `/api/orders/${order.id}/delivery-reject`, { reason: 'Colis endommagé' })
          ).then((r) => {
            expect(r.body.status).to.eq('CANCELLED');
            expect(r.body.deliveryRejectionReason).to.eq('Colis endommagé');
          }).then(() =>
            paymentsForOrder(ctx.supplierToken, order.id)
          ).then((found: any) => {
            expect(found).to.have.length(0);
          });
        });
      });
    });
  });
});

describe('20 - Delivery: acces boutique (admin = boutique, agent = assignees)', () => {
  before(() => cy.ensureTestUsers());

  it('shop admin voit les livraisons de sa boutique, pas celles des autres', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Shop visibility').then((order) => {
          return advanceToInDelivery(ctx, order.id).then(() =>
            cy.apiLogin('ali.e2e').then((aliToken) =>
              cy.apiGet(aliToken, '/api/orders/my-deliveries').then((other) => {
                const items = Array.isArray(other.body) ? other.body : [];
                expect(items.map((o: any) => o.id)).to.not.include(order.id);
                return cy.apiGet(ctx.shopToken, '/api/orders/my-deliveries');
              }).then((mine) => {
                const items = Array.isArray(mine.body) ? mine.body : [];
                expect(items.map((o: any) => o.id)).to.include(order.id);
              })
            )
          );
        });
      });
    });
  });

  it('agent voit ses livraisons assignees via my-deliveries', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Agent visibility').then((order) => {
          return advanceToInDelivery(ctx, order.id).then(() =>
            cy.apiGet(ctx.agentToken, '/api/orders/my-deliveries').then((r) => {
              const items = Array.isArray(r.body) ? r.body : [];
              expect(items.map((o: any) => o.id)).to.include(order.id);
            })
          );
        });
      });
    });
  });

  it('shop admin accepte la reception via API', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Shop accept').then((order) => {
          return advanceToInDelivery(ctx, order.id).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${order.id}/deliver`, { receivedBy: ctx.agentId })
          ).then(() =>
            cy.apiPost(ctx.shopToken, `/api/orders/${order.id}/accept`, {})
          ).then((r) => {
            expect(r.body.status).to.eq('ACCEPTED');
          });
        });
      });
    });
  });

  it('shop UI affiche les receptions en attente avec Accepter/Rejeter', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Shop UI reception').then((order) => {
        return advanceToInDelivery(ctx, order.id).then(() =>
          cy.apiPost(ctx.agentToken, `/api/orders/${order.id}/deliver`, { receivedBy: ctx.agentId })
        ).then(() => {
          cy.loginAsShopAdmin();
          cy.visit('/dashboard/shop/deliveries');
          cy.dismissOverlays();
          cy.contains('table tbody tr', order.reference, { timeout: 15000 }).within(() => {
            cy.contains('button', 'Accepter').should('exist');
            cy.contains('button', 'Rejeter').should('exist');
          });
          cy.contains('table tbody tr', order.reference).within(() => {
            cy.contains('button', 'Accepter').click();
          });
          cy.contains('Réception acceptée', { timeout: 10000 }).should('exist');
          cy.apiGet(ctx.shopToken, `/api/orders/${order.id}`).then((r) => {
            expect(r.body.status).to.eq('ACCEPTED');
          });
        });
      });
    });
  });
});

describe('20 - Delivery: notif boutique mene a la livraison (pas 403)', () => {
  before(() => cy.ensureTestUsers());

  it('clic notif livraison => page boutique, jamais 403', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Notif routing').then((order) => {
        return advanceToInDelivery(ctx, order.id).then(() =>
          cy.apiPost(ctx.agentToken, `/api/orders/${order.id}/deliver`, { receivedBy: ctx.agentId })
        ).then(() => {
          cy.loginAsShopAdmin();
          cy.visit('/dashboard');
          cy.dismissOverlays();
          cy.get('.notif-bell', { timeout: 15000 }).first().click();
          cy.contains('.notif-item', order.reference, { timeout: 30000 }).click();
          cy.url({ timeout: 15000 }).should('include', '/dashboard/shop/');
          cy.url().should('not.include', '/403');
        });
      });
    });
  });
});

describe('20 - Delivery: popup Valider/Non (jamais alert native)', () => {
  before(() => cy.ensureTestUsers());

  it('annulation via popup : Non conserve, Valider annule', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Popup cancel test').then((order) => {
        cy.loginAsSupplierAdmin();
        cy.visit('/dashboard/supplier/orders');
        cy.dismissOverlays();
        cy.contains('table tbody tr', order.reference, { timeout: 15000 }).within(() => {
          cy.get(`button[aria-label="Annuler la commande ${order.reference}"]`).click();
        });
        cy.get('[data-testid="confirm-dialog"]', { timeout: 10000 }).should('be.visible');
        cy.get('[data-testid="confirm-dialog"]').should('contain', 'Annuler la commande');
        cy.get('[data-testid="confirm-cancel"]').should('contain', 'Non');
        cy.get('[data-testid="confirm-ok"]').should('contain', 'Valider');
        cy.get('[data-testid="confirm-cancel"]').click();
        cy.get('[data-testid="confirm-dialog"]').should('not.exist');
        cy.apiGet(ctx.supplierToken, `/api/orders/${order.id}`).then((r) => {
          expect(r.body.status).to.eq('DRAFT');
        });
        cy.contains('table tbody tr', order.reference).within(() => {
          cy.get(`button[aria-label="Annuler la commande ${order.reference}"]`).click();
        });
        cy.get('[data-testid="confirm-ok"]').click();
        cy.contains('Commande annulée', { timeout: 10000 }).should('exist');
        cy.apiGet(ctx.supplierToken, `/api/orders/${order.id}`).then((r) => {
          expect(r.body.status).to.eq('CANCELLED');
        });
      });
    });
  });
});
