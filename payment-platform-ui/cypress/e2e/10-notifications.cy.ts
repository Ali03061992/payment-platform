const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';
function authHeaders(token: string) { return { Authorization: `Bearer ${token}` }; }

describe('10 - Notifications: UI', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
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
    cy.get('.notif-panel').should('be.visible');
    cy.get('.notif-panel').within(() => {
      cy.get('.notif-list').should('exist');
      cy.get('.notif-empty, .notif-item').should('exist');
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
    cy.get('.notif-item').then(($items) => {
      if ($items.length > 0) {
        cy.get('.notif-item').first().within(() => {
          cy.get('.notif-icon').should('exist');
          cy.get('.notif-content').should('exist');
          cy.get('.notif-message').should('not.be.empty');
          cy.get('.notif-time').should('exist');
        });
      }
    });
  });
});

describe('10 - Notifications: API', () => {
  let adminToken: string;
  let shopToken: string;
  let supplierToken: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { adminToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'abdelslam', password: 'Admin@123' } })
      .then(r => { shopToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => { supplierToken = r.body.accessToken; });
  });

  it('GET /api/notifications - should return array', () => {
    cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });

  it('GET /api/notifications - should reject without token', () => {
    cy.request({ method: 'GET', url: `${API()}/api/notifications`, failOnStatusCode: false })
      .then(r => { expect(r.status).to.eq(401); });
  });

  it('GET /api/notifications/unread-count - should return count', () => {
    cy.request({ method: 'GET', url: `${API()}/api/notifications/unread-count`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.property('count');
        expect(r.body.count).to.be.a('number');
      });
  });

  it('POST /api/notifications/read-all - should mark all as read', () => {
    cy.request({ method: 'POST', url: `${API()}/api/notifications/read-all`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.property('updated');
      });
  });

  it('Payment creation generates notifications', () => {
    cy.request({ method: 'POST', url: `${API()}/api/payments`, body: { shopId: 3, supplierId: 1, amount: 75.00, currency: 'EUR' }, headers: authHeaders(shopToken) })
      .then(r => {
        expect(r.status).to.be.oneOf([200, 201]);
        expect(r.body).to.have.property('reference');
        const ref = r.body.reference;
        cy.wait(8000);
        cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(supplierToken) })
          .then(nr => {
            const notifs = nr.body.filter((n: any) => n.relatedEntityId === ref);
            expect(notifs.length).to.be.greaterThan(0);
            expect(notifs[0].type).to.eq('PAYMENT_CREATED');
          });
      });
  });
});
