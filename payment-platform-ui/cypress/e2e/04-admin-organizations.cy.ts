// Parcours 04-admin-organizations.cy.ts : 04 - Admin: Supplier Management; 04 - Admin: Shop Management; 04 - Admin: Relation Management; 04 - Admin: Organization Stats (21 scenarios).
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), connexion admin via cy.loginAsAdmin(), UI live.
// Budget E2E : pas de hammering auth (le 429 est prouve cote backend), nettoyages via before/beforeEach.
describe('04 - Admin: Supplier Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/suppliers');
    cy.dismissOverlays();
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
    const name = `Supplier Auto ${Date.now()}`;
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card input[name="name"]').clear().type(name);
    cy.get('.form-card button.btn-primary').should('not.be.disabled').click();
    cy.get('table tbody tr', { timeout: 10000 }).should('contain', name);
  });

  it('should toggle supplier status', () => {
    cy.get('table tbody tr', { timeout: 10000 }).should('have.length.gte', 1);
    cy.get('table tbody tr').first().within(() => {
      cy.get('button.btn-sm').should('exist').click();
    });
    cy.wait(1000);
    cy.dismissOverlays();
    cy.get('table tbody tr').first().within(() => {
      cy.get('button.btn-sm').should('exist').click();
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
    cy.dismissOverlays();
  });

  it('should display shop management page', () => {
    cy.url().should('include', '/admin/shops');
    cy.get('.page-header h2').should('contain', 'Boutiques');
  });

  it('should show shop table', () => {
    cy.get('table thead th').should('have.length', 4);
    cy.get('table tbody tr').should('have.length.gte', 2);
  });

  it('should toggle create shop form', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouvelle boutique');
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card input[name="name"]').should('exist');
  });

  it('should create a new shop', () => {
    const name = `Shop Auto ${Date.now()}`;
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
    cy.dismissOverlays();
  });

  it('should display relation management page', () => {
    cy.url().should('include', '/admin/relations');
    cy.get('.page-header h2').should('contain', 'Relations Fournisseur-Boutique');
  });

  it('should show relation table', () => {
    cy.get('table tbody tr').should('have.length.gte', 2);
    cy.get('table thead').should('contain', 'Fournisseur');
    cy.get('table thead').should('contain', 'Boutique');
  });

  it('should show relations as ACTIVE', () => {
    cy.get('.status-badge.active').should('have.length.gte', 1);
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
