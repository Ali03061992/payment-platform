const API_URL = () => Cypress.env('apiUrl') || 'http://localhost:8081';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

Cypress.Commands.add('login', (username: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${API_URL()}/api/auth/login`,
    body: { username, password },
    failOnStatusCode: false,
  }).then((resp) => {
    if (resp.status !== 200) {
      cy.log(`Login failed for ${username}: ${resp.status} - ${JSON.stringify(resp.body)}`);
      return;
    }
    expect(resp.body).to.have.property('accessToken');
    const token = resp.body.accessToken;
    cy.window().then((win) => {
      win.sessionStorage.setItem('token', token);
      win.sessionStorage.setItem('user', JSON.stringify(resp.body.user));
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
  return cy.request({ method: 'GET', url: `${api}/api/admin/${endpoint}`, headers: h, failOnStatusCode: false }).then((r) => {
    if (r.status !== 200) return cy.wrap(null);
    const list = Array.isArray(r.body) ? r.body : (r.body.items || r.body.value || []);
    let existing = list.find((o: any) => o.name === name || o.name.includes(name) || name.includes(o.name));
    const createOrFind = existing ? cy.wrap(existing) :
      cy.request({ method: 'POST', url: `${api}/api/admin/${endpoint}`, headers: h, body: { name }, failOnStatusCode: false }).then((cr) => {
        if (cr.status === 200 || cr.status === 201) return cr.body;
        return cy.request({ method: 'GET', url: `${api}/api/admin/${endpoint}`, headers: h, failOnStatusCode: false })
          .then((r2) => {
            if (r2.status !== 200) return null;
            const l = Array.isArray(r2.body) ? r2.body : (r2.body.items || r2.body.value || []);
            return l.find((o: any) => o.name === name || o.name.includes(name) || name.includes(o.name));
          });
      });
    return createOrFind.then((org: any) => {
      if (!org) return cy.wrap(null);
      if (org.status !== 'ACTIVE') {
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
      if (loginResp.status !== 200) {
        cy.log('Cannot login as system.admin - skipping seed');
        return;
      }
      const h = authHeaders(loginResp.body.accessToken);

      ensureOrg(api, h, 'Covale', 'SUPPLIER').then((covale: any) => {
        ensureOrg(api, h, 'Pointteck', 'SUPPLIER').then((pointteck: any) => {
          ensureOrg(api, h, 'Abdelslam Tunis', 'SHOP').then((shopAbdelslam: any) => {
            ensureOrg(api, h, 'Ali Sfax', 'SHOP').then((shopAli: any) => {
              ensureOrg(api, h, 'Pointteck Tunis', 'SHOP').then((ptTunis: any) => {
                ensureOrg(api, h, 'Pointteck Sfax', 'SHOP').then((ptSfax: any) => {
                  const covaleId = covale?.id;
                  const pointteckId = pointteck?.id;
                  const shopAbdelslamId = shopAbdelslam?.id;
                  const shopAliId = shopAli?.id;
                  const ptTunisId = ptTunis?.id;
                  const ptSfaxId = ptSfax?.id;

                  if (!covaleId || !pointteckId || !shopAbdelslamId || !shopAliId || !ptTunisId || !ptSfaxId) {
                    cy.log('Missing org IDs - some seed data may be incomplete');
                  }

                  const pairs = [
                    covaleId && shopAbdelslamId ? { sid: covaleId, shopid: shopAbdelslamId } : null,
                    covaleId && shopAliId ? { sid: covaleId, shopid: shopAliId } : null,
                    pointteckId && ptTunisId ? { sid: pointteckId, shopid: ptTunisId } : null,
                    pointteckId && ptSfaxId ? { sid: pointteckId, shopid: ptSfaxId } : null,
                  ].filter(Boolean);

                  cy.request({ method: 'GET', url: `${api}/api/admin/supplier-shop-relations`, headers: h, failOnStatusCode: false }).then((relResp) => {
                    const existingRels = relResp.status === 200 ? (relResp.body || []) : [];
                    cy.wrap(pairs).each((pair: any) => {
                      const exists = existingRels.find((r: any) => r.supplierId === pair.sid && r.shopId === pair.shopid);
                      if (!exists) {
                        cy.request({ method: 'POST', url: `${api}/api/admin/supplier-shop-relations`, headers: h, body: { supplierId: pair.sid, shopId: pair.shopid }, failOnStatusCode: false });
                      }
                    }).then(() => {
                      const users = [
                        { username: 'covale.admin', password: 'Admin@123', firstName: 'Covale', lastName: 'Admin', email: 'covale.admin@test.com', role: 'SUPPLIER_ADMIN', organizationId: covaleId },
                        { username: 'covale.agent1', password: 'Admin@123', firstName: 'Agent', lastName: 'Un', email: 'agent1@test.com', role: 'SUPPLIER_AGENT', organizationId: covaleId },
                        { username: 'pointteck.admin', password: 'Admin@123', firstName: 'Pointteck', lastName: 'Admin', email: 'pointteck.admin@test.com', role: 'SUPPLIER_ADMIN', organizationId: pointteckId },
                        { username: 'pointteck.agent1', password: 'Admin@123', firstName: 'Agent', lastName: 'Pt1', email: 'pointteck.agent1@test.com', role: 'SUPPLIER_AGENT', organizationId: pointteckId },
                        { username: 'abdelslam', password: 'Admin@123', firstName: 'Abdelslam', lastName: 'Tunis', email: 'abdelslam@test.com', role: 'SHOP_ADMIN', organizationId: shopAbdelslamId },
                        { username: 'ali', password: 'Admin@123', firstName: 'Ali', lastName: 'Sfax', email: 'ali@test.com', role: 'SHOP_ADMIN', organizationId: shopAliId },
                        { username: 'pointteck.tunis.admin', password: 'Admin@123', firstName: 'Pt', lastName: 'Tunis', email: 'pt.tunis@test.com', role: 'SHOP_ADMIN', organizationId: ptTunisId },
                        { username: 'pointteck.sfax.admin', password: 'Admin@123', firstName: 'Pt', lastName: 'Sfax', email: 'pt.sfax@test.com', role: 'SHOP_ADMIN', organizationId: ptSfaxId },
                      ];
                      cy.wrap(users).each((u: any) => {
                        cy.request({ method: 'POST', url: `${api}/api/users`, headers: h, body: u, failOnStatusCode: false });
                      }).then(() => {
                        Cypress.env('covaleId', covaleId);
                        Cypress.env('pointteckId', pointteckId);
                        Cypress.env('shopAbdelslamId', shopAbdelslamId);
                        Cypress.env('shopAliId', shopAliId);
                        Cypress.env('shopPtTunisId', ptTunisId);
                        Cypress.env('shopPtSfaxId', ptSfaxId);
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
