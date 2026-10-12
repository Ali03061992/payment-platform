// Parcours 21-withholding-certificates.cy.ts : 21 - Certificats de retenue à la source (TEJ).
// Les certificats sont rattachés aux FACTURES DE COMMANDES (le paiement suit la facture) :
// émission à la livraison, montants TTC, prix TTC sans TVA ajoutée.
// Prérequis : services live, utilisateurs seedés via cy.ensureTestUsers(), API live.
// Ne pas lancer sans go explicite : suite e2e live uniquement.
const API = () => Cypress.env('apiUrl') || 'http://localhost:8081';

function hdr(token: string) {
  return { Authorization: `Bearer ${token}` };
}

function setupCtx() {
  const ctx: any = {};
  return cy.apiLogin('system.admin').then((t) => { ctx.adminToken = t; })
    .then(() => cy.apiLogin('covale.admin.e2e')).then((t) => { ctx.supplierToken = t; })
    .then(() => cy.apiLogin('covale.agent1.e2e')).then((t) => { ctx.agentToken = t; })
    .then(() => cy.apiLogin('abdelslam.e2e')).then((t) => { ctx.shopToken = t; })
    .then(() => cy.request({ method: 'GET', url: `${API()}/api/auth/me`, headers: hdr(ctx.agentToken) }))
    .then((r) => { ctx.agentId = r.body.id; })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/suppliers')).then((r) => {
      const body: any = r.body;
      const list = Array.isArray(body) ? body : (body?.items || body?.content || []);
      ctx.covaleId = list.find((s: any) => s.name === 'Covale E2E')?.id;
    })
    .then(() => cy.apiGet(ctx.adminToken, '/api/admin/shops')).then((r) => {
      const body: any = r.body;
      const list = Array.isArray(body) ? body : (body?.items || body?.content || []);
      ctx.shopAbdelslamId = list.find((s: any) => s.name === 'Abdelslam Tunis E2E')?.id;
    })
    .then(() => ensureCertProduct(ctx, `RAS-EXP-${Date.now()}`, 10000, 'expProductId'))
    .then(() => ensureCertProduct(ctx, `RAS-CHEAP-${Date.now()}`, 10, 'cheapProductId'))
    .then(() => cy.wrap(ctx));
}

function ensureCertProduct(ctx: any, sku: string, price: number, key: string) {
  return cy.apiPost(ctx.supplierToken, `/api/suppliers/${ctx.covaleId}/products`, {
    name: `Produit RAS ${sku}`, sku, description: 'E2E RAS', unitPrice: price,
    currency: 'TND', quantity: 100, minQuantity: 1,
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201], `Product creation failed: ${JSON.stringify(r.body)}`);
    ctx[key] = r.body.id;
  });
}

/** Commande livrée (facture) : produit cher (certificat) ou pas cher (sous-seuil). */
function deliverOrder(ctx: any, expensive: boolean) {
  const productId = expensive ? ctx.expProductId : ctx.cheapProductId;
  return cy.apiPost(ctx.shopToken, '/api/orders', {
    supplierId: ctx.covaleId, shopId: ctx.shopAbdelslamId,
    asapPayment: false, currency: 'TND', notes: 'E2E RAS',
    items: [{ productId, quantity: 1, discount: 0 }],
  }).then((r) => {
    expect(r.status).to.be.oneOf([200, 201], `Order creation failed: ${JSON.stringify(r.body)}`);
    const id = r.body.id as string;
    ctx.lastTotal = r.body.total as number;
    return cy.apiPost(ctx.supplierToken, `/api/orders/${id}/confirm`, {})
      .then(() => cy.apiPost(ctx.supplierToken, `/api/orders/${id}/prepare`, {}))
      .then(() => cy.apiPost(ctx.supplierToken, `/api/orders/${id}/ready`, {}))
      .then(() => cy.apiPost(ctx.supplierToken, `/api/orders/${id}/assign-delivery`, {
        agentId: ctx.agentId, plannedDeliveryDate: '2026-09-15',
      }))
      .then(() => cy.apiPost(ctx.agentToken, `/api/orders/${id}/accept-delivery`, { accepted: true }))
      .then(() => cy.apiPost(ctx.agentToken, `/api/orders/${id}/confirm-delivery`, { confirmedDate: '2026-09-14' }))
      .then(() => cy.request({
        method: 'POST',
        url: `${API()}/api/orders/${id}/deliver`,
        headers: hdr(ctx.agentToken),
        body: { receivedBy: ctx.agentId },
        failOnStatusCode: false,
      }))
      .then((d) => {
        expect(d.status).to.eq(200, `Deliver failed: ${JSON.stringify(d.body)}`);
        return cy.wrap(id);
      });
  });
}

