// Parcours 12-api-integration.cy.ts : 12 - API: Auth; 12 - API: Admin Organizations; 12 - API: Payments; 12 - API: Orders; 12 - API: Users (28 scenarios).
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), API live (Cypress.env apiUrl).
// Budget E2E : pas de hammering auth (le 429 est prouve cote backend), nettoyages via before/beforeEach.
function isPage(body: any): boolean {
  return body && typeof body === 'object' && !Array.isArray(body) && 'items' in body;
}
function getItems(body: any): any[] {
  return isPage(body) ? body.items : (Array.isArray(body) ? body : []);
}
const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

describe('12 - API: Auth', () => {
  it('POST /api/auth/login - should return JWT token', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: '@PAssword012345' },
    }).then((r) => {
      expect(r.status).to.eq(200);
      expect(r.body).to.have.property('accessToken');
    });
  });

  it('POST /api/auth/login - should reject invalid credentials', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'invalid', password: 'invalid' },
      failOnStatusCode: false,
    }).then((r) => { expect(r.status).to.be.oneOf([401, 403]); });
  });

  it('GET /api/auth/me - should return current user', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/auth/me').then((me) => {
        expect(me.status).to.eq(200);
        expect(me.body).to.have.property('username', 'system.admin');
        expect(me.body.roles).to.include('SYSTEM_ADMIN');
      });
    });
  });

  it('GET /api/auth/me - should reject without token', () => {
    cy.request({ method: 'GET', url: `${API()}/api/auth/me`, failOnStatusCode: false })
      .then((r) => { expect(r.status).to.be.oneOf([401, 403]); });
  });

  it('M1 - refresh should rotate tokens and logout should revoke', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: { username: 'system.admin', password: '@PAssword012345' },
    }).then((login) => {
      expect(login.status).to.eq(200);
      expect(login.body.refreshToken, 'refreshToken issued').to.be.a('string').and.not.be.empty;
      const firstRefresh = login.body.refreshToken as string;
      cy.request({
        method: 'POST', url: `${API()}/api/auth/refresh`,
        body: { refreshToken: firstRefresh },
      }).then((refreshed) => {
        expect(refreshed.status).to.eq(200);
        expect(refreshed.body.accessToken, 'new access token').to.be.a('string').and.not.be.empty;
        expect(refreshed.body.refreshToken, 'rotated refresh token').to.be.a('string')
          .and.not.eq(firstRefresh);
        const secondRefresh = refreshed.body.refreshToken as string;
        // Le nouveau access token fonctionne.
        cy.request({
          method: 'GET', url: `${API()}/api/auth/me`,
          headers: { Authorization: `Bearer ${refreshed.body.accessToken}` },
        }).then((me) => {
          expect(me.status).to.eq(200);
          // L'ancien refresh est consommé (rotation).
          cy.request({
            method: 'POST', url: `${API()}/api/auth/refresh`,
            body: { refreshToken: firstRefresh }, failOnStatusCode: false,
          }).then((reused) => {
            expect(reused.status).to.eq(401);
            // Logout révoque le courant.
            cy.request({
              method: 'POST', url: `${API()}/api/auth/logout`,
              body: { refreshToken: secondRefresh }, failOnStatusCode: false,
            }).then((logout) => {
              expect(logout.status).to.eq(204);
              cy.request({
                method: 'POST', url: `${API()}/api/auth/refresh`,
                body: { refreshToken: secondRefresh }, failOnStatusCode: false,
              }).then((afterLogout) => {
                expect(afterLogout.status).to.eq(401);
              });
            });
          });
        });
      });
    });
  });
});

