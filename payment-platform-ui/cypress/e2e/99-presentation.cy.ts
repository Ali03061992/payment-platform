// Captures et video de presentation NEXTERA.
// Prerequis : UI sur http://localhost:4200 et API sur http://localhost:8081.
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

const uiLogin = (username: string, password: string) => {
  cy.visit('/login');
  cy.get('#username').should('be.visible').clear().type(username);
  cy.get('#password').clear().type(password);
  cy.get('button[type="submit"]').click();
  cy.url({ timeout: 20000 }).should('include', '/dashboard');
  cy.waitForPageLoad();
  cy.dismissOverlays();
  cy.wait(800);
};

const visitDashboardPage = (path: string) => {
  cy.visit(path);
  cy.waitForPageLoad();
  cy.dismissOverlays();
  cy.wait(900);
};

describe('99 - Presentation NEXTERA', () => {
  before(() => cy.ensureTestUsers());

  beforeEach(() => {
    cy.clearAllSessionStorage();
    cy.clearAllLocalStorage();
    cy.clearAllCookies();
  });

  it('capture la landing page et ses sections marketing', () => {
    cy.viewport(1280, 720);
    cy.visit('/');
    cy.get('[data-testid="landing-page"]').should('exist');
    cy.wait(900);
    capture('00-landing-hero');

    cy.get('#plateforme').scrollIntoView();
    cy.wait(500);
    capture('00b-landing-plateforme');

    cy.get('#conformite').scrollIntoView();
    cy.wait(500);
    capture('00c-landing-conformite');

    cy.get('.preview-section').scrollIntoView();
    cy.wait(800);
    capture('00d-landing-apercu');

    cy.get('#solutions').scrollIntoView();
    cy.wait(500);
    capture('00e-landing-solutions');
  });

  it('capture le parcours administration', () => {
    cy.viewport(1280, 720);
    cy.visit('/login');
    cy.get('#username').should('be.visible');
    cy.wait(1200);
    capture('01-login');

    uiLogin('system.admin', '@PAssword012345');
    waitDashboard();
    cy.dismissOverlays();
    capture('02-admin-dashboard');

    visitDashboardPage('/dashboard/admin/org-stats');
    capture('03-admin-organisations', true);

    visitDashboardPage('/dashboard/payments');
    capture('04-paiements', true);

    visitDashboardPage('/dashboard/notifications');
    capture('05-notifications');

    visitDashboardPage('/dashboard/security');
    capture('06-securite-passkeys');

    cy.viewport(390, 720);
    cy.visit('/dashboard');
    waitDashboard();
    cy.dismissOverlays();
    cy.wait(1000);
    capture('07-mobile-dashboard');
  });

  it('capture le parcours fournisseur', () => {
    cy.viewport(1280, 720);
    cy.getTestCtx().then((ctx) => uiLogin(ctx.users.supplierAdmin.username, 'test1234'));
    waitDashboard();
    cy.dismissOverlays();
    capture('08-fournisseur-dashboard');

    visitDashboardPage('/dashboard/supplier/products');
    capture('09-fournisseur-produits', true);

    visitDashboardPage('/dashboard/supplier/orders');
    capture('10-fournisseur-commandes', true);

    visitDashboardPage('/dashboard/supplier/invoices');
    capture('11-journal-factures');

    visitDashboardPage('/dashboard/supplier/financial');
    capture('12-fournisseur-finances');

    visitDashboardPage('/dashboard/supplier/deliveries');
    capture('13-fournisseur-livraisons', true);
  });

  it('capture le parcours boutique et les reclamations', () => {
    cy.viewport(1280, 720);
    cy.getTestCtx().then((ctx) => uiLogin(ctx.users.shopAdmin.username, 'test1234'));
    waitDashboard();
    cy.dismissOverlays();
    capture('14-boutique-dashboard');

    visitDashboardPage('/dashboard/shop/orders');
    capture('15-boutique-commandes');

    visitDashboardPage('/dashboard/shop/orders/create');
    capture('16-nouvelle-commande');

    visitDashboardPage('/dashboard/reclamations');
    capture('17-reclamations');

    visitDashboardPage('/dashboard/shop/quotes');
    capture('18-devis-boutique');

    cy.viewport(390, 720);
    cy.visit('/dashboard/reclamations');
    cy.waitForPageLoad();
    cy.dismissOverlays();
    cy.wait(1000);
    capture('19-mobile-reclamations');
  });
});
