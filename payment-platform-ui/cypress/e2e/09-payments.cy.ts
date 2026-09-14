function isPage(body: any): boolean {
  return body && typeof body === 'object' && !Array.isArray(body) && 'items' in body;
}
function getItems(body: any): any[] {
  return isPage(body) ? body.items : (Array.isArray(body) ? body : []);
}

describe('09 - Payments: List', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments');
  });

  it('should display payments page', () => {
    cy.url().should('include', '/payments');
    cy.get('.page-header h2').should('contain', 'Paiements');
  });

  it('should have create payment button', () => {
    cy.get('button[routerLink="/dashboard/payments/create"]').should('contain', 'Nouveau paiement');
  });

  it('should navigate to create payment page', () => {
    cy.get('button[routerLink="/dashboard/payments/create"]').click();
    cy.url({ timeout: 5000 }).should('include', '/payments/create');
  });

  it('should show payment table with columns', () => {
    cy.get('.hide-mobile table thead th').should('have.length', 9);
    cy.get('.hide-mobile table thead').should('contain', 'Référence');
    cy.get('.hide-mobile table thead').should('contain', 'Boutique');
    cy.get('.hide-mobile table thead').should('contain', 'Créé par');
    cy.get('.hide-mobile table thead').should('contain', 'Fournisseur');
    cy.get('.hide-mobile table thead').should('contain', 'Montant');
    cy.get('.hide-mobile table thead').should('contain', 'Statut');
    cy.get('.hide-mobile table thead').should('contain', 'Traité par');
    cy.get('.hide-mobile table thead').should('contain', 'Créé le');
    cy.get('.hide-mobile table thead').should('contain', 'Actions');
  });

  it('should filter payments by status', () => {
    cy.get('.filters select').should('exist');
    cy.get('.result-count').should('exist');
    cy.get('.filters select').select('PENDING');
    cy.get('.result-count').should('contain', 'résultat');
  });

  it('should show payment status badges', () => {
    cy.get('body').then(($body) => {
      if ($body.find('.payment-badge').length > 0) {
        cy.get('.payment-badge').should('have.length.gte', 1);
      } else {
        cy.log('No payment badges found - payments may be empty');
      }
    });
  });

  it('should show confirm/cancel buttons for PENDING payments', () => {
    cy.get('.filters select').select('PENDING');
    cy.get('table tbody').then(($tbody) => {
      const text = $tbody.text();
      if (text.includes('Aucun paiement trouvé')) {
        cy.log('No PENDING payments - skip button check');
        return;
      }
      cy.get('table tbody tr').then(($rows) => {
        if ($rows.length > 0) {
          cy.get('button.btn-success').should('contain', 'Confirmer');
          cy.get('button.btn-danger').should('contain', 'Annuler');
        }
      });
    });
  });
});

describe('09 - Payments: Create', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments/create');
  });

  it('should display payment creation form', () => {
    cy.url().should('include', '/payments/create');
    cy.get('.form-card').should('exist');
    cy.get('.page-header h2').should('contain', 'Nouveau paiement');
  });

  it('should have shop and supplier selects', () => {
    cy.get('select[name="shop"]').should('exist');
    cy.get('select[name="supplier"]').should('exist');
  });

  it('should have amount input', () => {
    cy.get('input[name="amount"][type="number"]').should('exist');
  });

  it('should have currency select', () => {
    cy.get('select[name="currency"]').should('exist');
    cy.get('select[name="currency"] option').should('have.length', 3);
  });

  it('should have cancel button', () => {
    cy.get('button.btn-secondary[routerLink="/dashboard/payments"]').should('contain', 'Annuler');
  });

  it('should disable submit when fields empty', () => {
    cy.get('button.btn-primary').last().should('be.disabled');
  });

  it('should enable submit when all fields filled', () => {
    cy.get('select[name="shop"]').select(1);
    cy.get('select[name="supplier"]').select(1);
    cy.get('input[name="amount"]').clear().type('100');
    cy.get('button.btn-primary').last().should('not.be.disabled');
  });
});

