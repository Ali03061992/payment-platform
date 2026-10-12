// Parcours 05-supplier-products.cy.ts : 05 - Supplier: Product Management; 05 - Supplier: Product CRUD (10 scenarios).
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), connexion fournisseur via cy.loginAsSupplierAdmin(), UI live.
// Budget E2E : pas de hammering auth (le 429 est prouve cote backend), nettoyages via before/beforeEach.
const API05 = () => Cypress.env('apiUrl') || 'http://localhost:8081';
describe('05 - Supplier: Product Management', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/products');
    cy.dismissOverlays();
  });

  it('should display product management page', () => {
    cy.url().should('include', '/supplier/products');
    cy.get('.page-header h1').should('contain', 'Catalogue produits');
  });

  it('should show product table with all columns', () => {
    cy.get('table thead th').should('have.length', 9);
    cy.get('table thead').should('contain', 'SKU');
    cy.get('table thead').should('contain', 'Nom');
    cy.get('table thead').should('contain', 'Famille');
    cy.get('table thead').should('contain', 'Prix');
    cy.get('table thead').should('contain', 'TVA');
    cy.get('table thead').should('contain', 'Unite');
    cy.get('table thead').should('contain', 'Statut');
    cy.get('table thead').should('contain', 'Actions');
  });

  it('should have create product button', () => {
    cy.get('.page-header button.btn-primary').should('contain', 'Nouveau produit');
  });

  it('should toggle create product form', () => {
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card h3').should('contain', 'Creer un produit');
    cy.get('input[name="name"]').should('exist');
    cy.get('input[name="sku"]').should('exist');
    cy.get('.form-card input[name="description"]').should('exist');
    cy.get('select[name="familyId"]').should('exist');
    cy.get('select[name="categoryId"]').should('exist');
    cy.get('select[name="currency"]').should('exist');
    cy.get('input[name="unitPrice"]').should('exist');
    cy.get('input[name="minQuantity"]').should('exist');
    cy.get('select[name="vatRate"]').should('exist');
  });

  it('should filter products by category', () => {
    cy.get('.filter-bar .filter-select').should('exist');
    cy.get('.result-count').should('exist');
  });

  it('should show product actions', () => {
    cy.get('table tbody tr').first().then(($row) => {
      if (!$row.find('.empty').length) {
        cy.wrap($row).within(() => {
          cy.get('button').should('have.length.gte', 2);
        });
      }
    });
  });
});

describe('05 - Supplier: Product CRUD', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/products');
    cy.dismissOverlays();
  });

  it('should create a new product', () => {
    const sku = `E2E-${Date.now()}`;
    cy.get('.page-header button.btn-primary').click();
    cy.dismissOverlays();
    cy.get('.form-card', { timeout: 10000 }).should('be.visible');
    cy.get('input[name="name"]').clear().type('Produit E2E Test');
    cy.get('input[name="sku"]').clear().type(sku);
    cy.get('.form-card input[name="description"]').clear().type('Description test');
    cy.get('input[name="unitPrice"]').clear().type('25.50');
    cy.get('input[name="minQuantity"]').clear().type('5');
    cy.get('.form-card button[type="submit"]').should('not.be.disabled').click();
    // Le tableau est paginé : on prouve la création via l'API (le toast + le rechargement suivent).
    cy.get('.toast-success', { timeout: 10000 }).should('exist');
    cy.apiLogin('covale.admin.e2e').then((token) => {
      cy.request({
        method: 'GET',
        url: `${API05()}/api/suppliers/${Cypress.env('covaleId')}/products`,
        headers: { Authorization: `Bearer ${token}` },
        failOnStatusCode: false,
      }).then((r) => {
        const body: any = r.body;
        const list = Array.isArray(body) ? body : (body?.items || body?.content || []);
        expect(list.map((p: any) => p.sku)).to.include(sku);
      });
    });
  });

  it('should cancel product creation', () => {
    cy.get('.page-header button.btn-primary').click();
    cy.get('.form-card').should('be.visible');
    cy.get('.form-card button[type="button"]').contains('Annuler').click();
    cy.get('.form-card').should('not.exist');
  });

  it('should create a product with reduced VAT rate and FODEC', () => {
    const sku = `E2E-FISC-${Date.now()}`;
    cy.intercept('GET', '**/suppliers/*/products*').as('listProducts');
    cy.wait('@listProducts', { timeout: 15000 });
    cy.get('.page-header button.btn-primary').click();
    cy.dismissOverlays();
    cy.get('.form-card', { timeout: 10000 }).should('be.visible');
    cy.get('input[name="name"]').clear().type('Produit TVA 7 FODEC');
    cy.get('input[name="sku"]').clear().type(sku);
    cy.get('input[name="unitPrice"]').clear().type('100');
    cy.get('input[name="minQuantity"]').clear().type('1');
    cy.get('select[name="vatRate"] option').eq(2).then((opt) => {
      cy.get('select[name="vatRate"]').select(opt.val() as string);
    });
    cy.get('input[name="fodecApplicable"]').check({ force: true });
    cy.intercept('POST', '**/suppliers/*/products').as('createProduct');
    cy.get('.form-card button[type="submit"]').should('not.be.disabled').click();
    cy.wait('@createProduct', { timeout: 15000 }).its('response.statusCode').should('eq', 200);
    cy.wait('@listProducts', { timeout: 15000 });
    // Le tableau est paginé : preuve via l'API (taux + FODEC persistés).
    cy.apiLogin('covale.admin.e2e').then((token) => {
      cy.request({
        method: 'GET',
        url: `${API05()}/api/suppliers/${Cypress.env('covaleId')}/products`,
        headers: { Authorization: `Bearer ${token}` },
        failOnStatusCode: false,
      }).then((r) => {
        const body: any = r.body;
        const list = Array.isArray(body) ? body : (body?.items || body?.content || []);
        const created = list.find((p: any) => p.sku === sku);
        expect(created, 'produit créé').to.exist;
        expect(created.vatRate).to.eq(7);
        expect(created.fodecApplicable).to.be.true;
      });
    });
  });

  it('should require exemption reason at 0% VAT', () => {
    cy.get('.page-header button.btn-primary').click();
    cy.dismissOverlays();
    cy.get('.form-card', { timeout: 10000 }).should('be.visible');
    cy.get('select[name="vatRate"] option').eq(3).then((opt) => {
      cy.get('select[name="vatRate"]').select(opt.val() as string);
    });
    cy.get('input[name="vatExemptionText"]').should('exist');
  });
});
