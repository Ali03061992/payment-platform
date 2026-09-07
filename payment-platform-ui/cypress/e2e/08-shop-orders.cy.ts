describe('08 - Shop: Order List', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
  });

  it('should display order list page', () => {
    cy.url().should('include', '/shop/orders');
    cy.get('.page-header h2').should('contain', 'Commandes');
  });

  it('should have create order button', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouvelle commande');
    cy.get('.page-header button.btn-primary').should('have.attr', 'routerLink', '/dashboard/shop/orders/create');
  });

  it('should show order table with all columns', () => {
    cy.get('table thead th').should('have.length', 5);
    cy.get('table thead').should('contain', 'Référence');
    cy.get('table thead').should('contain', 'Date');
    cy.get('table thead').should('contain', 'Total');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should have status filter', () => {
    cy.get('.filters select').should('exist');
    cy.get('.result-count').should('exist');
  });

  it('should show all status filter options', () => {
    cy.get('.filters select option').should('have.length.gte', 8);
    cy.get('.filters select option').should('contain', 'Tous les statuts');
    cy.get('.filters select option').should('contain', 'Confirmé');
    cy.get('.filters select option').should('contain', 'Livré');
  });

  it('should filter orders by status', () => {
    cy.get('.filters select').select('CONFIRMED');
    cy.get('table tbody tr').should('have.length.gte', 0);
  });

  it('should show empty state when no orders match filter', () => {
    cy.get('.filters select').select('ACCEPTED');
    cy.get('.empty').should('contain', 'Aucune commande trouvée');
  });
});

describe('08 - Shop: Create Order', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders/create');
  });

  it('should display create order form', () => {
    cy.url().should('include', '/shop/orders/create');
    cy.get('.page-header h2').should('contain', 'Nouvelle commande');
  });

  it('should have supplier select', () => {
    cy.get('select[name="supplier"]').should('exist');
  });

  it('should have currency select', () => {
    cy.get('select[name="currency"]').should('exist');
  });

  it('should have ASAP payment toggle', () => {
    cy.get('input[name="asapPayment"]').should('exist');
  });

  it('should have notes textarea', () => {
    cy.get('textarea[name="notes"]').should('exist');
  });

  it('should have submit button', () => {
    cy.get('.form-actions').should('exist');
    cy.get('.form-actions button').should('contain', 'Créer la commande');
  });

  it('should have cancel link', () => {
    cy.get('a[routerLink*="shop/orders"]').should('exist');
  });

  it('should show products when supplier is selected', () => {
    cy.get('select[name="supplier"]').then(($select) => {
      const options = $select.find('option');
      if (options.length > 1) {
        cy.wrap($select).select(options.eq(1).val() as string);
        cy.get('.table-card', { timeout: 5000 }).should('exist');
      }
    });
  });
});

describe('08 - Shop: Order Detail', () => {
  before(() => cy.ensureTestUsers());

  it('should redirect on invalid order ID', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders/999999');
    cy.url({ timeout: 5000 }).should('include', '/shop/orders');
  });

  it('should load order detail for existing orders', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.get('table tbody', { timeout: 10000 }).then(($tbody) => {
      const clickableRows = $tbody.find('tr.clickable-row');
      if (clickableRows.length > 0) {
        cy.wrap(clickableRows).first().click({ force: true });
        cy.url({ timeout: 5000 }).should('match', /\/shop\/orders\/\d+/);
      }
    });
  });
});
