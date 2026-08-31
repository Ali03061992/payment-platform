const API_URL = 'http://localhost:8081';

declare namespace Cypress {
  interface Chainable {
    login(username: string, password: string): Chainable<void>;
    loginAsAdmin(): Chainable<void>;
    loginAsSupplierAdmin(): Chainable<void>;
    loginAsShopAdmin(): Chainable<void>;
    ensureTestUsers(): Chainable<void>;
  }
}

Cypress.Commands.add('ensureTestUsers', () => {
  cy.request({
    method: 'POST', url: `${API_URL}/api/auth/login`,
    body: { username: 'system.admin', password: 'Admin@123' },
  }).then((resp) => {
    const token = resp.body.accessToken;
    const headers = { Authorization: `Bearer ${token}` };

    cy.request({ method: 'GET', url: `${API_URL}/api/users`, headers, failOnStatusCode: false })
      .then((usersResp) => {
        const users = usersResp.status === 200 ? usersResp.body : [];
        const hasSupplierAdmin = users.some((u: any) => u.username === 'supplier.admin');
        const hasShopAdmin = users.some((u: any) => u.username === 'shop.admin');

        if (!hasSupplierAdmin) {
          cy.request({
            method: 'POST', url: `${API_URL}/api/admin/suppliers`, headers,
            body: { name: 'E2E Supplier Test' }, failOnStatusCode: false,
          }).then((supResp) => {
            if (supResp.status === 200 || supResp.status === 201) {
              const supId = supResp.body.id;
              cy.request({
                method: 'POST', url: `${API_URL}/api/users`, headers,
                body: {
                  username: 'supplier.admin', email: 'supplier.admin@test.com',
                  password: 'Admin@123', firstName: 'Supplier', lastName: 'Admin',
                  phone: '+21699111111', role: 'SUPPLIER_ADMIN', organizationId: supId,
                }, failOnStatusCode: false,
              });
            }
          });
        }

        if (!hasShopAdmin) {
          cy.request({
            method: 'POST', url: `${API_URL}/api/admin/shops`, headers,
            body: { name: 'E2E Shop Test' }, failOnStatusCode: false,
          }).then((shopResp) => {
            if (shopResp.status === 200 || shopResp.status === 201) {
              const shopId = shopResp.body.id;
              cy.request({
                method: 'POST', url: `${API_URL}/api/users`, headers,
                body: {
                  username: 'shop.admin', email: 'shop.admin@test.com',
                  password: 'Admin@123', firstName: 'Shop', lastName: 'Admin',
                  phone: '+21699222222', role: 'SHOP_ADMIN', organizationId: shopId,
                }, failOnStatusCode: false,
              });
            }
          });
        }
      });
  });
});

Cypress.Commands.add('login', (username: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${API_URL}/api/auth/login`,
    body: { username, password },
    failOnStatusCode: false,
  }).then((resp) => {
    if (resp.status === 200 && resp.body && resp.body.accessToken) {
      window.localStorage.setItem('token', resp.body.accessToken);
      cy.request({
        method: 'GET',
        url: `${API_URL}/api/auth/me`,
        headers: { Authorization: `Bearer ${resp.body.accessToken}` },
      }).then((meResp) => {
        if (meResp.status === 200) {
          window.localStorage.setItem('user', JSON.stringify(meResp.body));
        }
      });
    }
  });
});

Cypress.Commands.add('loginAsAdmin', () => {
  cy.login('system.admin', 'Admin@123');
});

Cypress.Commands.add('loginAsSupplierAdmin', () => {
  cy.login('supplier.admin', 'Admin@123');
});

Cypress.Commands.add('loginAsShopAdmin', () => {
  cy.login('shop.admin', 'Admin@123');
});