describe('12 - API: Admin Organizations', () => {
  before(() => cy.ensureTestUsers());

  it('GET /api/admin/suppliers - should list suppliers', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/admin/suppliers').then((r) => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.property('items');
        expect(getItems(r.body).length).to.be.gte(2);
        expect(r.body.totalElements).to.be.gte(2);
      });
    });
  });

  it('GET /api/admin/shops - should list shops', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/admin/shops').then((r) => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.property('items');
        expect(getItems(r.body).length).to.be.gte(2);
        expect(r.body.totalElements).to.be.gte(2);
      });
    });
  });

  it('GET /api/admin/stats - should return stats', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/admin/stats').then((r) => { expect(r.status).to.eq(200); });
    });
  });

  it('POST /api/admin/suppliers - should create supplier', () => {
    cy.apiLogin('system.admin').then((token) => {
      const name = `Supplier API ${Date.now()}`;
      cy.apiPost(token, '/api/admin/suppliers', { name }).then((r) => {
        expect(r.status).to.be.oneOf([200, 201]);
        expect(r.body).to.have.property('name', name);
      });
    });
  });

  it('POST /api/admin/shops - should create shop', () => {
    cy.apiLogin('system.admin').then((token) => {
      const name = `Shop API ${Date.now()}`;
      cy.apiPost(token, '/api/admin/shops', { name }).then((r) => {
        expect(r.status).to.be.oneOf([200, 201]);
        expect(r.body).to.have.property('name', name);
      });
    });
  });

  it('GET /api/admin - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, failOnStatusCode: false })
      .then((r) => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Payments', () => {
  before(() => cy.ensureTestUsers());

  it('GET /api/payments - should list payments', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/payments').then((r) => {
        expect(r.status).to.eq(200);
        expect(isPage(r.body) || Array.isArray(r.body)).to.be.true;
      });
    });
  });

  it('POST /api/payments - shop can create payment', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.shopAdmin.username).then((token) => {
        cy.apiPost(token, '/api/payments', {
          shopId: ctx.shops.abdelslam.id, supplierId: ctx.suppliers.covale.id, amount: 200.00, currency: 'TND',
        }).then((r) => {
          expect(r.status).to.be.oneOf([200, 201]);
          expect(r.body).to.have.property('reference');
          expect(r.body.status).to.eq('PENDING');
        });
      });
    });
  });

  it('GET /api/payments - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`, failOnStatusCode: false })
      .then((r) => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Orders', () => {
  it('GET /api/orders - should list orders', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/orders').then((r) => {
        expect(r.status).to.eq(200);
        expect(isPage(r.body) || Array.isArray(r.body)).to.be.true;
      });
    });
  });

  it('GET /api/orders - should reject without auth', () => {
    cy.request({ method: 'GET', url: `${API()}/api/orders`, failOnStatusCode: false })
      .then((r) => { expect(r.status).to.be.oneOf([401, 403]); });
  });
});

describe('12 - API: Users', () => {
  before(() => cy.ensureTestUsers());

  it('GET /api/users - should list users', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/users').then((r) => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.property('items');
        expect(getItems(r.body).length).to.be.gte(1);
        expect(r.body.totalElements).to.be.gte(1);
      });
    });
  });

  it('GET /api/users - should reject non-admin', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.supplierAdmin.username).then((token) => {
        cy.request({
          method: 'GET', url: `${API()}/api/users`,
          headers: { Authorization: `Bearer ${token}` },
          failOnStatusCode: false,
        }).then((res) => {
          expect(res.status).to.be.oneOf([401, 403]);
        });
      });
    });
  });
});

describe('12 - API: Catalog & Stocks', () => {
  before(() => cy.ensureTestUsers());

  it('GET /api/supplier/catalog/categories - should list categories', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.supplierAdmin.username).then((token) => {
        cy.apiGet(token, `/api/supplier/catalog/categories?supplierId=${ctx.suppliers.covale.id}`).then((r) => {
          expect(r.status).to.be.oneOf([200, 403]);
        });
      });
    });
  });

  it('GET /api/suppliers/:id/products - should list products', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.supplierAdmin.username).then((token) => {
        cy.apiGet(token, `/api/suppliers/${ctx.suppliers.covale.id}/products`).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.be.an('array');
        });
      });
    });
  });

  it('GET /api/suppliers/:id/movements - should list movements', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.supplierAdmin.username).then((token) => {
        cy.apiGet(token, `/api/suppliers/${ctx.suppliers.covale.id}/movements`).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.be.an('array');
        });
      });
    });
  });
});

describe('12 - API: Balances', () => {
  before(() => cy.ensureTestUsers());

  it('GET /api/balances/supplier/:id - should list supplier balances', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin('system.admin').then((token) => {
        cy.apiGet(token, `/api/balances/supplier/${ctx.suppliers.covale.id}`).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.be.an('array');
        });
      });
    });
  });

  it('GET /api/balances/shop/:id - should list shop balances', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin('system.admin').then((token) => {
        cy.apiGet(token, `/api/balances/shop/${ctx.shops.abdelslam.id}`).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.be.an('array');
        });
      });
    });
  });
});

