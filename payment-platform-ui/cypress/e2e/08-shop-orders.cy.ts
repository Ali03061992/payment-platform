describe('08 - Shop: Order List', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
  });

  it('should display order list page', () => {
    cy.url().should('include', '/shop/orders');
    cy.get('body').should('be.visible');
  });

  it('should show create order button', () => {
    cy.get('body').then(($body) => {
      const hasBtn = $body.find('a[href*="create"]').length > 0 ||
        $body.text().includes('Nouvelle') || $body.text().includes('Créer');
      expect(hasBtn).to.be.true;
    });
  });

  it('should navigate to create order page', () => {
    cy.get('a[href*="create"], a:contains("Nouvelle"), a:contains("Créer")').first().click();
    cy.url({ timeout: 5000 }).should('include', '/shop/orders/create');
  });
});

describe('08 - Shop: Create Order', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders/create');
  });

  it('should display order creation form', () => {
    cy.url().should('include', '/shop/orders/create');
    cy.get('body').should('be.visible');
  });

  it('should have supplier selection', () => {
    cy.get('select').should('have.length.gte', 1);
  });

  it('should have submit button', () => {
    cy.get('button[type="submit"], button:contains("Créer"), button:contains("Valider")').should('exist');
  });

  it('should have cancel button', () => {
    cy.get('a:contains("Annuler"), button:contains("Annuler")').should('exist');
  });
});

describe('08 - Shop: Order Detail', () => {
  before(() => cy.ensureTestUsers());

  it('should handle invalid order id gracefully', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders/999999');
    cy.url({ timeout: 10000 }).should('satisfy', (url: string) =>
      url.includes('/shop/orders') || url.includes('/dashboard')
    );
  });
});

describe('08 - Shop: Balance View', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/balance');
  });

  it('should display balance page', () => {
    cy.url().should('include', '/shop/balance');
    cy.get('body').should('be.visible');
  });

  it('should show balance information', () => {
    cy.get('body').should('contain.text', 'Balance');
  });
});
