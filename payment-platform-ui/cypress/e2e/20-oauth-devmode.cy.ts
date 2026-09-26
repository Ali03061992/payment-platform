// Parcours 20-oauth-devmode.cy.ts : dev-login sans password + panneau dev /login.
// TOUTE la spec est conditionnee au mode dev (CLI : --env isDev=true) via
// (isDev ? describe : describe.skip) : en mode non-dev, 0 test, suite verte
// garantie. En mode dev, le backend expose POST /api/auth/dev-login
// {username} (flag + profil non-prod, 404 si username inconnu) et le front
// affiche le panneau dev sur /login si window.__env.IS_DEV === 'true'.
// Prerequis mode dev : stack live profil local/dev + IS_DEV=true (front et
// backend) + utilisateurs seedes via cy.ensureTestUsers() (inchange, password).
// CONTRAT FRONT (agent parallele) : le panneau dev porte
// [data-testid="dev-login-panel"] sur /login quand IS_DEV === 'true'.
// Budget E2E : 2 appels auth seulement, pas de hammering.
const isDev = Cypress.env('isDev') === true || String(Cypress.env('isDev')).toLowerCase() === 'true';

(isDev ? describe : describe.skip)('20 - OAuth dev-mode: dev-login sans password', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  before(() => cy.ensureTestUsers());

  it('dev-login delivre un token valide (/me 200)', () => {
    cy.getTestCtx().then((ctx) => {
      expect(ctx.users?.shopAli?.username, 'shopAli username').to.be.a('string').and.not.be.empty;
      // apiLogin est en mode dual : ici branche dev-login {username} sans password.
      cy.apiLogin(ctx.users.shopAli.username).then((token) => {
        expect(token, 'accessToken').to.be.a('string').and.not.be.empty;
        cy.request({
          method: 'GET',
          url: `${API()}/api/auth/me`,
          headers: { Authorization: `Bearer ${token}` },
        }).then((me) => {
          expect(me.status).to.eq(200);
          expect(me.body.username).to.eq(ctx.users.shopAli.username);
        });
      });
    });
  });

  it('dev-login username inconnu -> 404', () => {
    cy.request({
      method: 'POST',
      url: `${API()}/api/auth/dev-login`,
      body: { username: `unknown_dev_${Date.now()}` },
      failOnStatusCode: false,
    }).then((r) => {
      expect(r.status).to.eq(404);
    });
  });

  it('panneau dev visible sur /login quand IS_DEV', () => {
    cy.visit('/login');
    cy.get('[data-testid="dev-login-panel"]', { timeout: 10000 }).should('be.visible');
  });
});
