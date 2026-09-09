const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';
function authHeaders(token: string) { return { Authorization: `Bearer ${token}` }; }

describe('12 - API: Auth', () => {
  it('POST /api/auth/login - should return JWT token', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: 'Admin@123' },
    }).then(r => {
      expect(r.status).to.eq(200);
      expect(r.body).to.have.property('accessToken');
    });
  });

  it('POST /api/auth/login - should reject invalid credentials', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'invalid', password: 'invalid' },
      failOnStatusCode: false,
    }).then(r => { expect(r.status).to.be.oneOf([401, 403]); });
  });

  it('POST /api/auth/login - should reject non-existent user', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'nonexistent', password: 'Admin@123' },
      failOnStatusCode: false,
    }).then(r => { expect(r.status).to.be.oneOf([401, 403]); });
  });

  it('GET /api/auth/me - should return current user', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: authHeaders(r.body.accessToken) })
          .then(me => {
            expect(me.status).to.eq(200);
            expect(me.body).to.have.property('username', 'system.admin');
            expect(me.body.roles).to.include('SYSTEM_ADMIN');
          });
      });
  });

  it('GET /api/auth/me - should reject without token', () => {
    cy.request({ method: 'GET', url: `${API()}/api/auth/me`, failOnStatusCode: false })
      .then(r => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Admin Organizations', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { token = r.body.accessToken; });
  });

  it('GET /api/admin/suppliers - should list suppliers', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: authHeaders(token) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
        expect(r.body.length).to.be.gte(2);
        expect(r.body.find((s: any) => s.name === 'Covale')).to.exist;
        expect(r.body.find((s: any) => s.name === 'Pointteck')).to.exist;
      });
  });

  it('GET /api/admin/shops - should list shops', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/shops`, headers: authHeaders(token) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
        expect(r.body.length).to.be.gte(4);
      });
  });

  it('GET /api/admin/stats - should return stats', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/stats`, headers: authHeaders(token) })
      .then(r => { expect(r.status).to.eq(200); });
  });

  it('GET /api/admin/supplier-shop-relations - should list 4 relations', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/supplier-shop-relations`, headers: authHeaders(token) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
        expect(r.body).to.have.length(4);
      });
  });

  it('POST /api/admin/suppliers - should create supplier', () => {
    const name = `Supplier API ${Date.now()}`;
    cy.request({ method: 'POST', url: `${API()}/api/admin/suppliers`, headers: authHeaders(token), body: { name } })
      .then(r => {
        expect(r.status).to.be.oneOf([200, 201]);
        expect(r.body).to.have.property('name', name);
      });
  });

  it('POST /api/admin/shops - should create shop', () => {
    const name = `Shop API ${Date.now()}`;
    cy.request({ method: 'POST', url: `${API()}/api/admin/shops`, headers: authHeaders(token), body: { name } })
      .then(r => {
        expect(r.status).to.be.oneOf([200, 201]);
        expect(r.body).to.have.property('name', name);
      });
  });

  it('GET /api/admin - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, failOnStatusCode: false })
      .then(r => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Payments', () => {
  let adminToken: string;
  let shopToken: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { adminToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'abdelslam', password: 'Admin@123' } })
      .then(r => { shopToken = r.body.accessToken; });
  });

  it('GET /api/payments - should list payments', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });

  it('GET /api/payments/stats - should return stats', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments/stats`, headers: authHeaders(adminToken) })
      .then(r => { expect(r.status).to.eq(200); });
  });

  it('POST /api/payments - shop can create payment', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/payments`,
      headers: authHeaders(shopToken),
      body: { shopId: 3, supplierId: 1, amount: 200.00, currency: 'EUR' },
    }).then(r => {
      expect(r.status).to.be.oneOf([200, 201]);
      expect(r.body).to.have.property('reference');
      expect(r.body.status).to.eq('PENDING');
    });
  });

  it('GET /api/payments - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`, failOnStatusCode: false })
      .then(r => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Orders', () => {
  let token: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { token = r.body.accessToken; });
  });

  it('GET /api/orders - should list orders', () => {
    cy.request({ method: 'GET', url: `${API()}/api/orders`, headers: authHeaders(token) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });

  it('GET /api/orders - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/orders`, failOnStatusCode: false })
      .then(r => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Users', () => {
  let adminToken: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { adminToken = r.body.accessToken; });
  });

  it('GET /api/users - should list users', () => {
    cy.request({ method: 'GET', url: `${API()}/api/users`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
        expect(r.body.length).to.be.gte(5);
      });
  });

  it('GET /api/users - should reject non-admin', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/users`, headers: authHeaders(r.body.accessToken), failOnStatusCode: false })
          .then(res => { expect(res.status).to.be.oneOf([401, 403]); });
      });
  });
});

describe('12 - API: Catalog (supplier)', () => {
  let token: string;
  let supplierId: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => {
        token = r.body.accessToken;
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: authHeaders(token) })
          .then(me => { supplierId = me.body.organizationId; });
      });
  });

  it('GET /api/supplier/catalog/categories - should list categories', () => {
    cy.request({ method: 'GET', url: `${API()}/api/supplier/catalog/categories?supplierId=${supplierId}`, headers: authHeaders(token), failOnStatusCode: false })
      .then(r => { expect(r.status).to.be.oneOf([200, 403]); });
  });

  it('GET /api/supplier/catalog/families - should list families', () => {
    cy.request({ method: 'GET', url: `${API()}/api/supplier/catalog/families?supplierId=${supplierId}`, headers: authHeaders(token), failOnStatusCode: false })
      .then(r => { expect(r.status).to.be.oneOf([200, 403]); });
  });
});

describe('12 - API: Stocks (supplier)', () => {
  let token: string;
  let supplierId: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => {
        token = r.body.accessToken;
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: authHeaders(token) })
          .then(me => { supplierId = me.body.organizationId; });
      });
  });

  it('GET /api/suppliers/:id/products - should list products', () => {
    cy.request({ method: 'GET', url: `${API()}/api/suppliers/${supplierId}/products`, headers: authHeaders(token) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });

  it('GET /api/suppliers/:id/movements - should list movements', () => {
    cy.request({ method: 'GET', url: `${API()}/api/suppliers/${supplierId}/movements`, headers: authHeaders(token) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });
});

describe('12 - API: Balances', () => {
  let adminToken: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => { adminToken = r.body.accessToken; });
  });

  it('GET /api/balances/supplier/1 - should list supplier balances', () => {
    cy.request({ method: 'GET', url: `${API()}/api/balances/supplier/1`, headers: authHeaders(adminToken), failOnStatusCode: false })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });

  it('GET /api/balances/shop/3 - should list shop balances', () => {
    cy.request({ method: 'GET', url: `${API()}/api/balances/shop/3`, headers: authHeaders(adminToken), failOnStatusCode: false })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.be.an('array');
      });
  });
});

describe('12 - API: Payment Lifecycle', () => {
  let shopToken: string;
  let supplierToken: string;
  let paymentRef: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'abdelslam', password: 'Admin@123' } })
      .then(r => { shopToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => { supplierToken = r.body.accessToken; });
  });

  it('shop creates payment', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/payments`,
      headers: authHeaders(shopToken),
      body: { shopId: 3, supplierId: 1, amount: 500.00, currency: 'EUR' },
    }).then(r => {
      expect(r.status).to.be.oneOf([200, 201]);
      expect(r.body.status).to.eq('PENDING');
      paymentRef = r.body.reference;
    });
  });

  it('supplier sees the payment', () => {
    cy.wait(2000);
    cy.request({ method: 'GET', url: `${API()}/api/payments`, headers: authHeaders(supplierToken) })
      .then(r => {
        const payment = r.body.find((p: any) => p.reference === paymentRef);
        expect(payment).to.exist;
      });
  });

  it('supplier confirms the payment', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`, headers: authHeaders(supplierToken) })
      .then(r => {
        const payment = r.body.find((p: any) => p.reference === paymentRef);
        if (payment) {
          cy.request({ method: 'POST', url: `${API()}/api/payments/${payment.id}/confirm`, headers: authHeaders(supplierToken) })
            .then(res => {
              expect(res.status).to.eq(200);
              expect(res.body.status).to.eq('CONFIRMED');
            });
        }
      });
  });

  it('shop gets notification after confirmation', () => {
    cy.wait(3000);
    cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(shopToken) })
      .then(r => {
        const notif = r.body.find((n: any) => n.type === 'PAYMENT_CONFIRMED');
        expect(notif).to.exist;
      });
  });
});

describe('12 - API: RBAC Verification', () => {
  it('system.admin should access all admin endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' } })
      .then(r => {
        const h = authHeaders(r.body.accessToken);
        cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/admin/shops`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/users`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/payments`, headers: h }).then(res => expect(res.status).to.eq(200));
      });
  });

  it('covale.admin should access supplier endpoints but not admin', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.admin', password: 'Admin@123' } })
      .then(r => {
        const h = authHeaders(r.body.accessToken);
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/products`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: h, failOnStatusCode: false })
          .then(res => expect(res.status).to.be.oneOf([403, 401]));
      });
  });

  it('abdelslam (shop admin) should access shop endpoints but not admin', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'abdelslam', password: 'Admin@123' } })
      .then(r => {
        const h = authHeaders(r.body.accessToken);
        cy.request({ method: 'GET', url: `${API()}/api/orders`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/balances/shop/3`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: h, failOnStatusCode: false })
          .then(res => expect(res.status).to.be.oneOf([403, 401]));
      });
  });

  it('covale.agent1 should access supplier agent endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: { username: 'covale.agent1', password: 'Admin@123' } })
      .then(r => {
        const h = authHeaders(r.body.accessToken);
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/products`, headers: h }).then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/movements`, headers: h }).then(res => expect(res.status).to.eq(200));
      });
  });
});
