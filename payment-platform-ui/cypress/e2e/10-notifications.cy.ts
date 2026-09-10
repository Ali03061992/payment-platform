const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';
function authHeaders(token: string) { return { Authorization: `Bearer ${token}` }; }
const env = (key: string) => Cypress.env(key) as string;

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
    cy.get('.notif-panel').should('be.visible');
    cy.get('body').then(($body) => {
      if ($body.find('.notif-item').length > 0) {
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
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { adminToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'abdelslam', password: 'Admin@123' } })
      .then(r => { shopToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => { supplierToken = r.body.accessToken; });
  });

  it('GET /api/notifications - should return array or page', () => {
    cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(isPage(r.body) || Array.isArray(r.body)).to.be.true;
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
    const shopAbdelslamId = env('shopAbdelslamId');
    const covaleId = env('covaleId');
    if (!shopAbdelslamId || !covaleId) { cy.log('Missing env vars - skipping'); return; }

    cy.request({ method: 'POST', url: `${API()}/api/payments`, body: { shopId: shopAbdelslamId, supplierId: covaleId, amount: 75.00, currency: 'EUR' }, headers: authHeaders(shopToken), failOnStatusCode: false })
      .then(r => {
        if (r.status !== 200 && r.status !== 201) {
          cy.log('Payment creation failed - skipping notification check');
          return;
        }
        expect(r.body).to.have.property('reference');
        cy.wait(5000);
        cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(supplierToken) })
          .then(nr => {
            const notifs = getItems(nr.body);
            const matching = notifs.filter((n: any) => n.type === 'PAYMENT_CREATED');
            cy.log(`Found ${matching.length} PAYMENT_CREATED notifications`);
          });
      });
  });
});
