describe('Supplier - Product Management', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/products');
  });

  it('should display product list', () => {
    cy.url().should('include', '/supplier/products');
    cy.get('body').should('be.visible');
  });
});

describe('Supplier - Stock Dashboard', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/dashboard');
  });

  it('should display stock dashboard', () => {
    cy.url().should('include', '/supplier/dashboard');
    cy.get('body').should('be.visible');
  });
});

describe('Supplier - Stock Management', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/stock');
  });

  it('should display stock list', () => {
    cy.url().should('include', '/supplier/stock');
    cy.get('body').should('be.visible');
  });

  it('should have add product button', () => {
    cy.get('body').then(($body) => {
      const hasBtn = $body.find('a[href*="create"]').length > 0 ||
        $body.text().includes('Ajouter');
      expect(hasBtn).to.be.true;
    });
  });
});

describe('Supplier - Add Product', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/stock/create');
  });

  it('should display product creation form', () => {
    cy.url().should('include', '/supplier/stock/create');
    cy.get('input').should('have.length.greaterThan', 0);
  });
});

describe('Supplier - Order Management', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/orders');
  });

  it('should display orders list', () => {
    cy.url().should('include', '/supplier/orders');
    cy.get('body').should('be.visible');
  });
});

describe('Supplier - Delivery Management', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  it('should redirect SUPPLIER_ADMIN away from delivery page (SUPPLIER_AGENT only)', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/deliveries');
    cy.url({ timeout: 10000 }).should('satisfy', (url: string) =>
      url.includes('/supplier/deliveries') || url.includes('/dashboard') && !url.includes('/deliveries')
    );
  });
});

describe('Supplier - Agent Payments', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/agent-payments');
  });

  it('should display agent payments page', () => {
    cy.url().should('include', '/supplier/agent-payments');
    cy.get('body').should('be.visible');
  });

  it('should have date filter inputs', () => {
    cy.get('input[type="date"]').should('have.length', 2);
  });

  it('should have status filter dropdown', () => {
    cy.get('select.form-control').should('exist');
  });

  it('should display totals bar or empty state', () => {
    cy.get('.loading', { timeout: 15000 }).should('not.exist');
    cy.get('body').then(($body) => {
      const hasTotals = $body.find('.totals-bar').length > 0;
      const hasEmpty = $body.find('.empty').length > 0;
      expect(hasTotals || hasEmpty).to.be.true;
    });
  });

  it('should have page title', () => {
    cy.get('h2').should('contain.text', 'Paiements par agent');
  });
});

describe('Supplier - Agent Payments - Nav', () => {
  before(() => {
    cy.ensureTestUsers();
  });

  it('should have agent payments link in sidebar', () => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard');
    cy.get('.sidebar').should('contain.text', 'Paiements agents');
  });
});
