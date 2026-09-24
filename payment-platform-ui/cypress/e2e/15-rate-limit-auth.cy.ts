// B2 : les endpoints d'auth passent par le rate-limit strict du gateway.
// - Le 429 est prouvé par le test unitaire backend RateLimitFilterTest (déterministe).
// - Cette spec prouve le câblage en environnement déployé : les réponses login/register
//   portent les headers X-RateLimit-* (donc le filtre est actif devant l'auth).
// - Pas de hammering ici : la suite fait ~70 logins depuis une seule IP et la CI
//   assouplit le budget auth (RATE_LIMIT_AUTH_PER_MINUTE=1000) ; un hammer rendrait
//   la suite flaky. Vérification manuelle du 429 : boucler 11x
//   `curl -X POST $GW/api/auth/login -d '{"username":"x","password":"y"}'`
//   avec le défaut prod (10/min) -> le 11e répond 429 + Retry-After: 60.
describe('15 - B2: Auth rate limiting', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  it('wrong password returns 401 (not 429) and carries rate-limit headers', () => {
    cy.request({
      method: 'POST',
      url: `${API()}/api/auth/login`,
      body: { username: 'wrong.user', password: 'wrongpass' },
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status, 'wrong creds status').to.be.oneOf([400, 401]);
      expect(resp.headers, 'X-RateLimit-Limit header').to.have.property('x-ratelimit-limit');
      expect(resp.headers, 'X-RateLimit-Remaining header').to.have.property('x-ratelimit-remaining');
    });
  });

  it('invalid register payload is not throttled and carries rate-limit headers', () => {
    cy.request({
      method: 'POST',
      url: `${API()}/api/auth/register`,
      body: { username: 'x' },
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status, 'invalid register must not be throttled').to.not.eq(429);
      expect(resp.headers, 'X-RateLimit-Limit header').to.have.property('x-ratelimit-limit');
    });
  });
});
