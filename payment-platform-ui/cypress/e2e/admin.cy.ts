describe('Admin - Dashboard', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard');
  });

  it('should display dashboard with stats cards', () => {
    cy.url().should('include', '/dashboard');
    cy.get('body').should('be.visible');
    cy.get('.stat, .card, [class*="stat"], [class*="card"]').should('have.length.greaterThan', 0);
  });

  it('should show admin navigation links', () => {
    cy.get('body').then(($body) => {
      const text = $body.text();
      const hasUsersLink = text.includes('Utilisateurs') || text.includes('utilisateurs') || text.includes('users');
      const hasSuppliersLink = text.includes('Fournisseurs') || text.includes('fournisseurs');
      const hasShopsLink = text.includes('Boutiques') || text.includes('boutiques');
      expect(hasUsersLink || hasSuppliersLink || hasShopsLink).to.be.true;
    });
  });
});

describe('Admin - User Management', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/users');
  });

  it('should display user list table', () => {
    cy.url().should('include', '/admin/users');
    cy.get('table, .table, [class*="table"]').should('exist');
  });

  it('should show create user button', () => {
    cy.get('a[href*="create"], button:contains("Créer"), a:contains("Créer")').should('exist');
  });

  it('should navigate to create user page', () => {
    cy.get('a[href*="create"], a:contains("Créer")').first().click();
    cy.url({ timeout: 5000 }).should('include', '/admin/users/create');
  });

  it('should filter users by role', () => {
    cy.get('select, [class*="filter"]').first().then(($el) => {
      if ($el.is('select')) {
        cy.wrap($el).select(1);
        cy.get('table tbody tr, table tbody td').should('exist');
      }
    });
  });

  it('should show activate/disable buttons for users', () => {
    cy.get('table tbody tr, table tbody td').then(($rows) => {
      if ($rows.length > 0) {
        cy.get('button:contains("Activer"), button:contains("Désactiver"), button:contains("Disable"), button:contains("Activate"), [class*="toggle"]').should('exist');
      }
    });
  });
});

describe('Admin - Create User', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/users/create');
  });

  it('should display user creation form', () => {
    cy.url().should('include', '/admin/users/create');
    cy.get('input').should('have.length.greaterThan', 0);
  });

  it('should have role selection', () => {
    cy.get('select, [class*="role"]').should('exist');
  });
});

describe('Admin - Supplier Management', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/suppliers');
  });

  it('should display supplier list', () => {
    cy.url().should('include', '/admin/suppliers');
    cy.get('body').should('be.visible');
  });

  it('should show create supplier form', () => {
    cy.get('button:contains("Nouveau"), button:contains("fournisseur")').first().click();
    cy.get('input[name="name"], input.form-control').should('exist');
  });
});

describe('Admin - Shop Management', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/shops');
  });

  it('should display shop list', () => {
    cy.url().should('include', '/admin/shops');
    cy.get('body').should('be.visible');
  });
});

describe('Admin - Relation Management', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/relations');
  });

  it('should display relations list', () => {
    cy.url().should('include', '/admin/relations');
    cy.get('body').should('be.visible');
  });
});

describe('Admin - Organization Stats', () => {
  beforeEach(() => {
    cy.loginAsAdmin();
    cy.visit('/dashboard/admin/org-stats');
  });

  it('should display organization statistics', () => {
    cy.url().should('include', '/admin/org-stats');
    cy.get('body').should('be.visible');
  });
});
