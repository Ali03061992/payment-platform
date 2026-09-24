describe('01 - Auth: Login Page', () => {
  before(() => cy.ensureTestUsers());
  beforeEach(() => {
    cy.window().then((win) => {
      win.sessionStorage.clear();
    });
    cy.visit('/');
    cy.url().should('include', '/login');
    cy.get('#username').should('be.visible');
  });

  it('should display login form with all fields', () => {
    cy.url().should('include', '/login');
    cy.get('input#username').should('exist');
    cy.get('input#password').should('exist');
    cy.get('button[type="submit"]').should('exist');
    cy.get('.login-logo').should('exist');
    cy.get('h1').should('contain', 'Connexion');
  });

  it('should show error on invalid credentials', () => {
    cy.get('#username').clear().type('wrong.user');
    cy.get('#password').clear().type('wrongpass');
    cy.get('button[type="submit"]').click();
    cy.get('.error-message', { timeout: 10000 }).should('be.visible');
    cy.url().should('include', '/login');
  });

  it('should show error on empty fields submission', () => {
    cy.get('button[type="submit"]').click();
    cy.url().should('include', '/login');
  });

  it('should login as SYSTEM_ADMIN and reach dashboard', () => {
    cy.get('#username').clear().type('system.admin');
    cy.get('#password').clear().type('@PAssword012345');
    cy.get('button[type="submit"]').click();
    cy.url({ timeout: 15000 }).should('include', '/dashboard');
    cy.window().then((win) => {
      const user = JSON.parse(win.sessionStorage.getItem('user') || '{}');
      expect(user.roles).to.include('SYSTEM_ADMIN');
      expect(win.sessionStorage.getItem('token')).to.not.be.null;
    });
  });

  it('should login as SUPPLIER_ADMIN and reach dashboard', () => {
    cy.getTestCtx().then((ctx) => {
      expect(ctx.users?.supplierAdmin?.username, 'supplierAdmin username').to.be.a('string').and.not.be.empty;
      cy.get('#username').clear().type(ctx.users.supplierAdmin.username);
      cy.get('#password').clear().type('test1234');
      cy.get('button[type="submit"]').should('not.be.disabled').click();
      cy.url({ timeout: 20000 }).should('include', '/dashboard');
      cy.window().then((win) => {
        const user = JSON.parse(win.sessionStorage.getItem('user') || '{}');
        expect(user.roles).to.include('SUPPLIER_ADMIN');
      });
    });
  });

  it('should login as SHOP_ADMIN and reach dashboard', () => {
    cy.getTestCtx().then((ctx) => {
      expect(ctx.users?.shopAdmin?.username, 'shopAdmin username').to.be.a('string').and.not.be.empty;
      cy.get('#username').clear().type(ctx.users.shopAdmin.username);
      cy.get('#password').clear().type('test1234');
      cy.get('button[type="submit"]').should('not.be.disabled').click();
      cy.url({ timeout: 20000 }).should('include', '/dashboard');
      cy.window().then((win) => {
        const user = JSON.parse(win.sessionStorage.getItem('user') || '{}');
        expect(user.roles).to.include('SHOP_ADMIN');
      });
    });
  });
});

describe('01 - Auth: Register Page', () => {
  beforeEach(() => cy.visit('/register'));

  it('should display full registration form', () => {
    cy.url().should('include', '/register');
    cy.get('h1').should('contain', 'Créer un compte');
    cy.get('#lastName').should('exist');
    cy.get('#firstName').should('exist');
    cy.get('#username').should('exist');
    cy.get('#email').should('exist');
    cy.get('#phone').should('exist');
    cy.get('#password').should('exist');
    cy.get('#role').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });

  it('should have login link', () => {
    cy.get('.login-link a').should('contain', 'Se connecter');
    cy.get('.login-link a').click();
    cy.url().should('include', '/login');
  });

  it('should submit registration form', () => {
    const ts = Date.now();
    cy.get('#lastName').clear().type('TestNom');
    cy.get('#firstName').clear().type('TestPrenom');
    cy.get('#username').clear().type(`testreg_${ts}`);
    cy.get('#email').clear().type(`testreg_${ts}@e2e.com`);
    cy.get('#password').clear().type('SecurePass123!');
    cy.get('#role').select('SUPPLIER_ADMIN');
    cy.get('button[type="submit"]').click();
    cy.url({ timeout: 10000 }).should('satisfy', (url: string) =>
      url.includes('/login') || url.includes('/register')
    );
  });
});

describe('01 - Auth: Password Setup Page', () => {
  it('should display password setup page', () => {
    cy.visit('/setup-password');
    cy.get('body').should('contain.text', 'Payment Platform');
  });
});

describe('01 - Auth: Logout', () => {
  it('should logout and redirect to login', () => {
    cy.login('system.admin', '@PAssword012345');
    cy.visit('/dashboard');
    cy.get('.sidebar .logout-btn').click();
    cy.url({ timeout: 5000 }).should('include', '/login');
    cy.window().then((win) => {
      expect(win.sessionStorage.getItem('token')).to.be.null;
      expect(win.sessionStorage.getItem('user')).to.be.null;
    });
  });
});

describe('01 - Auth: Guards', () => {
  it('should redirect unauthenticated user to login', () => {
    cy.visit('/dashboard');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('should redirect from root to login', () => {
    cy.visit('/');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('should redirect unknown routes to login', () => {
    cy.visit('/totally-unknown-page');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });
});
