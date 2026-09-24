// B3 : deny-by-default au gateway (plus de "/api/**".permitAll()).
// Ces assertions distinguent le rejet GATEWAY du rejet downstream :
// - le 401 gateway porte un body JSON {status, error: 'UNAUTHORIZED',
//   message: "Token d'authentification manquant", path} (JwtValidationFilter,
//   qui alimente aussi le SecurityContext pour la chaîne deny-by-default) ;
// - pré-B3, la route internal/ passait le filtre et le contrôleur org répondait
//   401 avec un body VIDE.
// Donc un body JSON non vide prouve que le gateway verrouille avant proxy.
describe('16 - B3: Gateway deny-by-default', () => {
  const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

  it('GET /api/payments without JWT returns gateway 401 JSON', () => {
    cy.request({
      method: 'GET',
      url: `${API()}/api/payments`,
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status, 'status').to.eq(401);
      expect(resp.body?.error, 'error').to.eq('UNAUTHORIZED');
      expect(resp.body?.message, 'gateway message').to.eq("Token d'authentification manquant");
    });
  });

  it('internal route without JWT is rejected by the gateway (not proxied)', () => {
    cy.request({
      method: 'GET',
      url: `${API()}/api/organizations/internal/00000000-0000-0000-0000-000000000001/status`,
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status, 'status').to.eq(401);
      // Body JSON gateway (vs body vide du contrôleur org pré-B3) : le gateway
      // exige désormais un JWT avant même le X-Internal-Token.
      expect(resp.body?.error, 'error').to.eq('UNAUTHORIZED');
      expect(resp.body?.message, 'gateway message').to.eq("Token d'authentification manquant");
    });
  });

  it('actuator health stays public', () => {
    cy.request({
      method: 'GET',
      url: `${API()}/actuator/health`,
      failOnStatusCode: false,
    }).then((resp) => {
      expect(resp.status, 'status').to.eq(200);
    });
  });
});
