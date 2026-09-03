function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

const TEST_DATA = {
  systemAdmin: { username: 'system.admin', password: 'Admin@123' },
  covaleAdmin: { username: 'covale.admin', password: 'Admin@123' },
  pointteckAdmin: { username: 'pointteck.admin', password: 'Admin@123' },
  covaleAgent1: { username: 'covale.agent1', password: 'Admin@123' },
  covaleAgent2: { username: 'covale.agent2', password: 'Admin@123' },
  pointteckAgent1: { username: 'pointteck.agent1', password: 'Admin@123' },
  pointteckAgent2: { username: 'pointteck.agent2', password: 'Admin@123' },
  abdelslam: { username: 'abdelslam', password: 'Admin@123' },
  ali: { username: 'ali', password: 'Admin@123' },
  pointteckTunisAdmin: { username: 'pointteck.tunis.admin', password: 'Admin@123' },
  pointteckSfaxAdmin: { username: 'pointteck.sfax.admin', password: 'Admin@123' },
};

describe('Data Structure - Authentication', () => {
  it('system.admin should login and have SYSTEM_ADMIN role', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: TEST_DATA.systemAdmin
    }).then(r => {
      expect(r.status).to.eq(200);
      expect(r.body.accessToken).to.exist;
      cy.request({
        method: 'GET', url: `${API()}/api/auth/me`,
        headers: authHeaders(r.body.accessToken)
      }).then(me => {
        expect(me.body.username).to.eq('system.admin');
        expect(me.body.roles).to.include('SYSTEM_ADMIN');
        expect(me.body.organizationId).to.be.null;
      });
    });
  });

  it('covale.admin should login and have SUPPLIER_ADMIN role with orgId=1', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: TEST_DATA.covaleAdmin
    }).then(r => {
      cy.request({
        method: 'GET', url: `${API()}/api/auth/me`,
        headers: authHeaders(r.body.accessToken)
      }).then(me => {
        expect(me.body.username).to.eq('covale.admin');
        expect(me.body.roles).to.include('SUPPLIER_ADMIN');
        expect(me.body.organizationId).to.eq(1);
      });
    });
  });

  it('pointteck.admin should login and have SUPPLIER_ADMIN role with orgId=2', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/auth/login`,
      body: TEST_DATA.pointteckAdmin
    }).then(r => {
      cy.request({
        method: 'GET', url: `${API()}/api/auth/me`,
        headers: authHeaders(r.body.accessToken)
      }).then(me => {
        expect(me.body.username).to.eq('pointteck.admin');
        expect(me.body.roles).to.include('SUPPLIER_ADMIN');
        expect(me.body.organizationId).to.eq(2);
      });
    });
  });

  it('covale agents should login with SUPPLIER_AGENT role orgId=1', () => {
    [TEST_DATA.covaleAgent1, TEST_DATA.covaleAgent2].forEach(user => {
      cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: user })
        .then(r => {
          cy.request({
            method: 'GET', url: `${API()}/api/auth/me`,
            headers: authHeaders(r.body.accessToken)
          }).then(me => {
            expect(me.body.roles).to.include('SUPPLIER_AGENT');
            expect(me.body.organizationId).to.eq(1);
          });
        });
    });
  });

  it('pointteck agents should login with SUPPLIER_AGENT role orgId=2', () => {
    [TEST_DATA.pointteckAgent1, TEST_DATA.pointteckAgent2].forEach(user => {
      cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: user })
        .then(r => {
          cy.request({
            method: 'GET', url: `${API()}/api/auth/me`,
            headers: authHeaders(r.body.accessToken)
          }).then(me => {
            expect(me.body.roles).to.include('SUPPLIER_AGENT');
            expect(me.body.organizationId).to.eq(2);
          });
        });
    });
  });

  it('abdelslam (Covale Tunis Sousse) should login with SHOP_ADMIN orgId=3', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.abdelslam })
      .then(r => {
        cy.request({
          method: 'GET', url: `${API()}/api/auth/me`,
          headers: authHeaders(r.body.accessToken)
        }).then(me => {
          expect(me.body.username).to.eq('abdelslam');
          expect(me.body.roles).to.include('SHOP_ADMIN');
          expect(me.body.organizationId).to.eq(3);
        });
      });
  });

  it('ali (Covale Sfax Mahdiya) should login with SHOP_ADMIN orgId=4', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.ali })
      .then(r => {
        cy.request({
          method: 'GET', url: `${API()}/api/auth/me`,
          headers: authHeaders(r.body.accessToken)
        }).then(me => {
          expect(me.body.username).to.eq('ali');
          expect(me.body.roles).to.include('SHOP_ADMIN');
          expect(me.body.organizationId).to.eq(4);
        });
      });
  });

  it('pointteck shop admins should login with correct orgIds', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.pointteckTunisAdmin })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: authHeaders(r.body.accessToken) })
          .then(me => { expect(me.body.organizationId).to.eq(5); });
      });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.pointteckSfaxAdmin })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: authHeaders(r.body.accessToken) })
          .then(me => { expect(me.body.organizationId).to.eq(6); });
      });
  });
});

describe('Data Structure - Organizations & Relations', () => {
  let adminToken: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.systemAdmin })
      .then(r => { adminToken = r.body.accessToken; });
  });

  it('should have Covale (id=1) and Pointteck (id=2) as suppliers', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        const covale = r.body.find((s: any) => s.name === 'Covale');
        const pointteck = r.body.find((s: any) => s.name === 'Pointteck');
        expect(covale).to.exist;
        expect(covale.id).to.eq(1);
        expect(covale.status).to.eq('ACTIVE');
        expect(pointteck).to.exist;
        expect(pointteck.id).to.eq(2);
        expect(pointteck.status).to.eq('ACTIVE');
      });
  });

  it('should have the 4 Covale/Pointteck shops with correct names and IDs', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/shops`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        const shops = r.body;
        const tunisSoussa = shops.find((s: any) => s.name === 'Tunis Sousse');
        const sfaxMahdiya = shops.find((s: any) => s.name === 'Sfax Mahdiya');
        const tunisCentre = shops.find((s: any) => s.name === 'Tunis Centre');
        const sfaxVille = shops.find((s: any) => s.name === 'Sfax Ville');
        expect(tunisSoussa).to.exist;
        expect(tunisSoussa.id).to.eq(3);
        expect(sfaxMahdiya).to.exist;
        expect(sfaxMahdiya.id).to.eq(4);
        expect(tunisCentre).to.exist;
        expect(tunisCentre.id).to.eq(5);
        expect(sfaxVille).to.exist;
        expect(sfaxVille.id).to.eq(6);
        [tunisSoussa, sfaxMahdiya, tunisCentre, sfaxVille].forEach(s => expect(s.status).to.eq('ACTIVE'));
      });
  });

  it('should have exactly 4 active supplier-shop relations', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/supplier-shop-relations`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.length(4);
        const rels = r.body;
        expect(rels.find((r: any) => r.supplierId === 1 && r.shopId === 3)).to.exist;
        expect(rels.find((r: any) => r.supplierId === 1 && r.shopId === 4)).to.exist;
        expect(rels.find((r: any) => r.supplierId === 2 && r.shopId === 5)).to.exist;
        expect(rels.find((r: any) => r.supplierId === 2 && r.shopId === 6)).to.exist;
        rels.forEach((r: any) => expect(r.status).to.eq('ACTIVE'));
      });
  });

  it('Covale (id=1) should have relations with shops 3 and 4', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/supplier-shop-relations/supplier/1`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.length(2);
        const shopIds = r.body.map((r: any) => r.shopId).sort();
        expect(shopIds).to.deep.eq([3, 4]);
      });
  });

  it('Pointteck (id=2) should have relations with shops 5 and 6', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/supplier-shop-relations/supplier/2`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.length(2);
        const shopIds = r.body.map((r: any) => r.shopId).sort();
        expect(shopIds).to.deep.eq([5, 6]);
      });
  });

  it('Shop 3 (Tunis Sousse) should have relation with supplier 1', () => {
    cy.request({ method: 'GET', url: `${API()}/api/admin/supplier-shop-relations/shop/3`, headers: authHeaders(adminToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        expect(r.body).to.have.length(1);
        expect(r.body[0].supplierId).to.eq(1);
      });
  });
});

describe('Data Structure - Role-based Access Control', () => {
  it('system.admin should access admin endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.systemAdmin })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/admin/shops`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/users`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
      });
  });

  it('covale.admin should access supplier endpoints but not admin endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.covaleAdmin })
      .then(r => {
        // Supplier endpoints use /api/suppliers/{supplierId}/products
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/products`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/movements`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/admin/suppliers`, headers: authHeaders(r.body.accessToken), failOnStatusCode: false })
          .then(res => expect(res.status).to.be.oneOf([403, 401]));
      });
  });

  it('pointteck.admin should access supplier endpoints for org 2', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.pointteckAdmin })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/2/products`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
      });
  });

  it('abdelslam (shop admin org 3) should access shop endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.abdelslam })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/orders`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/balances/shop/3`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
      });
  });

  it('ali (shop admin org 4) should access shop endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.ali })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/orders`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
      });
  });

  it('covale agents should access supplier agent endpoints', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.covaleAgent1 })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/products`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/1/movements`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
      });
  });

  it('pointteck agents should access supplier agent endpoints for org 2', () => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.pointteckAgent1 })
      .then(r => {
        cy.request({ method: 'GET', url: `${API()}/api/suppliers/2/products`, headers: authHeaders(r.body.accessToken) })
          .then(res => expect(res.status).to.eq(200));
      });
  });
});

