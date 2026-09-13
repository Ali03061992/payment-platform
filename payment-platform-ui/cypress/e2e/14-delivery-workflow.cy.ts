const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';
const hdr = (t: string) => ({ Authorization: `Bearer ${t}` });

function loginAs(username: string) {
  return cy.request({
    method: 'POST', url: `${API()}/api/auth/login`,
    body: { username, password: 'Admin@123' },
  }).then((r) => r.body.accessToken as string);
}

function me(token: string) {
  return cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: hdr(token) })
    .then((r) => r.body);
}

function apiPost(token: string, path: string, body: any) {
  return cy.request({
    method: 'POST', url: `${API()}${path}`,
    headers: hdr(token), body, failOnStatusCode: false,
  });
}

function apiGet(token: string, path: string) {
  return cy.request({ method: 'GET', url: `${API()}${path}`, headers: hdr(token) });
}

function setupTestData() {
  const ctx: any = {};
  return loginAs('system.admin').then((t) => { ctx.adminToken = t; })
    .then(() => loginAs('covale.admin')).then((t) => { ctx.supplierToken = t; })
    .then(() => loginAs('covale.agent1')).then((t) => { ctx.agentToken = t; })
    .then(() => loginAs('abdelslam')).then((t) => { ctx.shopToken = t; })
    .then(() => loginAs('ali')).then((t) => { ctx.shopAliToken = t; })
    .then(() => loginAs('pointteck.agent1')).then((t) => { ctx.ptAgentToken = t; })
    .then(() => me(ctx.agentToken)).then((u) => { ctx.agentId = u.id; })
    .then(() => apiGet(ctx.adminToken, '/api/admin/suppliers')).then((r) => {
      const list = Array.isArray(r.body) ? r.body : (r.body.value || []);
      const covale = list.find((s: any) => s.name === 'Covale');
      ctx.covaleId = covale?.id;
    })
    .then(() => apiGet(ctx.adminToken, '/api/admin/shops')).then((r) => {
      const list = Array.isArray(r.body) ? r.body : (r.body.value || []);
      ctx.shopAbdelslamId = list.find((s: any) => s.name === 'Abdelslam Tunis')?.id;
    })
    .then(() => {
      if (!ctx.covaleId) return;
      return apiGet(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`).then((r) => {
        const products = Array.isArray(r.body) ? r.body : (r.body.items || r.body.value || []);
        const available = products.find((p: any) => (p.quantity - p.reservedQty) >= 10);
        ctx.productId = available ? available.id : null;
        if (!ctx.productId) {
          return apiPost(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`, {
            name: 'Produit E2E Delivery', sku: `DEL-E2E-${Date.now()}`,
            description: 'Produit pour tests delivery', unitPrice: 25.50,
            currency: 'TND', quantity: 500, minQuantity: 5, unit: 'unite',
          }).then((r2) => { ctx.productId = r2.body.id; });
        }
      });
    })
    .then(() => cy.wrap(ctx));
}

function createOrder(ctx: any, notes: string, asap = false) {
  const items = ctx.productId
    ? [{ productId: ctx.productId, quantity: 2, discount: 0 }]
    : [];
  return apiPost(ctx.shopToken, '/api/orders', {
    supplierId: ctx.covaleId, shopId: ctx.shopAbdelslamId,
    asapPayment: asap, currency: 'TND', notes, items,
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201]);
    return cy.wrap(r.body.id as string);
  });
}

function advanceToReady(ctx: any, orderId: string) {
  return apiPost(ctx.supplierToken, `/api/orders/${orderId}/confirm`, {}).then(() =>
    apiPost(ctx.supplierToken, `/api/orders/${orderId}/prepare`, {})
  ).then(() =>
    apiPost(ctx.supplierToken, `/api/orders/${orderId}/ready`, {})
  );
}

function fullDelivery(ctx: any, notes: string, asap = false) {
  let orderId: string;
  return createOrder(ctx, notes, asap).then((id) => {
    orderId = id;
    return advanceToReady(ctx, id);
  }).then(() =>
    apiPost(ctx.supplierToken, `/api/orders/${orderId}/assign-delivery`, {
      agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
    })
  ).then(() =>
    apiPost(ctx.agentToken, `/api/orders/${orderId}/confirm-delivery`, { confirmedDate: '2026-09-14' })
  ).then(() =>
    apiPost(ctx.agentToken, `/api/orders/${orderId}/deliver`, { receivedBy: ctx.agentId })
  ).then(() => cy.wrap(orderId));
}

