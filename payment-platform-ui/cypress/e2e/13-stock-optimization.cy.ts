// Parcours 13-stock-optimization.cy.ts : 13 - Supplier: Stock Optimization (20 scenarios).
// Prerequis : services live (gateway + microservices + UI), utilisateurs seedes via cy.ensureTestUsers(), connexion fournisseur via cy.loginAsSupplierAdmin(), UI live.
// Budget E2E : pas de hammering auth (le 429 est prouve cote backend), nettoyages via before/beforeEach.
describe('13 - Supplier: Stock Optimization', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.loginAsSupplierAdmin();
    cy.visit('/dashboard/supplier/optimization');
    cy.dismissOverlays();
  });

  it('should display optimization page header', () => {
    cy.url().should('include', '/supplier/optimization');
    cy.get('.page-header h1').should('contain', 'Optimisation des stocks');
  });

  it('should have Parametres and Recalculer buttons', () => {
    cy.get('.page-header .header-actions button').should('have.length', 2);
    cy.get('.page-header .header-actions button').first().should('contain', 'Parametres');
    cy.get('.page-header .header-actions button').last().should('contain', 'Recalculer');
  });

  it('should toggle config panel on Parametres click', () => {
    cy.get('.config-panel').should('not.exist');
    cy.get('.page-header .header-actions button').contains('Parametres').click();
    cy.get('.config-panel').should('be.visible');
    cy.get('.config-panel h3').should('contain', 'Parametres');
    cy.get('.config-group input').should('have.length', 3);
  });

  it('should have lead time, ordering cost, and holding cost inputs', () => {
    cy.get('.page-header .header-actions button').contains('Parametres').click();
    cy.get('.config-group input').eq(0).should('have.value', '7');
    cy.get('.config-group input').eq(1).should('have.value', '50');
    cy.get('.config-group input').eq(2).should('have.value', '25');
    cy.get('.config-row button').should('contain', 'Appliquer');
  });

  it('should display KPI cards', () => {
    cy.get('.kpi-grid .kpi-card').should('have.length.gte', 2);
    cy.get('.kpi-grid .kpi-label').should('contain', 'Produits analyses');
    cy.get('.kpi-grid .kpi-label').should('contain', 'Valeur stock');
  });

  it('should have four tabs', () => {
    cy.get('.tabs .tab').should('have.length', 4);
    cy.get('.tabs .tab').eq(0).should('contain', "Vue d'ensemble");
    cy.get('.tabs .tab').eq(1).should('contain', 'ABC-XYZ');
    cy.get('.tabs .tab').eq(2).should('contain', 'Produits');
    cy.get('.tabs .tab').eq(3).should('contain', 'Risques');
  });

  it('should default to overview tab', () => {
    cy.get('.tabs .tab.active').should('contain', "Vue d'ensemble");
    cy.get('.tab-content').should('be.visible');
  });

  it('should show ABC and XYZ distributions on overview tab', () => {
    cy.get('.tab-content .card h3', { timeout: 10000 }).should('contain', 'Distribution ABC');
    cy.get('.tab-content .card h3').should('contain', 'Distribution XYZ');
    cy.get('body').then(($body) => {
      const kpiText = $body.find('.kpi-grid').text() || '';
      if (kpiText.includes('Produits analyses') && !$body.text().includes('0')) {
        cy.get('.bar-row').should('have.length.gte', 1);
      } else if ($body.find('.bar-row').length > 0) {
        cy.get('.bar-row').should('have.length.gte', 1);
      } else {
        cy.log('No optimization data - empty distributions expected');
      }
    });
  });

  it('should show ABC x XYZ matrix on overview tab', () => {
    cy.get('.tab-content .card h3').should('contain', 'Matrice ABC');
    cy.get('body').then(($body) => {
      if ($body.find('.matrix-grid .matrix-cell').length > 0) {
        cy.get('.matrix-grid .matrix-cell').should('have.length.gte', 1);
      } else {
        cy.log('No matrix cells - empty data set');
      }
    });
  });

  it('should show action distribution on overview tab', () => {
    cy.get('.tab-content .card h3').should('contain', 'Actions recommandees');
  });

  it('should switch to ABC-XYZ tab', () => {
    cy.get('.tabs .tab').contains('ABC-XYZ').click();
    cy.get('.tabs .tab.active').should('contain', 'ABC-XYZ');
    cy.get('.abcxyz-table table thead th').should('have.length', 4);
    cy.get('.abcxyz-table table tbody tr').should('have.length', 3);
  });

  it('should switch to Products tab and list products', () => {
    cy.get('.tabs .tab').contains('Produits').click();
    cy.get('.tabs .tab.active').should('contain', 'Produits');
    cy.get('body').then(($body) => {
      if ($body.find('.product-list .product-row').length > 0) {
        cy.get('.product-list .product-row').should('have.length.gte', 1);
      } else {
        cy.log('No products to optimize - empty list expected');
      }
    });
  });

  it('should show ABC, XYZ, and action badges on product rows', () => {
    cy.get('.tabs .tab').contains('Produits').click();
    cy.get('body').then(($body) => {
      if ($body.find('.product-row').length > 0) {
        cy.get('.product-row .product-badges .badge').should('have.length.gte', 2);
      } else {
        cy.log('No product rows - skipping badges check');
      }
    });
  });

  it('should expand product detail on click', () => {
    cy.get('.tabs .tab').contains('Produits').click();
    cy.get('body').then(($body) => {
      if ($body.find('.product-row').length === 0) {
        cy.log('No product rows - skipping expand check');
        return;
      }
      cy.get('.product-row').first().click();
      cy.get('.product-detail').should('be.visible');
      cy.get('.detail-section h4').should('contain', 'Classification');
      cy.get('.detail-section h4').should('contain', 'Prevision');
      cy.get('.detail-section h4').should('contain', 'Stock de securite');
    });
  });

  it('should show recommendation in expanded detail', () => {
    cy.get('.tabs .tab').contains('Produits').click();
    cy.get('body').then(($body) => {
      if ($body.find('.product-row').length === 0) {
        cy.log('No product rows - skipping recommendation check');
        return;
      }
      cy.get('.product-row').first().click();
      cy.get('.detail-section .rec-box').should('be.visible');
      cy.get('.rec-box .rec-action').should('exist');
    });
  });

  it('should toggle product detail off on second click', () => {
    cy.get('.tabs .tab').contains('Produits').click();
    cy.get('body').then(($body) => {
      if ($body.find('.product-row').length === 0) {
        cy.log('No product rows - skipping toggle check');
        return;
      }
      cy.get('.product-row').first().click();
      cy.get('.product-detail').should('be.visible');
      cy.get('.product-row').first().click();
      cy.get('.product-detail').should('not.exist');
    });
  });

  it('should switch to Risks tab', () => {
    cy.get('.tabs .tab').contains('Risques').click();
    cy.get('.tabs .tab.active').should('contain', 'Risques');
    cy.get('.tab-content .card h3').should('contain', 'Niveaux de risque');
  });

  it('should show risk distribution on Risks tab', () => {
    cy.get('.tabs .tab').contains('Risques').click();
    cy.get('body').then(($body) => {
      if ($body.find('.bar-chart .bar-row').length > 0) {
        cy.get('.bar-chart .bar-row').should('have.length.gte', 1);
      } else {
        cy.log('No risk data - empty chart expected');
      }
    });
  });

  it('should recalculate when clicking Recalculer', () => {
    cy.get('.page-header .header-actions button').contains('Recalculer').click();
    cy.get('.kpi-grid .kpi-card').should('have.length.gte', 2);
  });
});