describe('Data Structure - Payments Flow', () => {
  let shopToken: string;
  let supplierToken: string;
  let paymentRef: string;

  before(() => {
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.abdelslam })
      .then(r => { shopToken = r.body.accessToken; });
    cy.request({ method: 'POST', url: `${API()}/api/auth/login`, body: TEST_DATA.covaleAdmin })
      .then(r => { supplierToken = r.body.accessToken; });
  });

  it('shop (abdelslam, org 3) can create payment to supplier 1', () => {
    cy.request({
      method: 'POST', url: `${API()}/api/payments`, headers: authHeaders(shopToken),
      body: { shopId: 3, supplierId: 1, amount: 500.00, currency: 'EUR' }
    }).then(r => {
      expect(r.status).to.be.oneOf([200, 201]);
      expect(r.body).to.have.property('reference');
      expect(r.body.shopId).to.eq(3);
      expect(r.body.supplierId).to.eq(1);
      expect(r.body.status).to.eq('PENDING');
      paymentRef = r.body.reference;
    });
  });

  it('supplier (covale.admin, org 1) can see the payment', () => {
    cy.wait(2000);
    cy.request({ method: 'GET', url: `${API()}/api/payments`, headers: authHeaders(supplierToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        const payment = r.body.find((p: any) => p.reference === paymentRef);
        expect(payment).to.exist;
      });
  });

  it('supplier can confirm the payment', () => {
    cy.request({ method: 'GET', url: `${API()}/api/payments`, headers: authHeaders(supplierToken) })
      .then(r => {
        const payment = r.body.find((p: any) => p.reference === paymentRef);
        if (payment) {
          cy.request({
            method: 'POST', url: `${API()}/api/payments/${payment.id}/confirm`,
            headers: authHeaders(supplierToken)
          }).then(res => {
            expect(res.status).to.eq(200);
            expect(res.body.status).to.eq('CONFIRMED');
          });
        }
      });
  });

  it('shop receives notification after supplier confirms', () => {
    cy.wait(3000);
    cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(shopToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        const notif = r.body.find((n: any) => n.type === 'PAYMENT_CONFIRMED' && n.relatedEntityId === paymentRef);
        expect(notif).to.exist;
        expect(notif.message).to.contain('confirmé');
      });
  });

  it('supplier receives notification after shop creates payment', () => {
    cy.wait(1000);
    cy.request({ method: 'GET', url: `${API()}/api/notifications`, headers: authHeaders(supplierToken) })
      .then(r => {
        expect(r.status).to.eq(200);
        const notif = r.body.find((n: any) => n.type === 'PAYMENT_CREATED' && n.relatedEntityId === paymentRef);
        expect(notif).to.exist;
      });
  });
});