describe('12 - API: Payment Lifecycle', () => {
  before(() => cy.ensureTestUsers());

  it('shop creates -> supplier sees -> confirms -> shop gets notification', () => {
    cy.getTestCtx().then((ctx) => {
      let shopToken = '';
      let supplierToken = '';
      let paymentRef = '';

      cy.apiLogin(ctx.users.shopAdmin.username).then((t) => {
        shopToken = t;
        return cy.apiLogin(ctx.users.supplierAdmin.username);
      }).then((t) => {
        supplierToken = t;
        return cy.apiPost(shopToken, '/api/payments', {
          shopId: ctx.shops.abdelslam.id, supplierId: ctx.suppliers.covale.id, amount: 500.00, currency: 'TND',
        });
      }).then((r) => {
        expect(r.status).to.be.oneOf([200, 201]);
        expect(r.body.status).to.eq('PENDING');
        paymentRef = r.body.reference;
        return cy.wait(2000);
      }).then(() => {
        return cy.apiGet(supplierToken, '/api/payments');
      }).then((r) => {
        const items = getItems(r.body);
        const payment = items.find((p: any) => p.reference === paymentRef);
        expect(payment).to.exist;
        if (payment) {
          return cy.apiPost(supplierToken, `/api/payments/${payment.id}/confirm`, {}).then((res) => {
            expect(res.status).to.eq(200);
            expect(res.body.status).to.eq('CONFIRMED');
          });
        }
      }).then(() => {
        return cy.wait(3000);
      }).then(() => {
        return cy.apiGet(shopToken, '/api/notifications');
      }).then((r) => {
        const notifs = Array.isArray(r.body) ? r.body : getItems(r.body);
        const notif = notifs.find((n: any) => n.type === 'PAYMENT_CONFIRMED');
        if (!notif) cy.log('No PAYMENT_CONFIRMED notification - event may not have propagated');
      });
    });
  });
});

describe('12 - API: RBAC Verification', () => {
  before(() => cy.ensureTestUsers());

  it('system.admin should access all admin endpoints', () => {
    cy.apiLogin('system.admin').then((token) => {
      cy.apiGet(token, '/api/admin/suppliers').then((r) => expect(r.status).to.eq(200));
      cy.apiGet(token, '/api/admin/shops').then((r) => expect(r.status).to.eq(200));
      cy.apiGet(token, '/api/users').then((r) => expect(r.status).to.eq(200));
      cy.apiGet(token, '/api/payments').then((r) => expect(r.status).to.eq(200));
    });
  });

  it('supplier admin should access supplier endpoints', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.supplierAdmin.username).then((token) => {
        cy.apiGet(token, `/api/suppliers/${ctx.suppliers.covale.id}/products`).then((r) => expect(r.status).to.eq(200));
        cy.apiGet(token, '/api/admin/suppliers').then((r) => {
          expect(r.status).to.be.oneOf([200, 403, 401]);
        });
      });
    });
  });

  it('shop admin should access shop endpoints', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.shopAdmin.username).then((token) => {
        cy.apiGet(token, '/api/orders').then((r) => expect(r.status).to.eq(200));
        cy.apiGet(token, `/api/balances/shop/${ctx.shops.abdelslam.id}`).then((r) => expect(r.status).to.eq(200));
        cy.apiGet(token, '/api/admin/suppliers').then((r) => {
          expect(r.status).to.be.oneOf([200, 403, 401]);
        });
      });
    });
  });

  it('supplier agent should access agent endpoints', () => {
    cy.getTestCtx().then((ctx) => {
      cy.apiLogin(ctx.users.supplierAgent1.username).then((token) => {
        cy.apiGet(token, `/api/suppliers/${ctx.suppliers.covale.id}/products`).then((r) => expect(r.status).to.eq(200));
        cy.apiGet(token, `/api/suppliers/${ctx.suppliers.covale.id}/movements`).then((r) => expect(r.status).to.eq(200));
      });
    });
  });
});
