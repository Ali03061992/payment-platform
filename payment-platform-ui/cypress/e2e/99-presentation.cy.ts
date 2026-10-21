// Captures pour la présentation NEXTERA (supprimé après génération des assets).
const cleanCapture = () => {
  cy.document().then((doc) => {
    doc.documentElement.style.setProperty('overflow', 'hidden', 'important');
    doc.body.style.setProperty('overflow', 'hidden', 'important');
    doc.querySelectorAll('.layout, .main-content, .content-area').forEach((el) => {
      (el as HTMLElement).style.setProperty('overflow', 'hidden', 'important');
    });
    if (doc.getElementById('presentation-capture-style')) return;
    const style = doc.createElement('style');
    style.id = 'presentation-capture-style';
    style.textContent = `
      html, body { overflow: hidden !important; scrollbar-width: none !important; }
      ::-webkit-scrollbar { display: none !important; width: 0 !important; height: 0 !important; }
    `;
    doc.head.appendChild(style);
  });
};

const capture = (name: string, collapseSidebar = false) => {
  cy.get('body').then(($body) => {
    const $btn = $body.find('.toggle-btn');
    const collapsed = $body.find('.layout.sidebar-collapsed').length > 0;
    if ($btn.length && $btn.is(':visible') && collapsed !== collapseSidebar) {
      cy.wrap($btn).click();
    }
  });
  cleanCapture();
  cy.wait(600);
  cy.screenshot(name, { capture: 'viewport', overwrite: true });
};

const waitDashboard = () => {
  cy.get('.stats-grid', { timeout: 20000 }).should('be.visible');
  cy.get('.loading').should('not.exist');
  cy.wait(800);
};

describe('99 - Présentation NEXTERA', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.clearAllSessionStorage();
    cy.clearAllLocalStorage();
    cy.clearCookies();
  });

  it('capture le parcours administration', () => {
    cy.viewport(1280, 720);
    cy.visit('/login');
    cy.get('#username').should('be.visible');
    cy.wait(1200);
    capture('01-login');

    cy.loginAsAdmin();
    cy.wait(800);
    cy.visit('/dashboard');
    cy.waitForPageLoad();
    waitDashboard();
    capture('02-admin-dashboard');

    cy.visit('/dashboard/admin/suppliers');
    cy.waitForPageLoad();
    cy.wait(1000);
    capture('03-admin-fournisseurs');

    cy.visit('/dashboard/payments');
    cy.waitForPageLoad();
    cy.wait(900);
    capture('04-paiements', true);

    cy.visit('/dashboard/notifications');
    cy.wait(900);
    capture('05-notifications');

    cy.visit('/dashboard/security');
    cy.wait(900);
    capture('06-securite-passkeys');

    cy.viewport(390, 720);
    cy.visit('/dashboard');
    waitDashboard();
    cy.wait(1000);
    capture('07-mobile-dashboard');
  });

  it('capture le parcours fournisseur', () => {
    cy.viewport(1280, 720);
    cy.loginAsSupplierAdmin();
    cy.wait(800);
    cy.visit('/dashboard');
    cy.waitForPageLoad();
    waitDashboard();
    capture('08-fournisseur-dashboard');

    cy.visit('/dashboard/supplier/products');
    cy.waitForPageLoad();
    cy.wait(1000);
    capture('09-fournisseur-produits', true);

    cy.visit('/dashboard/supplier/orders');
    cy.waitForPageLoad();
    cy.wait(1000);
    capture('10-fournisseur-commandes', true);

    cy.visit('/dashboard/supplier/invoices');
    cy.waitForPageLoad();
    cy.wait(1000);
    capture('11-journal-factures');

    cy.visit('/dashboard/payments');
    cy.waitForPageLoad();
    cy.wait(900);
    capture('12-fournisseur-paiements', true);
  });

  it('capture le parcours boutique', () => {
    cy.viewport(1280, 720);
    cy.loginAsShopAdmin();
    cy.wait(800);
    cy.visit('/dashboard');
    cy.waitForPageLoad();
    waitDashboard();
    capture('13-boutique-dashboard');

    cy.visit('/dashboard/shop/orders');
    cy.waitForPageLoad();
    cy.wait(1000);
    capture('14-boutique-commandes');

    cy.visit('/dashboard/shop/orders/create');
    cy.waitForPageLoad();
    cy.wait(1000);
    capture('15-nouvelle-commande');
  });
});
