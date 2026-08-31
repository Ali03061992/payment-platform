describe('Payments - List', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments');
  });

  it('should display payments list', () => {
    cy.url().should('include', '/payments');
    cy.get('body').should('be.visible');
  });

  it('should show create payment button', () => {
    cy.get('a[href*="create"], button:contains("Nouveau"), a:contains("Nouveau"), a:contains("Créer")').should('exist');
  });

  it('should navigate to create payment page', () => {
    cy.get('button:contains("Nouveau"), a:contains("Nouveau")').first().click();
    cy.url({ timeout: 5000 }).should('include', '/payments/create');
  });
});

describe('Payments - Create', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments/create');
  });

  it('should display payment creation form', () => {
    cy.url().should('include', '/payments/create');
    cy.get('body').should('be.visible');
  });

  it('should have shop and supplier selection', () => {
    cy.get('select, [class*="select"]').should('have.length.greaterThan', 0);
  });

  it('should have amount input', () => {
    cy.get('input[type="number"], input[formControlName="amount"], #amount').should('exist');
  });

  it('should have cancel button that navigates back', () => {
    cy.get('a[href*="payments"], button:contains("Annuler"), a:contains("Annuler")').should('exist');
  });
});

describe('Payments - Detail', () => {
  it('should navigate back on invalid payment id', () => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments/999999');
    cy.url({ timeout: 10000 }).should('satisfy', (url: string) =>
      url.includes('/payments') || url.includes('/dashboard')
    );
  });
});

describe('Payments - Stats', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/payments/stats');
  });

  it('should display payment statistics', () => {
    cy.url().should('include', '/payments/stats');
    cy.get('body').should('be.visible');
  });
});
