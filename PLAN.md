# Plan: P3-02 — i18n with ngx-translate

> Plan non exécuté (dette phase 2, cf. [COMPTE_RENDU_EXPERT.md](COMPTE_RENDU_EXPERT.md) §3.3 :
> `ngx-translate` décoratif). À trancher (activer ou supprimer) avant exécution. Index : [docs/README.md](docs/README.md).

## Context

The payment-platform project already has ngx-translate installed (`@ngx-translate/core@^15.0.0`, `@ngx-translate/http-loader@^8.0.0`) and an `AppTranslateModule` configured. Translation files (`fr.json`, `en.json`) exist with ~278 lines of keys each, and a `LanguageSwitcherComponent` exists. However, **the feature is incomplete**:

1. `AppTranslateModule` is imported at the top of `app.module.ts` (line 60) but **NOT added to the `@NgModule` imports array** (lines 113-118) — so `TranslateModule` is never actually loaded.
2. The `LanguageSwitcherComponent` is declared in `app.module.ts` (line 61/110) but **never placed in any template**.
3. HTML templates still have **hardcoded French texts** (login, register, layout, payment-list, payment-detail, dashboard-admin, stock-management).
4. TypeScript files have **hardcoded French strings** (login error messages, register role labels, layout nav labels, stock-management status labels, payment-list status labels).
5. No locale-aware date/number formatting exists.

## Changes

### 1. Add `AppTranslateModule` to `app.module.ts` imports array
- **File:** `src/app/app.module.ts:113-118`
- Add `AppTranslateModule` to the `imports` array so `TranslateModule` and `TranslatePipe`/`TranslateDirective` are available app-wide.

### 2. Add `app-language-switcher` to the layout header
- **File:** `src/app/layout/layout.component.html`
- Add `<app-language-switcher>` to the desktop top-bar (line ~146-163, inside `.header-actions`).
- Add `<app-language-switcher>` to the mobile header (line ~10-37, inside `.mobile-header-actions`).

### 3. Replace hardcoded texts in HTML templates with `| translate` pipe (representative sample)

#### 3a. Login component
- **File:** `src/app/login/login.component.html`
- Replace: `Connexion` → `{{ 'AUTH.LOGIN_TITLE' | translate }}`, `Accédez à votre espace` → `{{ 'AUTH.LOGIN_SUBTITLE' | translate }}`, labels, button text, error messages.

#### 3b. Register component
- **File:** `src/app/register/register.component.html`
- Replace all hardcoded French labels: `Créer un compte`, `Rejoignez la plateforme de paiement`, `Nom`, `Prénom`, `Email`, `Téléphone (optionnel)`, `Mot de passe`, `Rôle`, button text, success messages, link text.

#### 3c. Layout component
- **File:** `src/app/layout/layout.component.html`
- Replace: `Payment Platform` (logo text), `Bienvenue, ...` greeting, `En ligne` badge, `Déconnexion`, `Notifications`, `Tout marquer lu`, `Aucune notification`.
- Replace nav item labels via translation keys.

#### 3d. Payment list component
- **File:** `src/app/payments/payment-list/payment-list.component.html`
- Replace: `Paiements`, `+ Nouveau paiement`, filter options (`Tous les statuts`, `En attente`, etc.), table headers (`Référence`, `Boutique`, etc.), button text (`Confirmer`, `Annuler`, `Détail`), `Aucun paiement trouvé`, card labels.

#### 3e. Payment detail component
- **File:** `src/app/payments/payment-detail/payment-detail.component.html`
- Replace: `Chargement...`, `Télécharger facture`, QR section text, detail labels (`Référence`, `Statut`, `Montant`, etc.), action buttons, history table headers, rejection form text.

#### 3f. Dashboard admin component
- **File:** `src/app/dashboard/dashboard-admin/dashboard-admin.component.html`
- Replace: `Vue d'ensemble de la plateforme`, stat labels (`Total utilisateurs`, `Comptes actifs`, etc.), profile card labels, payment stats labels, quick action labels.

#### 3g. Stock management component
- **File:** `src/app/supplier/stock-management/stock-management.component.html`
- Replace: `Gestion du stock`, stat labels, filter options, table headers, modal labels, empty states, movement history labels.

### 4. Replace hardcoded texts in TypeScript files

#### 4a. Login component TS
- **File:** `src/app/login/login.component.ts:42,48`
- Replace hardcoded error strings with `TranslateService` usage.

#### 4b. Register component TS
- **File:** `src/app/register/register.component.ts:23-27,40`
- Replace hardcoded role labels with translation keys.

#### 4c. Layout component TS
- **File:** `src/app/layout/layout.component.ts:32-59`
- Replace nav item labels with translation keys; use `TranslateService` to resolve them dynamically.

#### 4d. Payment list component TS
- **File:** `src/app/payments/payment-list/payment-list.component.ts:42-45`
- Replace hardcoded `statusLabel()` map with translation keys.

#### 4e. Payment detail component TS
- **File:** `src/app/payments/payment-detail/payment-detail.component.ts:56,64,72,97,102`
- Replace hardcoded toast messages and status/action labels with translation keys.

#### 4f. Stock management component TS
- **File:** `src/app/supplier/stock-management/stock-management.component.ts:115,116,132,133`
- Replace hardcoded toast messages and status/type labels with translation keys.

### 5. Add locale-aware date/number formatting

#### 5a. Create a locale-aware date pipe or use Angular's built-in `DatePipe` with locale
- **File:** `src/app/i18n/app-translate.module.ts`
- Register `LOCALE_ID` provider based on current language.
- Provide `registerLocaleData` for `fr` and `en`.

#### 5b. Update date/number pipes in templates
- Files: `payment-list.component.html`, `payment-detail.component.html`, `stock-management.component.html`
- Use `date:'short'` or `date:'medium'` (which respect `LOCALE_ID`), or use a custom pipe.

### 6. Update `LanguageSwitcherComponent` to also update `LOCALE_ID`
- **File:** `src/app/i18n/language-switcher.component.ts`
- When switching language, update `LOCALE_ID` by re-providing or using `DatePipe`/`DecimalPipe` locale.

## Verification

1. Run `ng build` to verify no compilation errors.
2. Run `ng test --watch=false` to verify existing tests still pass.
3. Manually verify: language switcher visible in header, switching FR/EN updates all translated texts, date formats adapt (dd/MM/yyyy for FR, MM/dd/yyyy for EN).
