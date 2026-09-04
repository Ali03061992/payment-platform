describe('03 - Admin: User Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/users');
  });

  it('should display user management page', () => {
    cy.url().should('include', '/admin/users');
    cy.get('.page-header h1').should('contain', 'Gestion des utilisateurs');
  });

  it('should show user table with columns', () => {
    cy.get('table thead th').should('have.length', 8);
    cy.get('table thead').should('contain', 'ID');
    cy.get('table thead').should('contain', "Nom d'utilisateur");
    cy.get('table thead').should('contain', 'Nom complet');
    cy.get('table thead').should('contain', 'Email');
    cy.get('table thead').should('contain', 'Rôle');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should display users in table', () => {
    cy.get('table tbody tr').should('have.length.gte', 1);
    cy.get('table tbody tr').first().within(() => {
      cy.get('td').should('have.length', 8);
    });
  });

  it('should have create user button', () => {
    cy.get('.page-header a.btn-primary').should('contain', 'Créer un compte');
    cy.get('.page-header a.btn-primary').should('have.attr', 'routerLink', 'create');
  });

  it('should filter users by role', () => {
    cy.get('.filters select').first().select('SYSTEM_ADMIN');
    cy.get('table tbody tr').each(($row) => {
      cy.wrap($row).find('.role-badge').should('contain', 'SYSTEM_ADMIN');
    });
  });

  it('should filter users by status', () => {
    cy.get('.filters select').last().select('ACTIVE');
    cy.get('table tbody tr').each(($row) => {
      cy.wrap($row).find('.status-badge').should('have.class', 'active');
    });
  });

  it('should show activate/disable buttons based on status', () => {
    cy.get('table tbody tr').first().within(() => {
      cy.get('.status-badge').then(($badge) => {
        if ($badge.hasClass('active')) {
          cy.get('button.btn-danger').should('contain', 'Désactiver');
        } else {
          cy.get('button.btn-success').should('contain', 'Activer');
        }
      });
    });
  });

  it('should display role badges', () => {
    cy.get('.role-badge').should('have.length.gte', 1);
  });

  it('should display status badges', () => {
    cy.get('.status-badge').should('have.length.gte', 1);
  });
});

describe('03 - Admin: Create User', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/users/create');
  });

  it('should display user creation form', () => {
    cy.url().should('include', '/admin/users/create');
    cy.get('input').should('have.length.gte', 4);
    cy.get('select').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });
});
