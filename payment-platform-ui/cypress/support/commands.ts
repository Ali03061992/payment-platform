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

function ensureOrg(api: string, h: any, name: string, type: string) {
  const endpoint = type === 'SUPPLIER' ? 'suppliers' : 'shops';
  return cy.request({ method: 'GET', url: `${api}/api/admin/${endpoint}`, headers: h }).then((r) => {
    let existing = (r.body || []).find((o: any) => o.name === name);
    const createOrFind = existing ? cy.wrap(existing) :
      cy.request({ method: 'POST', url: `${api}/api/admin/${endpoint}`, headers: h, body: { name }, failOnStatusCode: false }).then((cr) => {
        if (cr.status === 200 || cr.status === 201) return cr.body;
        return cy.request({ method: 'GET', url: `${api}/api/admin/${endpoint}`, headers: h }).then((r2) => r2.body.find((o: any) => o.name === name));
      });
    return createOrFind.then((org: any) => {
      if (org && org.status !== 'ACTIVE') {
        return cy.request({ method: 'PATCH', url: `${api}/api/admin/${endpoint}/${org.id}/activate`, headers: h, failOnStatusCode: false }).then(() => org);
      }
      return cy.wrap(org);
    });
  });
}

Cypress.Commands.add('ensureTestUsers', () => {
  const api = API_URL();
  cy.request({ method: 'POST', url: `${api}/api/auth/login`, body: { username: 'system.admin', password: 'Admin@123' }, failOnStatusCode: false })
    .then((loginResp) => {
      if (loginResp.status !== 200) return;
      const h = authHeaders(loginResp.body.accessToken);

      ensureOrg(api, h, 'Covale', 'SUPPLIER').then((covale: any) => {
        ensureOrg(api, h, 'Pointteck', 'SUPPLIER').then((pointteck: any) => {
          ensureOrg(api, h, 'Abdelslam Tunis', 'SHOP').then((shopAbdelslam: any) => {
            ensureOrg(api, h, 'Ali Sfax', 'SHOP').then((shopAli: any) => {
              ensureOrg(api, h, 'Pointteck Tunis', 'SHOP').then((ptTunis: any) => {
                ensureOrg(api, h, 'Pointteck Sfax', 'SHOP').then((ptSfax: any) => {
                  const expectedPairs = [
                    { sid: covale.id, shopid: shopAbdelslam.id },
                    { sid: covale.id, shopid: shopAli.id },
                    { sid: pointteck.id, shopid: ptTunis.id },
                    { sid: pointteck.id, shopid: ptSfax.id },
                  ];
                  cy.request({ method: 'GET', url: `${api}/api/admin/supplier-shop-relations`, headers: h }).then((relResp) => {
                    const existingRels = relResp.body || [];
                    cy.wrap(expectedPairs).each((pair: any) => {
                      const exists = existingRels.find((r: any) => r.supplierId === pair.sid && r.shopId === pair.shopid);
                      if (!exists) {
                        cy.request({ method: 'POST', url: `${api}/api/admin/supplier-shop-relations`, headers: h, body: { supplierId: pair.sid, shopId: pair.shopid }, failOnStatusCode: false });
                      }
                    }).then(() => {
                      const users = [
                        { username: 'covale.admin', password: 'Admin@123', firstName: 'Covale', lastName: 'Admin', email: 'covale.admin@test.com', role: 'SUPPLIER_ADMIN', organizationId: covale.id },
                        { username: 'covale.agent1', password: 'Admin@123', firstName: 'Agent', lastName: 'Un', email: 'agent1@test.com', role: 'SUPPLIER_AGENT', organizationId: covale.id },
                        { username: 'pointteck.admin', password: 'Admin@123', firstName: 'Pointteck', lastName: 'Admin', email: 'pointteck.admin@test.com', role: 'SUPPLIER_ADMIN', organizationId: pointteck.id },
                        { username: 'pointteck.agent1', password: 'Admin@123', firstName: 'Agent', lastName: 'Pt1', email: 'pointteck.agent1@test.com', role: 'SUPPLIER_AGENT', organizationId: pointteck.id },
                        { username: 'abdelslam', password: 'Admin@123', firstName: 'Abdelslam', lastName: 'Tunis', email: 'abdelslam@test.com', role: 'SHOP_ADMIN', organizationId: shopAbdelslam.id },
                        { username: 'ali', password: 'Admin@123', firstName: 'Ali', lastName: 'Sfax', email: 'ali@test.com', role: 'SHOP_ADMIN', organizationId: shopAli.id },
                        { username: 'pointteck.tunis.admin', password: 'Admin@123', firstName: 'Pt', lastName: 'Tunis', email: 'pt.tunis@test.com', role: 'SHOP_ADMIN', organizationId: ptTunis.id },
                        { username: 'pointteck.sfax.admin', password: 'Admin@123', firstName: 'Pt', lastName: 'Sfax', email: 'pt.sfax@test.com', role: 'SHOP_ADMIN', organizationId: ptSfax.id },
                      ];
                      cy.wrap(users).each((u: any) => {
                        cy.request({ method: 'POST', url: `${api}/api/users`, headers: h, body: u, failOnStatusCode: false });
                      }).then(() => {
                        Cypress.env('covaleId', covale.id);
                        Cypress.env('pointteckId', pointteck.id);
                        Cypress.env('shopAbdelslamId', shopAbdelslam.id);
                        Cypress.env('shopAliId', shopAli.id);
                        Cypress.env('shopPtTunisId', ptTunis.id);
                        Cypress.env('shopPtSfaxId', ptSfax.id);
                      });
                    });
                  });
                });
              });
            });
          });
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
