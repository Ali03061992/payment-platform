describe('02 - Admin Dashboard', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
  });

  it('should display dashboard with welcome message', () => {
    cy.url().should('include', '/dashboard');
    cy.get('.dashboard').should('exist');
    cy.get('.welcome-section h1').should('exist');
  });

  it('should show 5 stat cards for SYSTEM_ADMIN', () => {
    cy.get('.stats-grid', { timeout: 20000 }).should('exist');
    cy.get('.stat-card', { timeout: 20000 }).should('have.length', 5);
    cy.get('.stat-card').eq(0).should('contain', 'Total utilisateurs');
    cy.get('.stat-card').eq(1).should('contain', 'Comptes actifs');
    cy.get('.stat-card').eq(2).should('contain', 'Comptes désactivés');
    cy.get('.stat-card').eq(3).should('contain', 'Fournisseurs');
    cy.get('.stat-card').eq(4).should('contain', 'Boutiques');
  });

  it('should display profile info card', () => {
    cy.get('.info-card').first().within(() => {
      cy.get('h3').should('contain', 'Mon profil');
      cy.get('.info-row').should('have.length.gte', 3);
    });
  });

  it('should show quick actions for admin', () => {
    cy.get('.info-card').last().within(() => {
      cy.get('h3').should('contain', 'Actions rapides');
      cy.get('a.quick-action').should('have.length.gte', 2);
    });
  });

  it('should display sidebar with admin nav items', () => {
    cy.get('aside.sidebar').should('exist');
    cy.get('a.nav-item').should('have.length.gte', 5);
  });

  it('should display top bar with user name and notifications', () => {
    cy.get('.top-bar').should('exist');
    cy.get('.top-bar h2').should('contain', 'Bienvenue');
    cy.get('.notif-bell').should('exist');
    cy.get('.status-badge.active').should('contain', 'En ligne');
  });

  it('should show user info in sidebar footer', () => {
    cy.get('.toggle-btn').click();
    cy.get('.sidebar-footer .user-info').should('exist');
    cy.get('.sidebar-footer .user-name').should('not.be.empty');
    cy.get('.sidebar-footer .user-role').should('contain', 'SYSTEM_ADMIN');
  });
});
