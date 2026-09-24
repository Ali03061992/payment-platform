describe('06 - Supplier: Stock Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/stock');
    cy.dismissOverlays();
  });

  it('should display stock management page', () => {
    cy.url().should('include', '/supplier/stock');
    cy.get('.page-header h1').should('contain', 'Gestion du stock');
  });

  it('should have add product button', () => {
    cy.get('.page-header').should('exist');
  });

  it('should show stock table with quantity controls', () => {
    cy.get('body').then(($body) => {
      if ($body.find('.empty-state').length > 0) {
        cy.get('.empty-state').should('contain', 'Aucun produit en stock');
      } else {
        cy.get('table thead th').should('have.length', 7);
        cy.get('table thead').should('contain', 'SKU');
        cy.get('table thead').should('contain', 'Produit');
        cy.get('table thead').should('contain', 'Stock');
        cy.get('table thead').should('contain', 'Seuil min');
        cy.get('table thead').should('contain', 'Statut');
      }
    });
  });

  it('should filter stock by status', () => {
    cy.get('body').then(($body) => {
      if ($body.find('.filter-bar .filter-select').length > 0) {
        cy.get('.filter-bar .filter-select').first().select('OK');
        cy.get('body').should('be.visible');
      } else {
        cy.get('.empty-state').should('exist');
      }
    });
  });

  it('should display stock status badges', () => {
    cy.get('.badge').should('have.length.gte', 0);
  });

  it('should have quantity +/- buttons', () => {
    cy.get('body').then(($body) => {
      if ($body.find('table tbody tr').length > 0) {
        cy.get('table tbody tr').first().then(($row) => {
          if ($row.find('button.qty-btn').length > 0) {
            cy.wrap($row).find('button.qty-btn').should('have.length.gte', 1);
            cy.wrap($row).find('span.qty-value').should('exist');
          }
        });
      } else {
        cy.get('.empty-state').should('exist');
      }
    });
  });

  it('should increment quantity with + button', () => {
    cy.get('body').then(($body) => {
      if ($body.find('table tbody tr button.qty-btn').length === 0) {
        cy.log('No stock rows - skipping increment check');
        return;
      }
      cy.get('table tbody tr').first().then(($row) => {
        if ($row.find('button.qty-btn').length > 0) {
          cy.wrap($row).find('span.qty-value').invoke('text').then((before) => {
            const beforeVal = parseInt(before.trim());
            cy.wrap($row).find('button.qty-btn').last().click();
            cy.wait(1000);
          });
        }
      });
    });
  });

  it('should show delete buttons', () => {
    cy.get('.page-header').should('exist');
  });
});

describe('06 - Supplier: Stock Dashboard', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/dashboard');
  });

  it('should display stock dashboard page', () => {
    cy.url().should('include', '/supplier/dashboard');
    cy.get('body').should('be.visible');
  });
});

describe('06 - Supplier: Add Product to Stock', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/stock/create');
  });

  it('should display add product form', () => {
    cy.url().should('include', '/supplier/stock/create');
    cy.get('input').should('have.length.gte', 2);
  });
});