describe('21 - Withholding certificates (API)', () => {
  before(() => cy.ensureTestUsers());

  it('should issue a READY certificate at 1.5% on the delivered invoice', () => {
    setupCtx().then((ctx: any) => {
      deliverOrder(ctx, true).then((id: string) => {
        cy.apiGet(ctx.shopToken, `/api/orders/${id}/withholding-certificate`).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body.status).to.eq('READY');
          expect(r.body.nature).to.eq('MARCHANDISES');
          expect(r.body.rate).to.eq(1.5);
          expect(r.body.number).to.match(/^CRT-/);
          expect(r.body.debtorMatricule).to.be.a('string').and.not.empty;
          expect(r.body.orderReference).to.be.a('string').and.not.empty;
        });
      });
    });
  });

  it('should return 404 below the 1000 TTC threshold', () => {
    setupCtx().then((ctx: any) => {
      deliverOrder(ctx, false).then((id: string) => {
        // Produit à 10 DT : TTC ~12 DT < 1000 → sans assiette → 404.
        cy.request({
          method: 'GET',
          url: `${API()}/api/orders/${id}/withholding-certificate`,
          headers: hdr(ctx.shopToken),
          failOnStatusCode: false,
        }).then((r) => {
          expect(r.status).to.eq(404);
        });
      });
    });
  });

  it('should download PDF and XML of the certificate', () => {
    setupCtx().then((ctx: any) => {
      deliverOrder(ctx, true).then((id: string) => {
        cy.request({
          method: 'GET',
          url: `${API()}/api/orders/${id}/withholding-certificate/pdf`,
          headers: hdr(ctx.shopToken),
          encoding: 'binary',
        }).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.headers['content-type']).to.contain('application/pdf');
        });
        cy.request({
          method: 'GET',
          url: `${API()}/api/orders/${id}/withholding-certificate/xml`,
          headers: hdr(ctx.shopToken),
        }).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.contain('<certificat>');
          expect(r.body).to.contain('<facture>');
          expect(r.body).to.contain('<retenue>');
        });
      });
    });
  });

  it('should mark deposited, list due deadlines and export the monthly batch', () => {
    setupCtx().then((ctx: any) => {
      deliverOrder(ctx, true).then((id: string) => {
        cy.apiPatch(ctx.shopToken, `/api/orders/${id}/withholding-certificate/deposit`, { tejReference: 'TEJ-E2E-001' }).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body.status).to.eq('DEPOSITED');
          expect(r.body.tejReference).to.eq('TEJ-E2E-001');
        });
        const month = new Date().toISOString().slice(0, 7);
        cy.request({
          method: 'GET',
          url: `${API()}/api/orders/withholding-certificates/batch?debtorId=${ctx.shopAbdelslamId}&month=${month}`,
          headers: hdr(ctx.shopToken),
        }).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.contain('<certificatsRetenue');
        });
        cy.request({
          method: 'GET',
          url: `${API()}/api/orders/withholding-certificates/due?debtorId=${ctx.shopAbdelslamId}&days=90`,
          headers: hdr(ctx.shopToken),
        }).then((r) => {
          expect(r.status).to.eq(200);
          expect(r.body).to.be.an('array').with.length.of.at.least(1);
        });
      });
    });
  });
});

describe('21 - TEJ guide and prepare pages', () => {
  before(() => cy.ensureTestUsers());

  it('should display the TEJ guide with platform link', () => {
    cy.loginAsShopAdmin();
    cy.visit('/dashboard/payments-help/tej');
    cy.dismissOverlays();
    cy.get('.page-header h2', { timeout: 10000 }).should('contain', 'TEJ');
    cy.get('a.btn-primary[href="https://tej.finances.gov.tn"]').should('exist');
    cy.contains('30 %').should('exist');
  });

  it('should prepare a filing by order reference and by manual entry', () => {
    setupCtx().then((ctx: any) => {
      deliverOrder(ctx, true).then((id: string) => {
        cy.loginAsShopAdmin();
        cy.visit('/dashboard/payments-help/tej-prepare');
        cy.dismissOverlays();
        cy.get('input[name="reference"]').clear().type(id);
        cy.dismissOverlays();
        cy.get('.form-card button.btn-primary').first().click({ force: true });
        cy.contains('CRT-', { timeout: 10000 }).should('exist');
        cy.get('.tab-btn').contains('Saisie').click();
        cy.get('input[name="mGross"]').clear().type('10000');
        cy.get('input[name="mDebtorName"]').clear().type('Debiteur E2E');
        cy.get('input[name="mBenName"]').clear().type('Beneficiaire E2E');
        cy.get('input[name="mBenMf"]').clear().type('1234567A/A/M/000');
        cy.get('input[name="mPayRef"]').clear().type(`E2E-MAN-${Date.now()}`);
        cy.get('input[name="mPayDate"]').type('2026-07-15');
        cy.contains('150').should('exist');
      });
    });
  });
});

describe('21 - Withholding certificates (UI)', () => {
  before(() => cy.ensureTestUsers());

  it('should show certificate box and download buttons on order detail', () => {
    setupCtx().then((ctx: any) => {
      deliverOrder(ctx, true).then((id: string) => {
        cy.loginAsShopAdmin();
        cy.visit(`/dashboard/shop/orders/${id}`);
        cy.dismissOverlays();
        cy.contains('Retenue à la source', { timeout: 10000 }).should('exist');
        cy.contains('1.5').should('exist');
        cy.get('button').contains('Certificat de retenue (PDF)').should('exist');
        cy.get('button').contains('Certificat XML (TEJ)').should('exist');
      });
    });
  });
});
