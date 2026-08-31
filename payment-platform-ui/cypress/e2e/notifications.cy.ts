const API_URL = 'http://localhost:8081';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

describe('Notifications - API Integration', () => {
  let adminToken: string;
  let supplierToken: string;
  let shopToken: string;
  let paymentRef: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API_URL}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' } }).then(r => { adminToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API_URL}/api/auth/login`,
      body: { username: 'supplier.admin', password: 'Admin@123' } }).then(r => { supplierToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API_URL}/api/auth/login`,
      body: { username: 'shop.admin', password: 'Admin@123' } }).then(r => { shopToken = r.body.accessToken; });
  });

  it('GET /api/notifications - should return array for authenticated user', () => {
    cy.request({ method: 'GET', url: `${API_URL}/api/notifications`,
      headers: authHeaders(adminToken) }).then(r => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/notifications - should reject without token', () => {
    cy.request({ method: 'GET', url: `${API_URL}/api/notifications`,
      failOnStatusCode: false }).then(r => {
      expect(r.status).to.eq(401);
    });
  });

  it('GET /api/notifications/unread-count - should return count', () => {
    cy.request({ method: 'GET', url: `${API_URL}/api/notifications/unread-count`,
      headers: authHeaders(adminToken) }).then(r => {
      expect(r.status).to.eq(200);
      expect(r.body).to.have.property('count');
      expect(r.body.count).to.be.a('number');
    });
  });

  it('POST /api/notifications/read-all - should mark all as read', () => {
    cy.request({ method: 'POST', url: `${API_URL}/api/notifications/read-all`,
      headers: authHeaders(adminToken) }).then(r => {
      expect(r.status).to.eq(200);
      expect(r.body).to.have.property('updated');
    });
  });

  it('Payment creation should generate notifications for both supplier and shop', () => {
    cy.request({ method: 'POST', url: `${API_URL}/api/payments`, body: {
      shopId: 20, supplierId: 1, amount: 100.00, currency: 'EUR'
    }, headers: authHeaders(shopToken) }).then(r => {
      expect(r.status).to.be.oneOf([200, 201]);
      expect(r.body).to.have.property('reference');
      paymentRef = r.body.reference;
    });

    cy.wait(6000);

    cy.request({ method: 'GET', url: `${API_URL}/api/notifications`,
      headers: authHeaders(supplierToken) }).then(r => {
      const supplierNotifs = r.body.filter((n: any) => n.relatedEntityId === paymentRef);
      expect(supplierNotifs.length).to.be.greaterThan(0);
      expect(supplierNotifs[0].type).to.eq('PAYMENT_CREATED');
      expect(supplierNotifs[0].message).to.contain('Nouveau paiement');
    });

    cy.request({ method: 'GET', url: `${API_URL}/api/notifications`,
      headers: authHeaders(shopToken) }).then(r => {
      const shopNotifs = r.body.filter((n: any) => n.relatedEntityId === paymentRef);
      expect(shopNotifs.length).to.be.greaterThan(0);
      expect(shopNotifs[0].type).to.eq('PAYMENT_CREATED');
      expect(shopNotifs[0].message).to.contain('soumis');
    });
  });

  it('Payment confirmation should notify shop', () => {
    cy.request({ method: 'GET', url: `${API_URL}/api/notifications/unread-count`,
      headers: authHeaders(shopToken) }).then(before => {
      const beforeCount = before.body.count;

      cy.request({ method: 'GET', url: `${API_URL}/api/payments`, headers: authHeaders(shopToken) }).then(pays => {
        const pending = pays.body.find((p: any) => p.status === 'PENDING');
        if (pending) {
          cy.request({ method: 'POST', url: `${API_URL}/api/payments/${pending.id}/confirm`,
            headers: authHeaders(supplierToken) }).then(r => {
            expect(r.status).to.eq(200);
          });

          cy.wait(6000);

          cy.request({ method: 'GET', url: `${API_URL}/api/notifications/unread-count`,
            headers: authHeaders(shopToken) }).then(after => {
            expect(after.body.count).to.be.greaterThan(beforeCount);
          });
        }
      });
    });
  });
});

describe('Notifications - UI', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
  });

  it('should display notification bell in header', () => {
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

  it('should show unread badge when there are unread notifications', () => {
    cy.get('.notif-bell').should('exist');
    cy.get('body').then($body => {
      if ($body.find('.notif-badge').length > 0) {
        cy.get('.notif-badge').should('be.visible');
      } else {
        cy.log('No unread badge visible (all read)');
      }
    });
  });
});
