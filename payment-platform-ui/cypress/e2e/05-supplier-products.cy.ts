describe('05 - Supplier: Product Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/products');
    cy.dismissOverlays();
  });

  it('should display product management page', () => {
    cy.url().should('include', '/supplier/products');
    cy.get('.page-header h1').should('contain', 'Catalogue produits');
  });

  it('should show product table with all columns', () => {
    cy.get('table thead th').should('have.length', 8);
    cy.get('table thead').should('contain', 'SKU');
    cy.get('table thead').should('contain', 'Nom');
    cy.get('table thead').should('contain', 'Famille');
    cy.get('table thead').should('contain', 'Prix');
    cy.get('table thead').should('contain', 'Unite');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should have create product button', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouveau produit');
  });

  it('should toggle create product form', () => {
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card h3').should('contain', 'Creer un produit');
    cy.get('input[name="name"]').should('exist');
    cy.get('input[name="sku"]').should('exist');
    cy.get('.form-card input[name="description"]').should('exist');
    cy.get('select[name="familyId"]').should('exist');
    cy.get('select[name="categoryId"]').should('exist');
    cy.get('select[name="currency"]').should('exist');
    cy.get('input[name="unitPrice"]').should('exist');
    cy.get('input[name="minQuantity"]').should('exist');
  });

  it('should filter products by category', () => {
    cy.get('.filter-bar .filter-select').should('exist');
    cy.get('.result-count').should('exist');
  });

  it('should show product actions', () => {
    cy.get('table tbody tr').first().then(($row) => {
      if (!$row.find('.empty').length) {
        cy.wrap($row).within(() => {
          cy.get('button').should('have.length.gte', 2);
        });
      }
    });
  });
});

describe('05 - Supplier: Product CRUD', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/products');
    cy.dismissOverlays();
  });

  it('should create a new product', () => {
    const sku = `E2E-${Date.now()}`;
    cy.get('.page-header button.btn-primary').click();
    cy.dismissOverlays();
    cy.get('.form-card', { timeout: 10000 }).should('be.visible');
    cy.get('input[name="name"]').clear().type('Produit E2E Test');
    cy.get('input[name="sku"]').clear().type(sku);
    cy.get('.form-card input[name="description"]').clear().type('Description test');
    cy.get('input[name="unitPrice"]').clear().type('25.50');
    cy.get('input[name="minQuantity"]').clear().type('5');
    cy.get('.form-card button[type="submit"]').should('not.be.disabled').click();
    cy.get('table tbody tr', { timeout: 10000 }).should('contain', sku);
  });

  it('should cancel product creation', () => {
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card button[type="button"]').contains('Annuler').click();
    cy.get('.form-card').should('not.exist');
  });
});
