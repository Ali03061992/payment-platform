const API_URL = () => Cypress.env('apiUrl') || 'http://localhost:8081';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

Cypress.Commands.add('login', (username: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${API_URL()}/api/auth/login`,
    body: { username, password },
  }).then((resp) => {
    expect(resp.status).to.eq(200);
    expect(resp.body).to.have.property('accessToken');
    const token = resp.body.accessToken;
    cy.request({
      method: 'GET',
      url: `${API_URL()}/api/auth/me`,
      headers: authHeaders(token),
    }).then((me) => {
      window.sessionStorage.setItem('token', token);
      window.sessionStorage.setItem('user', JSON.stringify(me.body));
    });
  });
});

Cypress.Commands.add('loginAsAdmin', () => {
  cy.login('system.admin', 'Admin@123');
});

Cypress.Commands.add('loginAsSupplierAdmin', () => {
  cy.login('covale.admin', 'Admin@123');
});

Cypress.Commands.add('loginAsPointteckAdmin', () => {
  cy.login('pointteck.admin', 'Admin@123');
});

Cypress.Commands.add('loginAsShopAdmin', () => {
  cy.login('abdelslam', 'Admin@123');
});

Cypress.Commands.add('loginAsAli', () => {
  cy.login('ali', 'Admin@123');
});

Cypress.Commands.add('loginAsCovaleAgent', () => {
  cy.login('covale.agent1', 'Admin@123');
});

Cypress.Commands.add('loginAsPointteckAgent', () => {
  cy.login('pointteck.agent1', 'Admin@123');
});

Cypress.Commands.add('ensureTestUsers', () => {
  cy.request({
    method: 'POST',
    url: `${API_URL()}/api/auth/login`,
    body: { username: 'system.admin', password: 'Admin@123' },
    failOnStatusCode: false,
  }).then((resp) => {
    if (resp.status !== 200) return;
    const token = resp.body.accessToken;
    const testUsers = [
      { username: 'covale.admin', password: 'Admin@123', firstName: 'Covale', lastName: 'Admin', email: 'covale.admin@test.com', role: 'SUPPLIER_ADMIN', organizationId: 1 },
      { username: 'covale.agent1', password: 'Admin@123', firstName: 'Agent', lastName: 'Un', email: 'agent1@test.com', role: 'SUPPLIER_AGENT', organizationId: 1 },
      { username: 'abdelslam', password: 'Admin@123', firstName: 'Abdelslam', lastName: 'Tunis', email: 'abdelslam@test.com', role: 'SHOP_ADMIN', organizationId: 3 },
      { username: 'ali', password: 'Admin@123', firstName: 'Ali', lastName: 'Sfax', email: 'ali@test.com', role: 'SHOP_ADMIN', organizationId: 4 },
    ];
    testUsers.forEach((user) => {
      cy.request({
        method: 'POST',
        url: `${API_URL()}/api/auth/register`,
        body: user,
        failOnStatusCode: false,
      });
    });
  });
});

Cypress.Commands.add('createPayment', (shopId: string, supplierId: string, amount: number) => {
  cy.request({
    method: 'POST',
    url: `${API_URL()}/api/payments`,
    body: { shopId, supplierId, amount, currency: 'EUR' },
    failOnStatusCode: false,
  });
});

declare namespace Cypress {
  interface Chainable {
    login(username: string, password: string): Chainable<void>;
    loginAsAdmin(): Chainable<void>;
    loginAsSupplierAdmin(): Chainable<void>;
    loginAsPointteckAdmin(): Chainable<void>;
    loginAsShopAdmin(): Chainable<void>;
    loginAsAli(): Chainable<void>;
    loginAsCovaleAgent(): Chainable<void>;
    loginAsPointteckAgent(): Chainable<void>;
    ensureTestUsers(): Chainable<void>;
    createPayment(shopId: string, supplierId: string, amount: number): Chainable<void>;
  }
}