describe('14 - Delivery: Full API Lifecycle', () => {
  before(() => cy.ensureTestUsers());

  it('should walk through full lifecycle', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Full lifecycle').then((orderId: string) => {
          return apiPost(ctx.supplierToken, `/api/orders/${orderId}/confirm`, {})
            .then((r) => { expect(r.body.status).to.eq('CONFIRMED'); })
            .then(() => apiPost(ctx.supplierToken, `/api/orders/${orderId}/prepare`, {}))
            .then((r) => { expect(r.body.status).to.eq('PREPARING'); })
            .then(() => apiPost(ctx.supplierToken, `/api/orders/${orderId}/ready`, {}))
            .then((r) => { expect(r.body.status).to.eq('READY_FOR_DELIVERY'); })
            .then(() => apiPost(ctx.supplierToken, `/api/orders/${orderId}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            }))
            .then((r) => {
              expect(r.body.deliveryAgentId).to.eq(ctx.agentId);
              expect(r.body.plannedDeliveryDate).to.eq('2026-09-15');
            })
            .then(() => apiPost(ctx.agentToken, `/api/orders/${orderId}/confirm-delivery`, { confirmedDate: '2026-09-14' }))
            .then((r) => {
              expect(r.body.status).to.eq('IN_DELIVERY');
              expect(r.body.confirmedDeliveryDate).to.eq('2026-09-14');
            })
            .then(() => apiPost(ctx.agentToken, `/api/orders/${orderId}/deliver`, { receivedBy: ctx.agentId }))
            .then((r) => {
              expect(r.body.status).to.eq('DELIVERED');
              expect(r.body.deliveredAt).to.not.be.null;
            })
            .then(() => apiPost(ctx.shopToken, `/api/orders/${orderId}/accept`, {}))
            .then((r) => { expect(r.body.status).to.eq('ACCEPTED'); });
        });
      });
    });
  });

  it('should cancel DRAFT order', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Cancel draft').then((id) => {
          return apiPost(ctx.shopToken, `/api/orders/${id}/cancel`, {})
            .then((r) => expect(r.body.status).to.eq('CANCELLED'));
        });
      });
    });
  });

  it('should cancel CONFIRMED order', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Cancel confirmed').then((id) => {
          return apiPost(ctx.supplierToken, `/api/orders/${id}/confirm`, {}).then(() =>
            apiPost(ctx.shopToken, `/api/orders/${id}/cancel`, {})
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
            apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
          ).then(() =>
            apiPost(ctx.supplierToken, `/api/orders/${id}/delivery-reject`, { reason: 'Vehicle broken' })
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
            apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
          ).then(() =>
            apiPost(ctx.agentToken, `/api/orders/${id}/deliver`, { receivedBy: ctx.agentId })
          ).then(() =>
            apiPost(ctx.shopToken, `/api/orders/${id}/accept-asap`, {})
          ).then((r) => expect(r.body.status).to.eq('ACCEPTED'));
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
      createOrder(ctx, 'Assign test').then((id) => { advanceToReady(ctx, id); });
    });
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('.filters select').select('READY_FOR_DELIVERY');
    cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
    cy.get('table tbody tr').first().within(() => {
      cy.get('button').contains('Assigner').should('exist');
    });
  });

  it('should open assign modal with agent select and date', () => {
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
    cy.get('input[name="plannedDate"]').invoke('val').should('not.be.empty');
    cy.get('.btn-secondary').contains('Annuler').click();
  });

  it('should show delivery dates in order detail modal', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Detail dates').then((id) => {
          return advanceToReady(ctx, id).then(() =>
            apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-20',
            })
          );
        });
      });
    });
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('.filters select').select('READY_FOR_DELIVERY');
    cy.get('table tbody tr.clickable-row', { timeout: 10000 }).first().click();
    cy.get('.modal-content', { timeout: 5000 }).should('be.visible');
    cy.get('.modal-content').should('contain', 'Date prévue');
    cy.get('.btn-close').click();
  });

  it('should show Rejeter livr. for IN_DELIVERY', () => {
    setupTestData().then((ctx) => {
      createOrder(ctx, 'Reject btn').then((id) => {
        advanceToReady(ctx, id).then(() =>
          apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
            agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
          })
        ).then(() =>
          apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
        );
      });
    });
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('.filters select').select('IN_DELIVERY');
    cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
    cy.get('table tbody tr').first().within(() => {
      cy.get('button').contains('Rejeter livr.').should('exist');
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

  it('should show pending confirmations with planned date', () => {
    setupTestData().then((ctx) => {
      createOrder(ctx, 'Agent pending').then((id) => {
        advanceToReady(ctx, id).then(() =>
          apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
            agentId: ctx.agentId, plannedDeliveryDate: '2026-09-16',
          })
        );
      });
    });
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('.section-header', { timeout: 10000 }).contains('En attente de confirmation').should('exist');
    cy.get('.table-card').first().within(() => {
      cy.get('table thead').should('contain', 'Date prévue');
      cy.get('table tbody tr').should('have.length.gte', 1);
    });
  });

  it('should open confirm date modal', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('.table-card', { timeout: 10000 }).should('exist');
    cy.get('.table-card').first().within(() => {
      cy.get('table tbody tr').first().within(() => {
        cy.get('button').contains('Confirmer la date').click();
      });
    });
    cy.get('.modal-overlay').should('be.visible');
    cy.get('.modal-content').should('contain', 'Confirmer la date de livraison');
    cy.get('input[type="date"]').should('exist');
    cy.get('button[type="submit"]').should('contain', 'Confirmer la date');
    cy.get('.btn-secondary').contains('Annuler').click();
    cy.get('.modal-overlay').should('not.exist');
  });

  it('should confirm delivery date via UI', () => {
    setupTestData().then((ctx) => {
      createOrder(ctx, 'Confirm date').then((id) => {
        advanceToReady(ctx, id).then(() =>
          apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
            agentId: ctx.agentId, plannedDeliveryDate: '2026-09-20',
          })
        );
      });
    });
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('.table-card', { timeout: 10000 }).should('exist');
    cy.get('.table-card').first().within(() => {
      cy.get('table tbody tr').first().within(() => {
        cy.get('button').contains('Confirmer la date').click();
      });
    });
    cy.get('.modal-overlay').should('be.visible');
    cy.get('input[type="date"]').clear().type('2026-09-18');
    cy.get('button[type="submit"]').click();
    cy.get('.modal-overlay', { timeout: 5000 }).should('not.exist');
  });

  it('should show active deliveries with Marquer livré', () => {
    setupTestData().then((ctx) => {
      createOrder(ctx, 'Active delivery').then((id) => {
        advanceToReady(ctx, id).then(() =>
          apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
            agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
          })
        ).then(() =>
          apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
        );
      });
    });
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('.section-header', { timeout: 10000 }).contains('Livraisons en cours').should('exist');
    cy.contains('Livraisons en cours').parent('.section-header').next('.table-card').within(() => {
      cy.get('table thead').should('contain', 'Date confirmée');
      cy.get('table tbody tr').should('have.length.gte', 1);
      cy.get('table tbody tr').first().within(() => {
        cy.get('button').contains('Marquer livré').should('exist');
      });
    });
  });

  it('should mark delivered via UI', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => {
        return createOrder(ctx, 'Mark delivered').then((id) => {
          return advanceToReady(ctx, id).then(() =>
            apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
              agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
            })
          ).then(() =>
            apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
          );
        });
      });
    });
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.contains('Livraisons en cours').parent('.section-header').next('.table-card', { timeout: 10000 })
      .within(() => {
        cy.get('table tbody tr').first().within(() => {
          cy.get('button').contains('Marquer livré').click();
        });
      });
    cy.get('.modal-overlay').should('be.visible');
    cy.get('.modal-content').should('contain', 'Confirmer la livraison');
    cy.get('input[name="receivedBy"]').clear().type('0bc52e30-64fb-4ac7-8c5e-42ced1f2fb1e');
    cy.get('button[type="submit"]').contains('Confirmer la livraison').click();
    cy.get('.modal-overlay', { timeout: 5000 }).should('not.exist');
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

describe('14 - Delivery: Shop Order List UI', () => {
  before(() => cy.ensureTestUsers());

  it('should display shop order list', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.url().should('include', '/shop/orders');
    cy.get('.page-header h2').should('contain', 'Commandes');
  });

  it('should have all delivery status filter options', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select option').should('contain', 'Prêt pour livraison');
    cy.get('.filters select option').should('contain', 'En livraison');
    cy.get('.filters select option').should('contain', 'Livré');
    cy.get('.filters select option').should('contain', 'Livraison rejetée');
  });

  it('should filter DELIVERED and show Accepter/Rejeter', () => {
    setupTestData().then((ctx) => { fullDelivery(ctx, 'Shop delivered'); });
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('DELIVERED');
    cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
    cy.get('table tbody tr').first().within(() => {
      cy.get('button').contains('Accepter').should('exist');
      cy.get('button').contains('Rejeter').should('exist');
    });
  });

  it('should filter READY_FOR_DELIVERY and show badge', () => {
    setupTestData().then((ctx) => {
      createOrder(ctx, 'Ready filter').then((id) => { advanceToReady(ctx, id); });
    });
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('READY_FOR_DELIVERY');
    cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
    cy.get('.payment-badge').should('contain', 'Prêt pour livraison');
  });

  it('should filter IN_DELIVERY', () => {
    setupTestData().then((ctx) => {
      createOrder(ctx, 'In delivery filter').then((id) => {
        advanceToReady(ctx, id).then(() =>
          apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
            agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
          })
        ).then(() =>
          apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' })
        );
      });
    });
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('IN_DELIVERY');
    cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
    cy.get('.payment-badge').should('contain', 'En livraison');
  });

  it('shop admin should NOT access supplier deliveries', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url({ timeout: 5000 }).should('not.include', '/supplier/deliveries');
  });
});

describe('14 - Delivery: Shop Order Detail UI', () => {
  before(() => cy.ensureTestUsers());

  it('should show delivery dates and timeline', () => {
    cy.wrap(null).then(() => {
      return setupTestData().then((ctx) => { return fullDelivery(ctx, 'Detail dates'); });
    });
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('DELIVERED');
    cy.get('table tbody tr.clickable-row', { timeout: 10000 }).first().click();
    cy.url({ timeout: 5000 }).should('match', /\/shop\/orders\/[\w-]+/);
    cy.get('.detail-grid', { timeout: 5000 }).should('exist');
    cy.get('.detail-grid').should('contain', 'Date prévue');
    cy.get('.detail-grid').should('contain', 'Date confirmée');
    cy.get('.detail-grid').should('contain', 'Livré le');
    cy.get('.status-timeline', { timeout: 10000 }).should('be.visible');
    cy.get('.status-timeline h3').should('contain.text', 'Progression');
  });

  it('should show Accepter/Rejeter for DELIVERED', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('DELIVERED');
    cy.get('table tbody tr.clickable-row', { timeout: 10000 }).first().click();
    cy.get('.action-bar', { timeout: 5000 }).should('exist');
    cy.get('.action-bar').within(() => {
      cy.get('button').contains('Accepter').should('exist');
      cy.get('button').contains('Rejeter').should('exist');
    });
  });

  it('should accept from detail page', () => {
    setupTestData().then((ctx) => { fullDelivery(ctx, 'Accept detail'); });
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('DELIVERED');
    cy.get('table tbody tr.clickable-row', { timeout: 10000 }).first().click();
    cy.get('.action-bar .btn-success', { timeout: 5000 }).click();
  });

  it('should show ASAP payment in detail', () => {
    setupTestData().then((ctx) => { fullDelivery(ctx, 'ASAP detail', true); });
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('.filters select').select('DELIVERED');
    cy.get('table tbody tr.clickable-row', { timeout: 10000 }).first().click();
    cy.get('.detail-grid', { timeout: 5000 }).should('contain', 'Paiement ASAP');
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
        apiGet(ctx.agentToken, '/api/orders/my-deliveries').then((r) => {
          const deliveries = Array.isArray(r.body) ? r.body : [];
          deliveries.forEach((d: any) => {
            expect(d.deliveryAgentId).to.eq(user.id);
          });
        });
      });
    });
  });

  it('shop admin sees only own shop orders via API', () => {
    setupTestData().then((ctx) => {
      me(ctx.shopToken).then((user) => {
        apiGet(ctx.shopToken, '/api/orders').then((r) => {
          const orders = r.body.items || [];
          orders.forEach((o: any) => {
            expect(o.shopId).to.eq(user.organizationId);
          });
        });
      });
    });
  });

  it('ali sees only ali shop orders via API', () => {
    setupTestData().then((ctx) => {
      me(ctx.shopAliToken).then((user) => {
        apiGet(ctx.shopAliToken, '/api/orders').then((r) => {
          const orders = r.body.items || [];
          orders.forEach((o: any) => {
            expect(o.shopId).to.eq(user.organizationId);
          });
        });
      });
    });
  });
});
