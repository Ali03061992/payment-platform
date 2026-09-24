const API_URL = () => Cypress.env('apiUrl') || 'http://localhost:8081';

const ADMIN_PASSWORD = '@PAssword012345';
const E2E_PASSWORD = 'test1234';

function authHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function uid() {
  return `e2e_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
}

// ── Login commands ──────────────────────────────────────────────────

Cypress.Commands.add('login', (username: string, password: string) => {
  cy.request({
    method: 'POST',
    url: `${API_URL()}/api/auth/login`,
    body: { username, password },
    failOnStatusCode: false,
  }).then((resp) => {
    expect(resp.status).to.eq(200, `Login failed for ${username}: ${resp.status}`);
    expect(resp.body).to.have.property('accessToken');
    const token = resp.body.accessToken;
    cy.window().then((win) => {
      win.sessionStorage.setItem('token', token);
      win.sessionStorage.setItem('user', JSON.stringify(resp.body.user));
      try {
        win.localStorage.setItem('onboarding_completed', 'true');
        win.localStorage.setItem('notification_choice', 'dismissed');
      } catch {
        // ignore
      }
    });
  });
});

Cypress.Commands.add('dismissOverlays', () => {
  cy.window({ log: false }).then((win) => {
    try {
      win.localStorage.setItem('onboarding_completed', 'true');
      win.localStorage.setItem('notification_choice', 'dismissed');
    } catch {
      // ignore
    }
  });
  cy.get('body', { log: false }).then(($body) => {
    if ($body.find('.tour-tooltip .tour-btn-skip').length) {
      cy.get('.tour-tooltip .tour-btn-skip', { log: false }).first().click({ force: true });
    }
    if ($body.find('.notification-banner .banner-btn-dismiss').length) {
      cy.get('.notification-banner .banner-btn-dismiss', { log: false }).first().click({ force: true });
    }
  });
});

Cypress.Commands.add('loginAsAdmin', () => {
  cy.login('system.admin', ADMIN_PASSWORD);
});

Cypress.Commands.add('loginAsSupplierAdmin', () => {
  cy.getTestCtx().then((ctx) => {
    cy.login(ctx.users.supplierAdmin.username, E2E_PASSWORD);
  });
});

Cypress.Commands.add('loginAsPointteckAdmin', () => {
  cy.getTestCtx().then((ctx) => {
    cy.login(ctx.users.pointteckAdmin.username, E2E_PASSWORD);
  });
});

Cypress.Commands.add('loginAsShopAdmin', () => {
  cy.getTestCtx().then((ctx) => {
    cy.login(ctx.users.shopAdmin.username, E2E_PASSWORD);
  });
});

Cypress.Commands.add('loginAsAli', () => {
  cy.getTestCtx().then((ctx) => {
    cy.login(ctx.users.shopAli.username, E2E_PASSWORD);
  });
});

Cypress.Commands.add('loginAsCovaleAgent', () => {
  cy.getTestCtx().then((ctx) => {
    cy.login(ctx.users.supplierAgent1.username, E2E_PASSWORD);
  });
});

Cypress.Commands.add('loginAsPointteckAgent', () => {
  cy.getTestCtx().then((ctx) => {
    cy.login(ctx.users.pointteckAgent1.username, E2E_PASSWORD);
  });
});

// ── Organization helpers ────────────────────────────────────────────

function createOrg(token: string, name: string, type: string) {
  const endpoint = type === 'SUPPLIER' ? 'suppliers' : 'shops';
  return cy.request({
    method: 'POST',
    url: `${API_URL()}/api/admin/${endpoint}`,
    headers: authHeaders(token),
    body: { name, type },
    failOnStatusCode: false,
  }).then((r) => {
    if ((r.status === 200 || r.status === 201) && r.body?.id) return r.body;
    // Already exists or transient error - find it
    return cy.request({
      method: 'GET',
      url: `${API_URL()}/api/admin/${endpoint}`,
      headers: authHeaders(token),
      failOnStatusCode: false,
    }).then((list) => {
      if (list.status !== 200) return null;
      const body = list.body;
      const items = Array.isArray(body) ? body
        : Array.isArray(body?.content) ? body.content
        : Array.isArray(body?.items) ? body.items
        : Array.isArray(body?.value) ? body.value
        : Array.isArray(body?.data) ? body.data
        : [];
      const existing = items.find((o: any) => o.name === name);
      if (existing) return existing;
      // Fuzzy match
      const words = name.split(' ');
      const fuzzy = items.find((o: any) => words.some((w) => o.name.includes(w)));
      return fuzzy || null;
    });
  });
}

function ensureOrgActive(token: string, org: any, type: string) {
  if (!org?.id) return cy.wrap(null);
  if (org.status === 'ACTIVE') return cy.wrap(org);
  const endpoint = type === 'SUPPLIER' ? 'suppliers' : 'shops';
  return cy.request({
    method: 'PATCH',
    url: `${API_URL()}/api/admin/${endpoint}/${org.id}/activate`,
    headers: authHeaders(token),
    failOnStatusCode: false,
  }).then(() => org);
}

function createRelation(token: string, supplierId: string, shopId: string) {
  return cy.request({
    method: 'POST',
    url: `${API_URL()}/api/admin/supplier-shop-relations`,
    headers: authHeaders(token),
    body: { supplierId, shopId },
    failOnStatusCode: false,
  }).then((r) => {
    if (r.status === 200 || r.status === 201) return r.body;
    // Already exists
    return cy.request({
      method: 'GET',
      url: `${API_URL()}/api/admin/supplier-shop-relations`,
      headers: authHeaders(token),
      failOnStatusCode: false,
    }).then((list) => {
      const body = list.body;
      const items = Array.isArray(body) ? body
        : Array.isArray(body?.content) ? body.content
        : Array.isArray(body?.items) ? body.items
        : Array.isArray(body?.value) ? body.value
        : Array.isArray(body?.data) ? body.data
        : [];
      return items.find((r: any) => r.supplierId === supplierId && r.shopId === shopId) || { id: 'unknown' };
    });
  });
}

// ── User helpers ────────────────────────────────────────────────────

function createUser(token: string, data: any) {
  return cy.request({
    method: 'POST',
    url: `${API_URL()}/api/users`,
    headers: authHeaders(token),
    body: data,
    failOnStatusCode: false,
  }).then((r) => {
    if (r.status === 200 || r.status === 201) return r.body;
    // User may already exist - try to find via login
    return cy.request({
      method: 'POST',
      url: `${API_URL()}/api/auth/login`,
      body: { username: data.username, password: data.password },
      failOnStatusCode: false,
    }).then((lr) => {
      if (lr.status === 200) return lr.body.user;
      return null;
    });
  });
}

// ── Test context builder ────────────────────────────────────────────
// Builds all test data once and caches it in Cypress.env()

interface TestContext {
  suppliers: { covale: any; pointteck: any };
  shops: { abdelslam: any; ali: any; ptTunis: any; ptSfax: any };
  users: {
    supplierAdmin: any;
    supplierAgent1: any;
    pointteckAdmin: any;
    pointteckAgent1: any;
    shopAdmin: any;
    shopAli: any;
  };
}

function buildTestContext(): Cypress.Chainable<TestContext> {
  const existing = Cypress.env('testCtx') as TestContext | undefined;
  if (existing && existing.suppliers?.covale?.id) {
    return cy.wrap(existing);
  }

  const api = API_URL();
  let adminToken = '';

  return cy.request({
    method: 'POST',
    url: `${api}/api/auth/login`,
    body: { username: 'system.admin', password: ADMIN_PASSWORD },
  }).then((r) => {
    adminToken = r.body.accessToken;
    const h = authHeaders(adminToken);

    // Create suppliers
    return createOrg(adminToken, `Covale E2E`, 'SUPPLIER');
  }).then((covale) => {
    return ensureOrgActive(adminToken, covale, 'SUPPLIER').then((c) => {
      const ctx: any = Cypress.env('testCtx') || {};
      ctx.suppliers = ctx.suppliers || {};
      ctx.suppliers.covale = c;
      Cypress.env('testCtx', ctx);
      if (c?.id) Cypress.env('covaleId', c.id);
      return createOrg(adminToken, `Pointteck E2E`, 'SUPPLIER');
    });
  }).then((pointteck) => {
    return ensureOrgActive(adminToken, pointteck, 'SUPPLIER').then((p) => {
      const ctx: any = Cypress.env('testCtx');
      ctx.suppliers.pointteck = p;
      Cypress.env('testCtx', ctx);
      if (p?.id) Cypress.env('pointteckId', p.id);
      return createOrg(adminToken, `Abdelslam Tunis E2E`, 'SHOP');
    });
  }).then((shopAbdelslam) => {
    return ensureOrgActive(adminToken, shopAbdelslam, 'SHOP').then((s) => {
      const ctx: any = Cypress.env('testCtx');
      ctx.shops = ctx.shops || {};
      ctx.shops.abdelslam = s;
      Cypress.env('testCtx', ctx);
      if (s?.id) Cypress.env('shopAbdelslamId', s.id);
      return createOrg(adminToken, `Ali Sfax E2E`, 'SHOP');
    });
  }).then((shopAli) => {
    return ensureOrgActive(adminToken, shopAli, 'SHOP').then((s) => {
      const ctx: any = Cypress.env('testCtx');
      ctx.shops.ali = s;
      Cypress.env('testCtx', ctx);
      if (s?.id) Cypress.env('shopAliId', s.id);
      return createOrg(adminToken, `Pointteck Tunis E2E`, 'SHOP');
    });
  }).then((ptTunis) => {
    return ensureOrgActive(adminToken, ptTunis, 'SHOP').then((s) => {
      const ctx: any = Cypress.env('testCtx');
      ctx.shops.ptTunis = s;
      Cypress.env('testCtx', ctx);
      if (s?.id) Cypress.env('shopPtTunisId', s.id);
      return createOrg(adminToken, `Pointteck Sfax E2E`, 'SHOP');
    });
  }).then((ptSfax) => {
    return ensureOrgActive(adminToken, ptSfax, 'SHOP').then((s) => {
      const ctx: any = Cypress.env('testCtx');
      ctx.shops.ptSfax = s;
      Cypress.env('testCtx', ctx);
      if (s?.id) Cypress.env('shopPtSfaxId', s.id);

      // Create relations (skip pairs with missing ids)
      const pairs = [
        { sid: ctx.suppliers.covale?.id, shopid: ctx.shops.abdelslam?.id },
        { sid: ctx.suppliers.covale?.id, shopid: ctx.shops.ali?.id },
        { sid: ctx.suppliers.pointteck?.id, shopid: ctx.shops.ptTunis?.id },
        { sid: ctx.suppliers.pointteck?.id, shopid: ctx.shops.ptSfax?.id },
      ].filter((p) => p.sid && p.shopid);
      return cy.wrap(pairs).each((pair: any) => {
        createRelation(adminToken, pair.sid, pair.shopid);
      }).then(() => ctx);
    });
  }).then((ctx: any) => {
    // Create users (skip users whose org is missing)
    const users = [
      { username: 'covale.admin.e2e', password: E2E_PASSWORD, firstName: 'Covale', lastName: 'Admin', email: `covale.admin.e2e@e2e.test`, role: 'SUPPLIER_ADMIN', organizationId: ctx.suppliers.covale?.id },
      { username: 'covale.agent1.e2e', password: E2E_PASSWORD, firstName: 'Agent', lastName: 'Covale1', email: `agent1.e2e@e2e.test`, role: 'SUPPLIER_AGENT', organizationId: ctx.suppliers.covale?.id },
      { username: 'pointteck.admin.e2e', password: E2E_PASSWORD, firstName: 'Pointteck', lastName: 'Admin', email: `pt.admin.e2e@e2e.test`, role: 'SUPPLIER_ADMIN', organizationId: ctx.suppliers.pointteck?.id },
      { username: 'pointteck.agent1.e2e', password: E2E_PASSWORD, firstName: 'Agent', lastName: 'Pt1', email: `pt.agent1.e2e@e2e.test`, role: 'SUPPLIER_AGENT', organizationId: ctx.suppliers.pointteck?.id },
      { username: 'abdelslam.e2e', password: E2E_PASSWORD, firstName: 'Abdelslam', lastName: 'Tunis', email: `abdelslam.e2e@e2e.test`, role: 'SHOP_ADMIN', organizationId: ctx.shops.abdelslam?.id },
      { username: 'ali.e2e', password: E2E_PASSWORD, firstName: 'Ali', lastName: 'Sfax', email: `ali.e2e@e2e.test`, role: 'SHOP_ADMIN', organizationId: ctx.shops.ali?.id },
    ].filter((u) => !!u.organizationId);

    return cy.wrap(users).each((u: any) => {
      createUser(adminToken, u).then((created: any) => {
        if (!created) return;
        const ctx: any = Cypress.env('testCtx');
        ctx.users = ctx.users || {};
        if (u.role === 'SUPPLIER_ADMIN' && u.organizationId === ctx.suppliers.covale?.id) ctx.users.supplierAdmin = created;
        if (u.role === 'SUPPLIER_AGENT' && u.organizationId === ctx.suppliers.covale?.id) ctx.users.supplierAgent1 = created;
        if (u.role === 'SUPPLIER_ADMIN' && u.organizationId === ctx.suppliers.pointteck?.id) ctx.users.pointteckAdmin = created;
        if (u.role === 'SUPPLIER_AGENT' && u.organizationId === ctx.suppliers.pointteck?.id) ctx.users.pointteckAgent1 = created;
        if (u.role === 'SHOP_ADMIN' && u.organizationId === ctx.shops.abdelslam?.id) ctx.users.shopAdmin = created;
        if (u.role === 'SHOP_ADMIN' && u.organizationId === ctx.shops.ali?.id) ctx.users.shopAli = created;
        Cypress.env('testCtx', ctx);
      });
    }).then(() => {
      const ctx: any = Cypress.env('testCtx');
      if (ctx.suppliers?.covale?.id) Cypress.env('covaleId', ctx.suppliers.covale.id);
      if (ctx.suppliers?.pointteck?.id) Cypress.env('pointteckId', ctx.suppliers.pointteck.id);
      if (ctx.shops?.abdelslam?.id) Cypress.env('shopAbdelslamId', ctx.shops.abdelslam.id);
      if (ctx.shops?.ali?.id) Cypress.env('shopAliId', ctx.shops.ali.id);
      if (ctx.shops?.ptTunis?.id) Cypress.env('shopPtTunisId', ctx.shops.ptTunis.id);
      if (ctx.shops?.ptSfax?.id) Cypress.env('shopPtSfaxId', ctx.shops.ptSfax.id);
      return Cypress.env('testCtx');
    });
  });
}

Cypress.Commands.add('ensureTestUsers', () => {
  buildTestContext();
});

Cypress.Commands.add('getTestCtx', () => {
  const ctx = Cypress.env('testCtx') as TestContext | undefined;
  if (ctx) return cy.wrap(ctx);
  return buildTestContext();
});

// ── Payment helper ──────────────────────────────────────────────────

Cypress.Commands.add('createPayment', (shopId: string, supplierId: string, amount: number) => {
  cy.window().then((win) => {
    const token = win.sessionStorage.getItem('token');
    cy.request({
      method: 'POST',
      url: `${API_URL()}/api/payments`,
      headers: authHeaders(token!),
      body: { shopId, supplierId, amount, currency: 'TND' },
      failOnStatusCode: false,
    });
  });
});

// ── API helpers for tests ───────────────────────────────────────────

Cypress.Commands.add('apiLogin', (username: string, password?: string) => {
  const effectivePassword = password || (username === 'system.admin' ? ADMIN_PASSWORD : E2E_PASSWORD);
  return cy.request({
    method: 'POST',
    url: `${API_URL()}/api/auth/login`,
    body: { username, password: effectivePassword },
  }).then((r) => r.body.accessToken as string);
});

Cypress.Commands.add('apiGet', (token: string, path: string) => {
  return cy.request({
    method: 'GET',
    url: `${API_URL()}${path}`,
    headers: authHeaders(token),
  });
});

Cypress.Commands.add('apiPost', (token: string, path: string, body: any) => {
  return cy.request({
    method: 'POST',
    url: `${API_URL()}${path}`,
    headers: authHeaders(token),
    body,
    failOnStatusCode: false,
  });
});

Cypress.Commands.add('apiPatch', (token: string, path: string, body?: any) => {
  return cy.request({
    method: 'PATCH',
    url: `${API_URL()}${path}`,
    headers: authHeaders(token),
    body: body || {},
    failOnStatusCode: false,
  });
});

// ── Unique name generator ──────────────────────────────────────────

Cypress.Commands.add('uniqueName', (prefix: string) => {
  return cy.wrap(`${prefix} ${Date.now()}`);
});

// ── Type declarations ───────────────────────────────────────────────

declare namespace Cypress {
  interface Chainable {
    login(username: string, password: string): Chainable<void>;
    dismissOverlays(): Chainable<void>;
    loginAsAdmin(): Chainable<void>;
    loginAsSupplierAdmin(): Chainable<void>;
    loginAsPointteckAdmin(): Chainable<void>;
    loginAsShopAdmin(): Chainable<void>;
    loginAsAli(): Chainable<void>;
    loginAsCovaleAgent(): Chainable<void>;
    loginAsPointteckAgent(): Chainable<void>;
    ensureTestUsers(): Chainable<void>;
    getTestCtx(): Chainable<TestContext>;
    createPayment(shopId: string, supplierId: string, amount: number): Chainable<void>;
    apiLogin(username: string, password?: string): Chainable<string>;
    apiGet(token: string, path: string): Chainable<any>;
    apiPost(token: string, path: string, body: any): Chainable<any>;
    apiPatch(token: string, path: string, body?: any): Chainable<any>;
    uniqueName(prefix: string): Chainable<string>;
  }
}