describe('09 - Payments: Detail', () => {
  before(() => cy.ensureTestUsers());

  it('should navigate back on invalid payment id', () => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments/999999');
    cy.url({ timeout: 10000 }).should('satisfy', (url: string) =>
      url.includes('/payments') || url.includes('/dashboard')
    );
  });

  it('should display payment detail for valid payment', () => {
    let paymentId: string;
    cy.loginAsAdmin();
    cy.window().then((win) => {
      const token = win.sessionStorage.getItem('token');
      cy.request({
        method: 'GET',
        url: `${Cypress.env('apiUrl') || 'http://localhost:8081'}/api/payments`,
        headers: { Authorization: `Bearer ${token}` },
      }).then((r) => {
        const items = getItems(r.body);
        if (items.length > 0) {
          paymentId = items[0].id;
          cy.visit(`/dashboard/payments/${paymentId}`);
          cy.get('.detail-grid').should('exist');
          cy.get('.detail-card').should('have.length.gte', 5);
          cy.get('.detail-label').should('contain', 'Référence');
          cy.get('.detail-label').should('contain', 'Statut');
          cy.get('.detail-label').should('contain', 'Montant');
        }
      });
    });
  });

  it('should have QR Code toggle button', () => {
    cy.loginAsAdmin();
    cy.window().then((win) => {
      const token = win.sessionStorage.getItem('token');
      cy.request({
        method: 'GET', url: `${Cypress.env('apiUrl') || 'http://localhost:8081'}/api/payments`,
        headers: { Authorization: `Bearer ${token}` },
      }).then((r) => {
        const items = getItems(r.body);
        if (items.length > 0) {
          cy.visit(`/dashboard/payments/${items[0].id}`);
          cy.get('button:contains("QR Code")').should('exist');
        }
      });
    });
  });
});

describe('09 - Payments: Stats', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments/stats');
  });

  it('should display payment statistics page', () => {
    cy.url().should('include', '/payments/stats');
    cy.get('.page-header h2').should('contain', 'Statistiques');
  });

  it('should show stat cards', () => {
    cy.get('.stats-grid').should('exist');
    cy.get('.stat-card').should('have.length', 5);
    cy.get('.stat-card').eq(0).should('contain', 'Total');
    cy.get('.stat-card').eq(1).should('contain', 'En attente');
    cy.get('.stat-card').eq(2).should('contain', 'Confirmés');
    cy.get('.stat-card').eq(3).should('contain', 'Rejetés');
    cy.get('.stat-card').eq(4).should('contain', 'Annulés');
  });
});

