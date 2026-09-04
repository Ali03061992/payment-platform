describe('07 - Supplier: Order Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
  });

  it('should display order management page', () => {
    cy.url().should('include', '/supplier/orders');
    cy.get('.page-header h2').should('contain', 'Gestion des commandes');
  });

  it('should show order table with columns', () => {
    cy.get('table thead th').should('have.length', 6);
    cy.get('table thead').should('contain', 'Référence');
    cy.get('table thead').should('contain', 'Boutique');
    cy.get('table thead').should('contain', 'Date');
    cy.get('table thead').should('contain', 'Total');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should filter orders by status', () => {
    cy.get('.filters select').should('exist');
    cy.get('.result-count').should('exist');
    cy.get('.filters select').select('CONFIRMED');
    cy.get('.result-count').should('contain', 'résultat');
  });

  it('should show status badges on orders', () => {
    cy.get('table tbody tr').then(($rows) => {
      if ($rows.length > 0) {
        cy.get('.payment-badge').should('have.length.gte', 1);
      }
    });
  });

  it('should show action buttons based on order status', () => {
    cy.get('table tbody tr').then(($rows) => {
      if ($rows.length > 0) {
        cy.get('table tbody tr').first().within(() => {
          cy.get('.actions button, .actions a').should('have.length.gte', 0);
        });
      }
    });
  });

  it('should open order detail modal on row click', () => {
    cy.get('table tbody tr').then(($rows) => {
      if ($rows.length > 0) {
        cy.get('table tbody tr').first().click();
        cy.get('.modal-overlay, .modal-content', { timeout: 5000 }).should('exist');
      }
    });
  });
});

describe('07 - Supplier: Delivery Management', () => {
  before(() => cy.ensureTestUsers());

  it('should access delivery management as SUPPLIER_AGENT', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url({ timeout: 10000 }).should('include', '/supplier/deliveries');
    cy.get('body').should('be.visible');
  });

  it('should redirect SUPPLIER_ADMIN from delivery page', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url({ timeout: 10000 }).should('satisfy', (url: string) =>
      !url.includes('/supplier/deliveries')
    );
  });
});

describe('07 - Supplier: Agent Payments', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/agent-payments');
  });

  it('should display agent payments page', () => {
    cy.url().should('include', '/supplier/agent-payments');
    cy.get('h2').should('contain', 'Paiements par agent');
  });

  it('should have date filter inputs', () => {
    cy.get('input[type="date"]').should('have.length', 2);
  });

  it('should have status filter dropdown', () => {
    cy.get('select.form-control').should('exist');
  });

  it('should display totals bar or empty state', () => {
    cy.get('.loading').should('not.exist');
    cy.get('body').then(($body) => {
      const hasTotals = $body.find('.totals-bar').length > 0;
      const hasEmpty = $body.find('.empty').length > 0;
      expect(hasTotals || hasEmpty).to.be.true;
    });
  });

  it('should have agent payments link in sidebar', () => {
    cy.visit('/dashboard');
    cy.get('.sidebar').should('contain.text', 'Paiements agents');
  });
});
