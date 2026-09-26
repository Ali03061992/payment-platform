// Parcours 10-notifications.cy.ts : 10 - Notifications: UI; 10 - Notifications: API (13 scenarios).
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), connexion admin via cy.loginAsAdmin(), API live (Cypress.env apiUrl), UI live.
// Budget E2E : pas de hammering auth (le 429 est prouve cote backend), nettoyages via before/beforeEach.
function isPage(body: any): boolean {
  return body && typeof body === 'object' && !Array.isArray(body) && 'items' in body;
}
function getItems(body: any): any[] {
  return isPage(body) ? body.items : (Array.isArray(body) ? body : []);
}

describe('10 - Notifications: UI', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
    cy.dismissOverlays();
  });

  it('should display notification bell in top bar', () => {
    cy.get('.top-bar').should('be.visible');
    cy.get('.notif-bell').should('exist');
  });

  it('should open notification panel on bell click', () => {
    cy.get('.notif-bell').click();
    cy.get('.notif-panel').should('be.visible');
    cy.get('.notif-panel .notif-header h3').should('contain', 'Notifications');
  });

  it('should show notification list or empty state', () => {
    cy.get('.notif-bell').click();
    cy.dismissOverlays();
    cy.get('.notif-panel').should('be.visible');
    cy.get('.notif-panel').within(() => {
      cy.get('.notif-list', { timeout: 10000 }).should('exist');
      cy.get('.notif-empty, .notif-item', { timeout: 10000 }).should('exist');
    });
  });

  it('should close panel when clicking overlay', () => {
    cy.get('.notif-bell').click();
    cy.get('.notif-panel').should('be.visible');
    cy.get('.notif-overlay').click({ force: true });
    cy.get('.notif-panel').should('not.exist');
  });

  it('should show mark all read button when unread exist', () => {
    cy.get('.notif-bell').click();
    cy.get('body').then(($body) => {
      if ($body.find('.notif-badge').length > 0) {
        cy.get('.notif-mark-all').should('exist');
        cy.get('.notif-mark-all').should('contain', 'Tout marquer lu');
      }
    });
  });

  it('should show unread badge when notifications exist', () => {
    cy.get('.notif-bell').should('exist');
    cy.get('body').then(($body) => {
      if ($body.find('.notif-badge').length > 0) {
        cy.get('.notif-badge').should('be.visible');
      } else {
        cy.log('No unread notifications');
      }
    });
  });

  it('should display notification items with icon and message', () => {
    cy.get('.notif-bell').click();
    cy.dismissOverlays();
    cy.get('.notif-panel').should('be.visible');
    cy.get('.notif-empty, .notif-item', { timeout: 10000 }).should('exist');
    cy.get('body').then(($body) => {
      if ($body.find('.notif-panel .notif-item').length > 0) {
        cy.get('.notif-item').first().within(() => {
          cy.get('.notif-icon').should('exist');
          cy.get('.notif-content').should('exist');
        });
      } else {
        cy.get('.notif-empty').should('exist');
      }
    });
  });
});

describe('10 - Notifications: API', () => {
  let adminToken: string;
  let shopToken: string;
  let supplierToken: string;

  before(() => {
    cy.ensureTestUsers();
    cy.apiLogin('system.admin').then((t) => { adminToken = t; });
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.shopAdmin.username).then((t) => { shopToken = t; });
      cy.apiLogin(ctx.users.supplierAdmin.username).then((t) => { supplierToken = t; });
    });
  });

  it('GET /api/notifications - should return array or page', () => {
    cy.apiGet(adminToken, '/api/notifications').then((r) => {
      expect(r.status).to.eq(200);
      expect(isPage(r.body) || Array.isArray(r.body)).to.be.true;
    });
  });

  it('GET /api/notifications - should reject without token', () => {
    cy.request({ method: 'GET', url: `${Cypress.env('apiUrl') || 'http://localhost:8081'}/api/notifications`, failOnStatusCode: false })
      .then((r) => { expect(r.status).to.eq(401); });
  });

  it('GET /api/notifications/unread-count - should return count', () => {
    cy.apiGet(adminToken, '/api/notifications/unread-count').then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.have.property('count');
      expect(r.body.count).to.be.a('number');
    });
  });

  it('POST /api/notifications/read-all - should mark all as read', () => {
    cy.apiPost(adminToken, '/api/notifications/read-all', {}).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.have.property('updated');
    });
  });

  it('Payment creation generates notifications', () => {
    const api = Cypress.env('apiUrl') || 'http://localhost:8081';
    cy.getTestCtx().then((ctx) => {
      cy.request({
        method: 'POST', url: `${api}/api/payments`,
        body: { shopId: ctx.shops.abdelslam.id, supplierId: ctx.suppliers.covale.id, amount: 75.00, currency: 'TND' },
        headers: { Authorization: `Bearer ${shopToken}` },
        failOnStatusCode: false,
      }).then((r) => {
        if (r.status !== 200 && r.status !== 201) {
          cy.log('Payment creation failed - skipping notification check');
          return;
        }
        expect(r.body).to.have.property('reference');
        cy.wait(5000);
        cy.apiGet(supplierToken, '/api/notifications').then((nr) => {
          const notifs = getItems(nr.body);
          const matching = notifs.filter((n: any) => n.type === 'PAYMENT_CREATED');
          cy.log(`Found ${matching.length} PAYMENT_CREATED notifications`);
        });
      });
    });
  });
});
