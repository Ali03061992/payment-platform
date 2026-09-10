const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

describe('04 - Admin: Supplier Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/suppliers');
  });

  it('should display supplier management page', () => {
    cy.url().should('include', '/admin/suppliers');
    cy.get('.page-header h2').should('contain', 'Fournisseurs');
  });

  it('should show supplier table with columns', () => {
    cy.get('table thead th').should('have.length', 4);
    cy.get('table thead').should('contain', 'Nom');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Créé le');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should display existing suppliers', () => {
    cy.get('table tbody tr').should('have.length.gte', 2);
  });

  it('should toggle create supplier form', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouveau fournisseur');
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card h3').should('contain', 'Créer un fournisseur');
    cy.get('.form-card input[name="name"]').should('exist');
  });

  it('should create a new supplier', () => {
    const name = `Supplier E2E ${Date.now()}`;
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card input[name="name"]').clear().type(name);
    cy.get('.form-card button.btn-primary').should('not.be.disabled').click();
    cy.get('table tbody tr', { timeout: 10000 }).should('contain', name);
  });

  it('should toggle supplier status', () => {
    cy.get('table tbody tr').first().within(() => {
      cy.get('button.btn-sm').click();
    });
    cy.wait(1000);
    cy.get('table tbody tr').first().within(() => {
      cy.get('button.btn-sm').click();
    });
    cy.wait(1000);
  });

  it('should have status badges', () => {
    cy.get('.status-badge').should('have.length.gte', 1);
  });
});

describe('04 - Admin: Shop Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/shops');
  });

  it('should display shop management page', () => {
    cy.url().should('include', '/admin/shops');
    cy.get('.page-header h2').should('contain', 'Boutiques');
  });

  it('should show shop table', () => {
    cy.get('table thead th').should('have.length', 4);
    cy.get('table tbody tr').should('have.length.gte', 4);
  });

  it('should toggle create shop form', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouvelle boutique');
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card input[name="name"]').should('exist');
  });

  it('should create a new shop', () => {
    const name = `Shop E2E ${Date.now()}`;
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card input[name="name"]').clear().type(name);
    cy.get('.form-card button.btn-primary').should('not.be.disabled').click();
    cy.get('table tbody tr', { timeout: 10000 }).should('contain', name);
  });
});

describe('04 - Admin: Relation Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/relations');
  });

  it('should display relation management page', () => {
    cy.url().should('include', '/admin/relations');
    cy.get('.page-header h2').should('contain', 'Relations Fournisseur-Boutique');
  });

  it('should show relation table with 4 relations', () => {
    cy.get('table tbody tr').should('have.length', 4);
    cy.get('table thead').should('contain', 'Fournisseur');
    cy.get('table thead').should('contain', 'Boutique');
  });

  it('should show all relations as ACTIVE', () => {
    cy.get('.status-badge.active').should('have.length.gte', 4);
  });

  it('should toggle create relation form', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouvelle relation');
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('select[name="supplier"]').should('exist');
    cy.get('select[name="shop"]').should('exist');
  });

  it('should have deactivate button for active relations', () => {
    cy.get('button.btn-danger').should('have.length.gte', 1);
    cy.get('button.btn-danger').first().should('contain', 'Désactiver');
  });
});

describe('04 - Admin: Organization Stats', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/org-stats');
  });

  it('should display organization statistics page', () => {
    cy.url().should('include', '/admin/org-stats');
    cy.get('body').should('be.visible');
  });
});
