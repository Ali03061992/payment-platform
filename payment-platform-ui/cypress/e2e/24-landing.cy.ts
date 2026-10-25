// Parcours 24-landing.cy.ts : page publique NEXTERA (presentation, contact, acces).
// Prerequis : UI live. Aucun backend n'est necessaire pour afficher la landing page.
describe('24 - Landing page', () => {
  beforeEach(() => {
    cy.viewport(1280, 800);
    cy.visit('/');
    cy.get('[data-testid="landing-page"]').should('exist');
  });

  it('should expose the public presentation content', () => {
    cy.contains('Pilotez vos échanges fournisseurs-boutiques').should('be.visible');
    cy.contains('Une plateforme, tout votre cycle commercial').should('exist');
    cy.contains('Démarrer en trois étapes').should('exist');
    cy.contains('Parlons de votre projet').should('exist');
  });

  it('should not expose public self-registration', () => {
    cy.get('a[href="/register"]').should('not.exist');
    cy.contains('Créer un compte').should('not.exist');
    cy.get('a[href="#contact"]').should('have.length.greaterThan', 0);
  });

  it('should provide contact entry points', () => {
    cy.get('a[href^="mailto:"]').should('have.length', 1);
    cy.get('a[href^="tel:"]').should('have.length', 1);
    cy.get('a[href^="https://wa.me/"]').should('have.length', 1);
  });

  it('should navigate to the login page', () => {
    cy.get('a[href="/login"]').first().click();
    cy.url({ timeout: 5000 }).should('include', '/login');
    cy.get('#username').should('be.visible');
  });

  it('should stay responsive on mobile without horizontal overflow', () => {
    cy.viewport(390, 720);
    cy.get('[data-testid="landing-page"]').should('exist');
    cy.document().then((doc) => {
      expect(doc.documentElement.scrollWidth).to.be.at.most(doc.documentElement.clientWidth + 1);
    });
  });
});
