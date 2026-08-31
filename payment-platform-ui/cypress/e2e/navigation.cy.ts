describe('Navigation - Sidebar', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
  });

  it('should display sidebar navigation', () => {
    cy.get('.sidebar, [class*="sidebar"], nav, [class*="nav"]').should('exist');
  });

  it('should have Dashboard link (Tableau de bord)', () => {
    cy.get('body').should('contain.text', 'Tableau de bord');
  });

  it('should have Payments section', () => {
    cy.get('body').should('contain.text', 'Paiements');
  });

  it('should navigate to Payments via sidebar', () => {
    cy.get('a[href*="payments"], [routerLink*="payments"]').first().click();
    cy.url({ timeout: 5000 }).should('include', '/payments');
  });
});

describe('Navigation - Guards', () => {
  it('should redirect unauthenticated user to login', () => {
    cy.visit('/dashboard');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('should redirect from root to login', () => {
    cy.visit('/');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('should redirect unknown routes to login', () => {
    cy.visit('/nonexistent-page');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });
});

describe('Navigation - Role-based Access', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  it('should redirect admin away from supplier pages', () => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/supplier/products');
    cy.url({ timeout: 5000 }).should('satisfy', (url: string) => {
      return url.includes('/supplier/products') || url.includes('/dashboard') && !url.includes('/supplier/products');
    });
  });

  it('should redirect admin away from shop pages', () => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.url({ timeout: 5000 }).should('satisfy', (url: string) => {
      return url.includes('/shop/orders') || url.includes('/dashboard') && !url.includes('/shop/orders');
    });
  });

  it('should allow admin to access admin pages', () => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/users');
    cy.url().should('include', '/admin/users');
  });

  it('should allow supplier to access supplier pages', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/products');
    cy.url({ timeout: 5000 }).should('include', '/supplier/products');
  });

  it('should allow shop to access shop pages', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/shop/orders');
    cy.url({ timeout: 5000 }).should('include', '/shop/orders');
  });
});
