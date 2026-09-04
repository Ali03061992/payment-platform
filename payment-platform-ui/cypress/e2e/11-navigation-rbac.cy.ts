describe('11 - Navigation: Sidebar', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
  });

  it('should display sidebar', () => {
    cy.get('aside.sidebar').should('exist');
    cy.get('.sidebar-header .logo').should('exist');
  });

  it('should have Dashboard link', () => {
    cy.get('.sidebar-nav').should('contain.text', 'Tableau de bord');
  });

  it('should have Payments section', () => {
    cy.get('.sidebar-nav').should('contain.text', 'Paiements');
  });

  it('should navigate to Payments via sidebar', () => {
    cy.get('a[href*="payments"], [routerLink*="payments"]').first().click();
    cy.url({ timeout: 5000 }).should('include', '/payments');
  });

  it('should have all admin nav items', () => {
    cy.get('.sidebar-nav .nav-item').should('have.length.gte', 8);
  });

  it('should highlight active nav item', () => {
    cy.get('.sidebar-nav .nav-item.active').should('exist');
  });

  it('should show user info in sidebar footer', () => {
    cy.get('.sidebar-footer .user-info').should('exist');
    cy.get('.sidebar-footer .user-name').should('not.be.empty');
    cy.get('.sidebar-footer .user-role').should('contain', 'SYSTEM_ADMIN');
  });

  it('should have logout button in sidebar', () => {
    cy.get('.sidebar-footer .logout-btn').should('exist');
    cy.get('.sidebar-footer .logout-btn').should('contain', 'Déconnexion');
  });
});

describe('11 - Navigation: Guards', () => {
  it('should redirect unauthenticated user to login', () => {
    cy.visit('/dashboard');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('should redirect from root to login', () => {
    cy.visit('/');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });

  it('should redirect unknown routes to login', () => {
    cy.visit('/unknown-route-xyz');
    cy.url({ timeout: 5000 }).should('include', '/login');
  });
});

describe('11 - Navigation: Role-based Access Control', () => {
  before(() => cy.ensureTestUsers());

  describe('SYSTEM_ADMIN', () => {
    it('should access admin pages', () => {
      cy.loginAsAdmin();
      cy.visit('/dashboard/admin/users');
      cy.url().should('include', '/admin/users');
    });

    it('should access supplier management', () => {
      cy.loginAsAdmin();
      cy.visit('/dashboard/admin/suppliers');
      cy.url().should('include', '/admin/suppliers');
    });

    it('should access shop management', () => {
      cy.loginAsAdmin();
      cy.visit('/dashboard/admin/shops');
      cy.url().should('include', '/admin/shops');
    });

    it('should access relation management', () => {
      cy.loginAsAdmin();
      cy.visit('/dashboard/admin/relations');
      cy.url().should('include', '/admin/relations');
    });

    it('should access payments', () => {
      cy.loginAsAdmin();
      cy.visit('/dashboard/payments');
      cy.url().should('include', '/payments');
    });

    it('should access payment stats', () => {
      cy.loginAsAdmin();
      cy.visit('/dashboard/payments/stats');
      cy.url().should('include', '/payments/stats');
    });
  });

  describe('SUPPLIER_ADMIN', () => {
    it('should access supplier products', () => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/supplier/products');
      cy.url({ timeout: 5000 }).should('include', '/supplier/products');
    });

    it('should access supplier stock', () => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/supplier/stock');
      cy.url({ timeout: 5000 }).should('include', '/supplier/stock');
    });

    it('should access supplier orders', () => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/supplier/orders');
      cy.url({ timeout: 5000 }).should('include', '/supplier/orders');
    });

    it('should access agent payments', () => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/supplier/agent-payments');
      cy.url({ timeout: 5000 }).should('include', '/supplier/agent-payments');
    });
  });

  describe('SHOP_ADMIN', () => {
    it('should access shop orders', () => {
      cy.loginAsShopAdmin();
      cy.visit('/dashboard/shop/orders');
      cy.url({ timeout: 5000 }).should('include', '/shop/orders');
    });

    it('should access shop balance', () => {
      cy.loginAsShopAdmin();
      cy.visit('/dashboard/shop/balance');
      cy.url({ timeout: 5000 }).should('include', '/shop/balance');
    });

    it('should access payments', () => {
      cy.loginAsShopAdmin();
      cy.visit('/dashboard/payments');
      cy.url({ timeout: 5000 }).should('include', '/payments');
    });
  });

  describe('Cross-role restrictions', () => {
    it('supplier admin should not access admin pages', () => {
      cy.loginAsSupplierAdmin();
      cy.visit('/dashboard/admin/users');
      cy.url({ timeout: 5000 }).should('satisfy', (url: string) =>
        !url.includes('/admin/users')
      );
    });

    it('shop admin should not access admin pages', () => {
      cy.loginAsShopAdmin();
      cy.visit('/dashboard/admin/users');
      cy.url({ timeout: 5000 }).should('satisfy', (url: string) =>
        !url.includes('/admin/users')
      );
    });

    it('shop admin should not access supplier pages', () => {
      cy.loginAsShopAdmin();
      cy.visit('/dashboard/supplier/products');
      cy.url({ timeout: 5000 }).should('satisfy', (url: string) =>
        !url.includes('/supplier/products')
      );
    });
  });
});

describe('11 - Navigation: Supplier Sidebar Items', () => {
  before(() => cy.ensureTestUsers());

  it('should show supplier-specific nav items', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard');
    cy.get('.sidebar-nav').should('contain.text', 'Catalogue');
    cy.get('.sidebar-nav').should('contain.text', 'Stock');
    cy.get('.sidebar-nav').should('contain.text', 'Commandes');
  });
});

describe('11 - Navigation: Shop Sidebar Items', () => {
  before(() => cy.ensureTestUsers());

  it('should show shop-specific nav items', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard');
    cy.get('.sidebar-nav').should('contain.text', 'Mes commandes');
    cy.get('.sidebar-nav').should('contain.text', 'Balance');
  });
});
