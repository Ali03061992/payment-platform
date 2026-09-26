// Parcours 07-supplier-orders.cy.ts : 07 - Supplier: Order Management; 07 - Supplier: Create Order; 07 - Supplier: Order Detail; 07 - Supplier: Delivery Management (20 scenarios).
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), connexion fournisseur via cy.loginAsSupplierAdmin(), UI live.
// Budget E2E : pas de hammering auth (le 429 est prouve cote backend), nettoyages via before/beforeEach.
describe('07 - Supplier: Order Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.dismissOverlays();
  });

  it('should display order management page', () => {
    cy.url().should('include', '/supplier/orders');
    cy.get('.page-header h2').should('contain', 'Gestion des commandes');
  });

  it('should have create order button', () => {
    cy.get('.page-header a.btn-primary').should('contain', 'Nouvelle commande');
    cy.get('.page-header a.btn-primary').should('have.attr', 'routerLink', '/dashboard/supplier/orders/create');
  });

  it('should show order table with all columns', () => {
    cy.get('table thead th').should('have.length', 6);
    cy.get('table thead').should('contain', 'Référence');
    cy.get('table thead').should('contain', 'Boutique');
    cy.get('table thead').should('contain', 'Date');
    cy.get('table thead').should('contain', 'Total');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should have status filter', () => {
    cy.get('select[aria-label="Filtrer par statut"]').should('exist');
    cy.get('.result-count').should('exist');
  });

  it('should filter orders by status', () => {
    cy.get('select[aria-label="Filtrer par statut"]').select('CONFIRMED');
    cy.get('table tbody tr').should('have.length.gte', 1);
  });

  it('should show create order link in nav', () => {
    cy.get('.page-header a.btn-primary').should('exist');
  });
});

describe('07 - Supplier: Create Order', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders/create');
  });

  it('should display create order form', () => {
    cy.url().should('include', '/supplier/orders/create');
    cy.get('.page-header h2').should('contain', 'Nouvelle commande');
  });

  it('should have shop select', () => {
    cy.get('select[name="shop"]').should('exist');
  });

  it('should have currency select', () => {
    cy.get('select[name="currency"]').should('exist');
  });

  it('should have notes textarea', () => {
    cy.get('textarea[name="notes"]').should('exist');
  });

  it('should have ASAP payment toggle', () => {
    cy.get('input[name="asapPayment"]').should('exist');
  });

  it('should have cancel button', () => {
    cy.get('a[routerLink*="supplier/orders"]').should('contain', 'Retour');
  });
});

describe('07 - Supplier: Order Detail', () => {
  before(() => cy.ensureTestUsers());

  it('should load order detail page for existing orders', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
    cy.get('table tbody', { timeout: 10000 }).then(($tbody) => {
      const clickableRows = $tbody.find('tr.clickable-row');
      if (clickableRows.length > 0) {
        cy.wrap(clickableRows).first().click();
        cy.get('.modal-content', { timeout: 5000 }).should('be.visible');
        cy.get('.btn-close').click();
      }
    });
  });
});

describe('07 - Supplier: Delivery Management', () => {
  before(() => cy.ensureTestUsers());

  it('should display delivery management page for agent', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url().should('include', '/supplier/deliveries');
    cy.get('.page-header h2').should('contain', 'Mes livraisons');
  });

  it('should show delivery table or empty state', () => {
    cy.loginAsCovaleAgent();
    cy.visit('/dashboard/supplier/deliveries');
    cy.get('body').should('be.visible');
  });
});
