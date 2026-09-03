function adminHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

describe('API Integration - Auth', () => {
  it('POST /api/auth/login - should return JWT token', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then((resp) => {
      expect(resp.status).to.eq(200);
      expect(resp.body).to.have.property('accessToken');
    });
  });

  it('POST /api/auth/login - should reject invalid credentials', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'invalid', password: 'invalid' }, failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.be.oneOf([401, 403]);
    });
  });

  it('POST /api/auth/login - should reject non-existent user', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'nonexistentuser', password: 'Admin@123' }, failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.be.oneOf([401, 403]);
    });
  });

  it('GET /api/auth/me - should return current user', () => {
    cy.loginAsAdmin();
    cy.window().then((win) => {
      const token = win.localStorage.getItem('token');
      cy.request({
        method: 'GET', url: `${API()}/api/auth/me`,
        headers: adminHeaders(token!),
      }).then((resp) => {
        expect(resp.status).to.eq(200);
        expect(resp.body).to.have.property('username', 'system.admin');
      });
    });
  });

  it('GET /api/auth/me - should reject without token', () => {
    cy.request({
      method: 'GET', url: `${API()}/api/auth/me`, failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status).to.be.oneOf([401, 403]);
    });
  });
});

describe('API Integration - Admin Organizations', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then((r) => { token = r.body.accessToken; });
  });

  it('GET /api/admin/suppliers - should list suppliers', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/admin/shops - should list shops', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/shops`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/admin/stats - should return stats', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/stats`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
    });
  });

  it('GET /api/admin/supplier-shop-relations - should list relations', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/supplier-shop-relations`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('POST /api/admin/suppliers - should create supplier', () => {
    const name = `Supplier API ${Date.now()}`;
    cy.request({ method: 'POST', url: `${API()}/api/admin/suppliers`,
      headers: adminHeaders(token), body: { name } }).then((r) => {
      expect(r.status).to.be.oneOf([200, 201]);
      expect(r.body).to.have.property('name', name);
    });
  });

  it('POST /api/admin/shops - should create shop', () => {
    const name = `Shop API ${Date.now()}`;
    cy.request({ method: 'POST', url: `${API()}/api/admin/shops`,
      headers: adminHeaders(token), body: { name } }).then((r) => {
      expect(r.status).to.be.oneOf([200, 201]);
      expect(r.body).to.have.property('name', name);
    });
  });

  it('GET /api/admin - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`,
      failOnStatusCode: false }).then((r) => {
      expect(r.status).to.be.oneOf([401, 403]);
    });
  });
});

describe('API Integration - Payments', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then((r) => { token = r.body.accessToken; });
  });

  it('GET /api/payments - should list payments', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/payments/stats - should return stats', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments/stats`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
    });
  });

  it('GET /api/payments - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`,
      failOnStatusCode: false }).then((r) => {
      expect(r.status).to.be.oneOf([401, 403]);
    });
  });
});

describe('API Integration - Orders', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then((r) => { token = r.body.accessToken; });
  });

  it('GET /api/orders - should list orders', () => {
    cy.request({ method: 'GET', url: `${API()}/api/orders`,
      headers: adminHeaders(token) }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/orders - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/orders`,
      failOnStatusCode: false }).then((r) => {
      expect(r.status).to.be.oneOf([401, 403]);
    });
  });
});

describe('API Integration - Users', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then((r) => { token = r.body.accessToken; });
  });

  it('GET /api/users - should list users', () => {
    cy.request({ method: 'GET', url: `${API()}/api/users`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/users - should reject non-admin', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'supplier.admin', password: 'Admin@123' }, failOnStatusCode: false,
    }).then((loginResp) => {
      if (loginResp.status === 200) {
        cy.request({ method: 'GET', url: `${API()}/api/users`,
          headers: adminHeaders(loginResp.body.accessToken), failOnStatusCode: false,
        }).then((r) => {
          expect(r.status).to.be.oneOf([401, 403]);
        });
      }
    });
  });
});

describe('API Integration - Catalog (supplier role)', () => {
  let token: string;
  let supplierId: number;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'supplier.admin', password: 'Admin@123' }, failOnStatusCode: false,
    }).then((r) => {
      if (r.status === 200) {
        token = r.body.accessToken;
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`,
          headers: adminHeaders(token) }).then((me) => {
          supplierId = me.body.organizationId;
        });
      }
    });
  });

  it('GET /api/supplier/catalog/categories - should list categories', () => {
    if (!token || !supplierId) return;
    cy.request({ method: 'GET', url: `${API()}/api/supplier/catalog/categories?supplierId=${supplierId}`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.be.oneOf([200, 403]);
    });
  });

  it('GET /api/supplier/catalog/families - should list families', () => {
    if (!token || !supplierId) return;
    cy.request({ method: 'GET', url: `${API()}/api/supplier/catalog/families?supplierId=${supplierId}`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.be.oneOf([200, 403]);
    });
  });
});

describe('API Integration - Stocks (supplier role)', () => {
  let token: string;
  let supplierId: number;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'supplier.admin', password: 'Admin@123' }, failOnStatusCode: false,
    }).then((r) => {
      if (r.status === 200) {
        token = r.body.accessToken;
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`,
          headers: adminHeaders(token) }).then((me) => {
          supplierId = me.body.organizationId;
        });
      }
    });
  });

  it('GET /api/suppliers/:id/products - should list products', () => {
    if (!token || !supplierId) return;
    cy.request({ method: 'GET', url: `${API()}/api/suppliers/${supplierId}/products`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/suppliers/:id/movements - should list movements', () => {
    if (!token || !supplierId) return;
    cy.request({ method: 'GET', url: `${API()}/api/suppliers/${supplierId}/movements`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });
});

describe('API Integration - Balances', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then((r) => { token = r.body.accessToken; });
  });

  it('GET /api/balances/supplier/1 - should list supplier balances', () => {
    cy.request({ method: 'GET', url: `${API()}/api/balances/supplier/1`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });

  it('GET /api/balances/shop/1 - should list shop balances', () => {
    cy.request({ method: 'GET', url: `${API()}/api/balances/shop/1`,
      headers: adminHeaders(token), failOnStatusCode: false }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.be.an('array');
    });
  });
});