describe('Data Structure - UI Navigation', () => {
  it('system.admin should see all nav items', () => {
    cy.login('system.admin', 'Admin@123');
    cy.visit('/dashboard');
    cy.get('.sidebar-nav .nav-item').should('have.length.at.least', 10);
  });

  it('covale.admin should see supplier nav items', () => {
    cy.login('covale.admin', 'Admin@123');
    cy.visit('/dashboard');
    cy.get('.sidebar-nav .nav-item').should('have.length.at.least', 3);
    cy.contains('Catalogue').should('be.visible');
    cy.contains('Stock').should('be.visible');
    cy.contains('Commandes').should('be.visible');
    cy.contains('Livraisons').should('be.visible');
  });

  it('pointteck.admin should see supplier nav items', () => {
    cy.login('pointteck.admin', 'Admin@123');
    cy.visit('/dashboard');
    cy.contains('Catalogue').should('be.visible');
    cy.contains('Stock').should('be.visible');
  });

  it('abdelslam (shop admin) should see shop nav items', () => {
    cy.login('abdelslam', 'Admin@123');
    cy.visit('/dashboard');
    cy.contains('Mes commandes').should('be.visible');
    cy.contains('Nouvelle commande').should('be.visible');
    cy.contains('Balance').should('be.visible');
  });

  it('ali (shop admin) should see shop nav items', () => {
    cy.login('ali', 'Admin@123');
    cy.visit('/dashboard');
    cy.contains('Mes commandes').should('be.visible');
  });

  it('covale agents should see limited supplier nav items', () => {
    cy.login('covale.agent1', 'Admin@123');
    cy.visit('/dashboard');
    cy.contains('Stock').should('be.visible');
    cy.contains('Livraisons').should('be.visible');
    cy.contains('Catalogue').should('not.exist');
    cy.contains('Commandes').should('not.exist');
  });

  it('pointteck agents should see limited supplier nav items', () => {
    cy.login('pointteck.agent1', 'Admin@123');
    cy.visit('/dashboard');
    cy.contains('Stock').should('be.visible');
    cy.contains('Livraisons').should('be.visible');
  });
});