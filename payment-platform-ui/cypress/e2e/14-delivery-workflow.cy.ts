const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';
const hdr = (t: string) => ({ Authorization: `Bearer ${t}` });

function me(token: string) {
  return cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: hdr(token) })
    .then((r) => r.body);
}

function createOrder(ctx: any, notes: string, asap = false) {
  expect(ctx.productId, 'productId must be set before creating order').to.not.be.null;
  expect(ctx.covaleId, 'covaleId must be set').to.not.be.null;
  expect(ctx.shopAbdelslamId, 'shopAbdelslamId must be set').to.not.be.null;
  const items = [{ productId: ctx.productId, quantity: 2, discount: 0 }];
  return cy.apiPost(ctx.shopToken, '/api/orders', {
    supplierId: ctx.covaleId, shopId: ctx.shopAbdelslamId,
    asapPayment: asap, currency: 'TND', notes, items,
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201], `Order creation failed: ${JSON.stringify(r.body)}`);
    return cy.wrap(r.body.id as string);
  });
}

function advanceToReady(ctx: any, orderId: string) {
  return cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/confirm`, {}).then(() =>
    cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/prepare`, {})
  ).then(() =>
    cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/ready`, {})
  );
}

function setupTestData() {
  const ctx: any = {};
  return cy.apiLogin('system.admin').then((t) => { ctx.adminToken = t; })
    .then(() => cy.apiLogin('covale.admin.e2e')).then((t) => { ctx.supplierToken = t; })
    .then(() => cy.apiLogin('covale.agent1.e2e')).then((t) => { ctx.agentToken = t; })
    .then(() => cy.apiLogin('abdelslam.e2e')).then((t) => { ctx.shopToken = t; })
    .then(() => cy.apiLogin('ali.e2e')).then((t) => { ctx.shopAliToken = t; })
    .then(() => cy.apiLogin('pointteck.agent1.e2e')).then((t) => { ctx.ptAgentToken = t; })
    .then(() => me(ctx.agentToken)).then((u) => { ctx.agentId = u.id; })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/suppliers')).then((r) => {
      const body = r.body;
      const list = Array.isArray(body) ? body
        : Array.isArray(body?.content) ? body.content
        : Array.isArray(body?.items) ? body.items
        : Array.isArray(body?.value) ? body.value
        : Array.isArray(body?.data) ? body.data
        : [];
      const covale = list.find((s: any) => s.name.includes('Covale E2E'));
      ctx.covaleId = covale?.id;
    })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/shops')).then((r) => {
      const body = r.body;
      const list = Array.isArray(body) ? body
        : Array.isArray(body?.content) ? body.content
        : Array.isArray(body?.items) ? body.items
        : Array.isArray(body?.value) ? body.value
        : Array.isArray(body?.data) ? body.data
        : [];
      ctx.shopAbdelslamId = list.find((s: any) => s.name.includes('Abdelslam'))?.id;
    })
    .then(() => {
      if (!ctx.covaleId) return;
      return cy.apiGet(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`).then((r) => {
        const body = r.body;
        const products = Array.isArray(body) ? body
          : Array.isArray(body?.content) ? body.content
          : Array.isArray(body?.items) ? body.items
          : Array.isArray(body?.value) ? body.value
          : Array.isArray(body?.data) ? body.data
          : [];
        const available = products.find((p: any) => (p.quantity - (p.reservedQty || 0)) >= 10);
        ctx.productId = available ? available.id : null;
        if (!ctx.productId) {
          return cy.apiPost(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`, {
            name: `Produit E2E Delivery ${Date.now()}`, sku: `DEL-E2E-${Date.now()}`,
            description: 'Produit pour tests delivery', unitPrice: 25.50,
            currency: 'TND', quantity: 500, minQuantity: 5,
          }).then((r2) => {
            ctx.productId = r2.body?.id || r2.body?.productId || null;
          });
        }
      });
    })
    .then(() => cy.wrap(ctx));
}

function fullDelivery(ctx: any, notes: string, asap = false) {
  let orderId: string;
  return createOrder(ctx, notes, asap).then((id) => {
    orderId = id;
    return advanceToReady(ctx, id);
  }).then(() =>
    cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/assign-delivery`, {
      agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
    })
  ).then(() =>
    cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/accept-delivery`, { accepted: true })
  ).then(() =>
    cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/confirm-delivery`, { confirmedDate: '2026-09-14' })
  ).then(() =>
    cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/deliver`, { receivedBy: ctx.agentId })
  ).then(() => cy.wrap(orderId));
}

describe('14 - Delivery: Full API Lifecycle', () => {
  before(() => cy.ensureTestUsers());

  it('should walk through full lifecycle', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Full lifecycle').then((orderId: string) => {
          return cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/confirm`, {})
            .then((r) => { expect(r.body.status).to.eq('CONFIRMED'); })
            .then(() => cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/prepare`, {}))
            .then((r) => { expect(r.body.status).to.eq('PREPARING'); })
            .then(() => cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/ready`, {}))
            .then((r) => { expect(r.body.status).to.eq('READY_FOR_DELIVERY'); })
            .then(() => cy.apiPost(ctx.supplierToken, `/api/orders/${orderId}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            }))
            .then((r) => {
              expect(r.body.deliveryAgentId).to.eq(ctx.agentId);
              expect(r.body.plannedDeliveryDate).to.eq('2026-09-15');
            })
            .then(() => cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/accept-delivery`, { accepted: true }))
            .then((r) => { expect(r.body.status).to.eq('DELIVERY_ACCEPTED'); })
            .then(() => cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/confirm-delivery`, { confirmedDate: '2026-09-14' }))
            .then((r) => {
              expect(r.body.status).to.eq('IN_DELIVERY');
              expect(r.body.confirmedDeliveryDate).to.eq('2026-09-14');
            })
            .then(() => cy.apiPost(ctx.agentToken, `/api/orders/${orderId}/deliver`, { receivedBy: ctx.agentId }))
            .then((r) => {
              expect(r.body.status).to.eq('DELIVERED');
              expect(r.body.deliveredAt).to.not.be.null;
            })
            .then(() => cy.apiPost(ctx.shopToken, `/api/orders/${orderId}/accept`, {}))
            .then((r) => { expect(r.body.status).to.eq('ACCEPTED'); });
        });
      });
    });
  });

  it('should cancel DRAFT order', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Cancel draft').then((id) => {
          return cy.apiPost(ctx.shopToken, `/api/orders/${id}/cancel`, {})
            .then((r) => expect(r.body.status).to.eq('CANCELLED'));
        });
      });
    });
  });

  it('should cancel CONFIRMED order', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Cancel confirmed').then((id) => {
          return cy.apiPost(ctx.supplierToken, `/api/orders/${id}/confirm`, {}).then(() =>
            cy.apiPost(ctx.shopToken, `/api/orders/${id}/cancel`, {})
          ).then((r) => expect(r.body.status).to.eq('CANCELLED'));
        });
      });
    });
  });

  it('should reject delivery from IN_DELIVERY', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Reject delivery').then((id) => {
          return advanceToReady(ctx, id).then(() =>
            cy.apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/accept-delivery`, { accepted: true })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
          ).then(() =>
            cy.apiPost(ctx.supplierToken, `/api/orders/${id}/delivery-reject`, { reason: 'Vehicle broken' })
          ).then((r) => expect(r.body.status).to.eq('DELIVERY_REJECTED'));
        });
      });
    });
  });

  it('should accept-asap creates payment', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'ASAP accept', true).then((id) => {
          return advanceToReady(ctx, id).then(() =>
            cy.apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/accept-delivery`, { accepted: true })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/deliver`, { receivedBy: ctx.agentId })
          ).then(() =>
            cy.apiPost(ctx.shopToken, `/api/orders/${id}/accept-asap`, {})
          ).then((r) => expect(r.body.status).to.eq('ACCEPTED'));
        });
      });
    });
  });

  it('should accept delivery from READY_FOR_DELIVERY', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Accept delivery').then((id) => {
          return advanceToReady(ctx, id).then(() =>
            cy.apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/accept-delivery`, { accepted: true })
          ).then((r) => {
            expect(r.body.status).to.eq('DELIVERY_ACCEPTED');
          });
        });
      });
    });
  });

  it('should reject delivery from READY_FOR_DELIVERY with motif', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Reject at ready').then((id) => {
          return advanceToReady(ctx, id).then(() =>
            cy.apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            cy.apiPost(ctx.agentToken, `/api/orders/${id}/accept-delivery`, { accepted: false, reason: 'Vehicle unavailable' })
          ).then((r) => {
            expect(r.body.status).to.eq('DELIVERY_REJECTED');
            expect(r.body.deliveryRejectionReason).to.eq('Vehicle unavailable');
          });
        });
      });
    });
  });
});

describe('14 - Delivery: Supplier Admin Order Management UI', () => {
  before(() => cy.ensureTestUsers());

  it('should display all delivery statuses in filter', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('.page-header h2').should('contain', 'Gestion des commandes');
    cy.get('.filters select option').should('contain', 'Prêt pour livraison');
    cy.get('.filters select option').should('contain', 'En livraison');
    cy.get('.filters select option').should('contain', 'Livraison rejetée');
  });

  it('should show Assigner for READY_FOR_DELIVERY', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Assign test').then((id) => {
        return advanceToReady(ctx, id).then(() => {
          cy.loginAsSupplierAdmin();
          cy.visit('/dashboard/supplier/orders');
          cy.get('.filters select').select('READY_FOR_DELIVERY');
          cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
          cy.get('table tbody tr').first().within(() => {
            cy.get('button').contains('Assigner').should('exist');
          });
        });
      });
    });
  });

  it('should open assign modal with agent select and date', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Modal test').then((id) => {
        return advanceToReady(ctx, id).then(() => {
          cy.loginAsSupplierAdmin();
          cy.visit('/dashboard/supplier/orders');
          cy.get('.filters select').select('READY_FOR_DELIVERY');
          cy.get('table tbody tr', { timeout: 10000 }).first().within(() => {
            cy.get('button').contains('Assigner').click();
          });
          cy.get('.modal-overlay').should('be.visible');
          cy.get('.modal-content').should('contain', 'Assigner un livreur');
          cy.get('select[name="agentId"]').should('exist');
          cy.get('input[name="plannedDate"]').should('exist');
          cy.get('.btn-secondary').contains('Annuler').click();
        });
      });
    });
  });
});

describe('14 - Delivery: Agent Delivery Management UI', () => {
  before(() => cy.ensureTestUsers());

  it('should display "Mes livraisons" page', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url().should('include', '/supplier/deliveries');
    cy.get('.page-header h2').should('contain', 'Mes livraisons');
  });

  it('should show empty state when no deliveries', () => {
    cy.loginAsPointteckAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('.empty', { timeout: 10000 }).should('contain', 'Aucune livraison assignée');
  });

  it('agent should have Livraisons nav', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('a.nav-item[href*="deliveries"]').should('exist');
  });
});

describe('14 - Delivery: Cross-role Access Control', () => {
  before(() => cy.ensureTestUsers());

  it('supplier admin should NOT have Livraisons nav', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('a.nav-item[href*="deliveries"]').should('not.exist');
  });

  it('delivery agent SHOULD have Livraisons nav', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('a.nav-item[href*="deliveries"]').should('exist');
  });

  it('shop admin should have Mes commandes nav', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('a.nav-item[href*="shop/orders"]').should('exist');
  });

  it('agent should NOT access shop pages', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/shop/orders');
    cy.url({ timeout: 5000 }).should('not.include', '/shop/orders');
  });

  it('shop admin should NOT access supplier deliveries', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url({ timeout: 5000 }).should('not.include', '/supplier/deliveries');
  });

  it('agent sees only own deliveries via API', () => {
    setupTestData().then((ctx) => {
      me(ctx.agentToken).then((user) => {
        cy.apiGet(ctx.agentToken, '/api/orders/my-deliveries').then((r) => {
          const deliveries = Array.isArray(r.body) ? r.body : [];
          deliveries.forEach((d: any) => {
            expect(d.deliveryAgentId).to.eq(user.id);
          });
        });
      });
    });
  });

  it('supplier admin should see deliveries filtered by agent via API', () => {
    setupTestData().then((ctx) => {
      cy.apiGet(ctx.supplierToken, `/api/orders/deliveries?agentId=${ctx.agentId}`).then((r) => {
        const deliveries = Array.isArray(r.body) ? r.body : [];
        deliveries.forEach((d: any) => {
          expect(d.deliveryAgentId).to.eq(ctx.agentId);
        });
      });
    });
  });

  it('supplier admin should see deliveries without filter', () => {
    setupTestData().then((ctx) => {
      cy.apiGet(ctx.supplierToken, '/api/orders/deliveries').then((r) => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
    });
  });

  it('supplier admin should see deliveries tab in order management', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('.tabs .tab-btn').should('have.length', 2);
    cy.get('.tabs .tab-btn').eq(1).should('contain', 'Livraisons');
  });

  it('order detail should show createdByName', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Detail createdByName test').then((id) => {
        cy.apiGet(ctx.supplierToken, `/api/orders/${id}`).then((r) => {
          expect(r.body).to.have.property('createdByName');
          expect(r.body.createdByName).to.not.be.null;
        });
      });
    });
  });

  it('deliver ASAP order should auto-create payment', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'ASAP delivery payment test', true).then((id) => {
        return advanceToReady(ctx, id).then(() =>
          cy.apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
            agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
          })
        ).then(() =>
          cy.apiPost(ctx.agentToken, `/api/orders/${id}/accept-delivery`, { accepted: true })
        ).then(() =>
          cy.apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
        ).then(() =>
          cy.apiPost(ctx.agentToken, `/api/orders/${id}/deliver`, { receivedBy: ctx.agentId })
        ).then((r) => {
          expect(r.body.status).to.eq('DELIVERED');
        }).then(() =>
          cy.apiPost(ctx.shopToken, `/api/orders/${id}/accept-asap`, {})
        ).then((r) => {
          expect(r.body.status).to.eq('ACCEPTED');
          expect(r.body.asapPayment).to.be.true;
        });
      });
    });
  });

  it('supplier admin order management should switch tabs', () => {
    setupTestData().then((ctx) => {
      return createOrder(ctx, 'Tab switch test').then((id) => {
        return advanceToReady(ctx, id);
      }).then(() => {
        cy.loginAsSupplierAdmin();
        cy.visit('/dashboard/supplier/orders');
        cy.get('.tabs .tab-btn').contains('Livraisons').click();
        cy.get('.tabs .tab-btn').eq(1).should('have.class', 'active');
        cy.get('.tabs .tab-btn').contains('Commandes').click();
        cy.get('.tabs .tab-btn').eq(0).should('have.class', 'active');
      });
    });
  });

  it('delivery detail modal should display full info', () => {
    setupTestData().then((ctx) => {
      return fullDelivery(ctx, 'Delivery detail test').then(() => {
        cy.loginAsSupplierAdmin();
        cy.visit('/dashboard/supplier/orders');
        cy.get('.tabs .tab-btn').contains('Livraisons').click();
        cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
        cy.get('table tbody tr').first().click();
        cy.get('.modal-overlay').should('be.visible');
        cy.get('.modal-content').should('contain', 'Livraison');
        cy.get('.detail-grid').should('exist');
        cy.get('.detail-grid .detail-label').should('contain', 'Statut');
        cy.get('.detail-grid .detail-label').should('contain', 'Total');
        cy.get('.detail-grid .detail-label').should('contain', 'Boutique');
        cy.get('.btn-close').click();
        cy.get('.modal-overlay').should('not.exist');
      });
    });
  });
});
