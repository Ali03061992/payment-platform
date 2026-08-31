# Cypress Component Testing Setup (Documentation)

## Current Status: Incompatible with Angular 16

**Cypress Component Testing does not work with Angular 16 + Cypress 15.x** due to a fundamental webpack version mismatch:

| Component | Version | Requirement |
|-----------|---------|-------------|
| Angular | 16.2.x | Uses webpack 5 + webpack-dev-server 4.15.1 |
| Cypress | 15.21.x | Internal webpack-dev-server requires v5+ |
| @cypress/angular | Any | Requires Angular 17+ for Cypress 15+ |

## Error
```
Error: Unexpected major version of webpack-dev-server. 
Cypress webpack-dev-server works with webpack-dev-server versions 5 - saw 4.15.1
```

## Solutions (pick one)

### Option 1: Upgrade to Angular 17+ (Recommended)
```bash
ng update @angular/core@17 @angular/cli@17
# Then use @cypress/angular@latest with Cypress 15+
```

### Option 2: Downgrade Cypress to 13.x
```bash
npm install cypress@13.17.0 --legacy-peer-deps
# Works with Angular 16's webpack-dev-server 4
# But loses Cypress 14/15 features
```

### Option 3: Use Custom Webpack 5 with Angular 16 (Experimental)
```bash
ng config cli.packageManager npm
ng config cli.defaultProject payment-platform-ui
# Add webpack 5 config in angular.json
"architect": {
  "build": {
    "builder": "@angular-devkit/build-angular:browser",
    "options": {
      "webpackConfig": "webpack.config.js"
    }
  }
}
```

## Component Tests Created (Ready for Angular 17+)

The following component test files are created and ready to use once upgraded:

| Test File | Component | Coverage |
|-----------|-----------|----------|
| `src/app/login/login.component.cy.ts` | LoginComponent | Form validation, success/error flows, loading state, fallback user |
| `src/app/layout/layout.component.cy.ts` | LayoutComponent | Role-based nav, sidebar toggle, notification panel, logout, initials |
| `src/app/payments/payment-detail/payment-detail.component.cy.ts` | PaymentDetailComponent | Load payment, confirm/reject/cancel, QR code, status badges, history |

## Running Component Tests (After Angular 17+ Upgrade)

```bash
# Open Cypress Component Testing UI
npm run cy:open --component

# Run headless
npm run cy:run --component

# Or add to package.json:
# "cy:run:component": "cypress run --component"
```

## Current Test Strategy: E2E Tests Only

Until Angular 17+ migration, the test suite relies on **133+ E2E tests** covering:

| Test Suite | Tests | Status |
|------------|-------|--------|
| api-integration | 25 | ✅ |
| admin | 14 | ✅ |
| auth | 10 | ✅ |
| navigation | 12 | ✅ |
| notifications | 11 | ✅ |
| payments | 9 | ✅ |
| shop | 6 | ✅ |
| supplier | 13 | ✅ |
| **data-structure (Covale/Pointteck)** | **33** | **✅** |
| **TOTAL** | **133+** | **✅** |

E2E tests provide full integration coverage including:
- Authentication & RBAC
- Organization structure (Covale/Pointteck)
- Payment flows with notifications
- UI navigation per role
- API contract validation

## Migration Checklist for Angular 17+

When ready to enable Component Testing:

1. [ ] `ng update @angular/core@17 @angular/cli@17`
2. [ ] `npm install @cypress/angular@latest --legacy-peer-deps`
3. [ ] Update `cypress.config.ts` for Angular 17+ webpack 5
4. [ ] Run `npx cypress run --component` to verify
5. [ ] Add component test CI step

## Component Test Patterns Used

```typescript
// Mount with providers
cy.mount(MyComponent, {
  providers: [
    { provide: MyService, useValue: serviceSpy },
    { provide: Router, useValue: routerSpy },
  ],
});

// Test with delayed observables
let resolve: (value: any) => void;
const delayed$ = new Promise(r => { resolve = r; });
serviceSpy.method.and.returnValue(
  new Observable(obs => delayed$.then(v => { obs.next(v); obs.complete(); }))
);

// Test loading states
cy.contains('Chargement').should('exist');
resolve!(mockData);
cy.contains('Expected Data').should('exist');
```

---

*Generated for Payment Platform v1.0.0 • Angular 16 → 17 migration pending*