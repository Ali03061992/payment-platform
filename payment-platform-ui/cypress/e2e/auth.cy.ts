describe('Authentication', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  describe('Login Page', () => {
    it('should display login form', () => {
      cy.url().should('include', '/login');
      cy.get('input[formControlName="username"], input[name="username"], #username').should('exist');
      cy.get('input[formControlName="password"], input[name="password"], #password').should('exist');
      cy.get('button[type="submit"], button.btn-primary').should('exist');
    });

    it('should show error on invalid credentials', () => {
      cy.get('input[formControlName="username"], input[name="username"], #username').clear().type('wrong.user');
      cy.get('input[formControlName="password"], input[name="password"], #password').clear().type('wrongpass');
      cy.get('button[type="submit"], button.btn-primary').click();
      cy.get('.error, .alert-danger, [class*="error"], mat-error, .message-error').should('be.visible');
    });

    it('should login as SYSTEM_ADMIN and redirect to dashboard', () => {
      cy.get('input[formControlName="username"], input[name="username"], #username').clear().type('system.admin');
      cy.get('input[formControlName="password"], input[name="password"], #password').clear().type('Admin@123');
      cy.get('button[type="submit"], button.btn-primary').click();
      cy.url({ timeout: 15000 }).should('include', '/dashboard');
      cy.window().then((win) => {
        const user = JSON.parse(win.localStorage.getItem('user') || '{}');
        expect(user.roles).to.include('SYSTEM_ADMIN');
      });
    });

    it('should show register link', () => {
      cy.get('a[href="/register"], a[routerLink="/register"]').should('exist');
    });

    it('should navigate to register page', () => {
      cy.get('a[href="/register"], a[routerLink="/register"]').click();
      cy.url().should('include', '/register');
    });
  });

  describe('Register Page', () => {
    beforeEach(() => {
      cy.visit('/register');
    });

    it('should display registration form', () => {
      cy.get('input[formControlName="username"], input[name="username"], #username').should('exist');
      cy.get('input[formControlName="email"], input[name="email"], #email').should('exist');
      cy.get('input[formControlName="password"], input[name="password"], #password').should('exist');
      cy.get('input[formControlName="firstName"], input[name="firstName"], #firstName').should('exist');
      cy.get('input[formControlName="lastName"], input[name="lastName"], #lastName').should('exist');
      cy.get('select[formControlName="role"], select[name="role"], #role').should('exist');
    });

    it('should show login link', () => {
      cy.get('a[href="/login"], a[routerLink="/login"]').should('exist');
    });

    it('should navigate back to login', () => {
      cy.get('a[href="/login"], a[routerLink="/login"]').first().click();
      cy.url().should('include', '/login');
    });
  });

  describe('Password Setup Page', () => {
    it('should display password setup page', () => {
      cy.visit('/setup-password');
      cy.get('.password-setup-container, .password-setup-card').should('exist');
      cy.get('body').should('contain.text', 'Payment Platform');
    });
  });

  describe('Logout', () => {
    it('should logout and redirect to login', () => {
      cy.login('system.admin', 'Admin@123');
      cy.visit('/dashboard');
      cy.get('.sidebar .logout-btn').click();
      cy.url({ timeout: 5000 }).should('include', '/login');
      cy.window().then((win) => {
        expect(win.localStorage.getItem('token')).to.be.null;
      });
    });
  });
});