describe('09 - Payments: Cancel Flow', () => {
  before(() => cy.ensureTestUsers());

  it('should allow shop admin to cancel their own payment', () => {
    const api = Cypress.env('apiUrl') || 'http://localhost:8081';
    let shopToken: string;
    let paymentId: string;

    cy.getTestCtx().then((ctx) => {
      cy.login(ctx.users.shopAli.username, 'test1234').then(() => {
        cy.window().then((win) => {
          shopToken = win.sessionStorage.getItem('token')!;
          cy.request({
            method: 'POST',
            url: `${api}/api/payments`,
            headers: { Authorization: `Bearer ${shopToken}` },
            body: { shopId: ctx.shops.ali.id, supplierId: ctx.suppliers.covale.id, amount: 100, currency: 'TND', description: 'Shop cancel test' },
          }).then((resp) => {
            expect(resp.status).to.eq(201);
            paymentId = resp.body.id;
            cy.request({
              method: 'POST',
              url: `${api}/api/payments/${paymentId}/cancel`,
              headers: { Authorization: `Bearer ${shopToken}` },
            }).then((r) => {
              expect(r.status).to.eq(200);
              expect(r.body.status).to.eq('CANCELLED');
            });
          });
        });
      });
    });
  });

  it('should allow supplier admin to cancel a payment', () => {
    const api = Cypress.env('apiUrl') || 'http://localhost:8081';
    let shopToken: string;
    let supplierToken: string;
    let paymentId: string;

    cy.getTestCtx().then((ctx) => {
      cy.login(ctx.users.shopAli.username, 'test1234').then(() => {
        cy.window().then((win) => {
          shopToken = win.sessionStorage.getItem('token')!;
          cy.request({
            method: 'POST',
            url: `${api}/api/payments`,
            headers: { Authorization: `Bearer ${shopToken}` },
            body: { shopId: ctx.shops.ali.id, supplierId: ctx.suppliers.covale.id, amount: 200, currency: 'TND', description: 'Supplier cancel test' },
          }).then((resp) => {
            expect(resp.status).to.eq(201);
            paymentId = resp.body.id;

            cy.login(ctx.users.supplierAdmin.username, 'test1234').then(() => {
              cy.window().then((win2) => {
                supplierToken = win2.sessionStorage.getItem('token')!;
                cy.request({
                  method: 'POST',
                  url: `${api}/api/payments/${paymentId}/cancel`,
                  headers: { Authorization: `Bearer ${supplierToken}` },
                }).then((r) => {
                  expect(r.status).to.eq(200);
                  expect(r.body.status).to.eq('CANCELLED');
                });
              });
            });
          });
        });
      });
    });
  });

  it('should reject shop cancel on another shop payment', () => {
    const api = Cypress.env('apiUrl') || 'http://localhost:8081';
    let shopTokenAli: string;
    let shopTokenAbdelslam: string;
    let paymentId: string;

    cy.getTestCtx().then((ctx) => {
      cy.login(ctx.users.shopAdmin.username, 'test1234').then(() => {
        cy.window().then((win) => {
          shopTokenAbdelslam = win.sessionStorage.getItem('token')!;
          cy.request({
            method: 'POST',
            url: `${api}/api/payments`,
            headers: { Authorization: `Bearer ${shopTokenAbdelslam}` },
            body: { shopId: ctx.shops.abdelslam.id, supplierId: ctx.suppliers.covale.id, amount: 50, currency: 'TND', description: 'Abdelslam payment' },
          }).then((resp) => {
            expect(resp.status).to.eq(201);
            paymentId = resp.body.id;

            cy.login(ctx.users.shopAli.username, 'test1234').then(() => {
              cy.window().then((win2) => {
                shopTokenAli = win2.sessionStorage.getItem('token')!;
                cy.request({
                  method: 'POST',
                  url: `${api}/api/payments/${paymentId}/cancel`,
                  headers: { Authorization: `Bearer ${shopTokenAli}` },
                  failOnStatusCode: false,
                }).then((r) => {
                  expect(r.status).to.eq(403);
                });
              });
            });
          });
        });
      });
    });
  });

  it('should reject cancel on already confirmed payment', () => {
    const api = Cypress.env('apiUrl') || 'http://localhost:8081';
    let shopToken: string;
    let supplierToken: string;
    let paymentId: string;

    cy.getTestCtx().then((ctx) => {
      cy.login(ctx.users.shopAli.username, 'test1234').then(() => {
        cy.window().then((win) => {
          shopToken = win.sessionStorage.getItem('token')!;
          cy.request({
            method: 'POST',
            url: `${api}/api/payments`,
            headers: { Authorization: `Bearer ${shopToken}` },
            body: { shopId: ctx.shops.ali.id, supplierId: ctx.suppliers.covale.id, amount: 300, currency: 'TND', description: 'Confirm then cancel test' },
          }).then((resp) => {
            expect(resp.status).to.eq(201);
            paymentId = resp.body.id;

            cy.login(ctx.users.supplierAdmin.username, 'test1234').then(() => {
              cy.window().then((win2) => {
                supplierToken = win2.sessionStorage.getItem('token')!;
                cy.request({
                  method: 'POST',
                  url: `${api}/api/payments/${paymentId}/confirm`,
                  headers: { Authorization: `Bearer ${supplierToken}` },
                }).then(() => {
                  cy.request({
                    method: 'POST',
                    url: `${api}/api/payments/${paymentId}/cancel`,
                    headers: { Authorization: `Bearer ${shopToken}` },
                    failOnStatusCode: false,
                  }).then((r) => {
                    expect(r.status).to.be.oneOf([400, 409]);
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
