# CERTIFICATION DE SÉCURITÉ — Payment Platform B2B

**Document officiel produit par l'agent `expert-securite`**
**Statut : CERTIFIÉ SOUS CONDITIONS — mise en production interdite tant que les P0 sont ouverts**

| Champ | Valeur |
|---|---|
| Date d'émission | 07/10/2026 |
| Version | 1.0 |
| Auditeur | `expert-securite` (agent audit, ne code jamais) |
| Périmètre | `backend` (5 microservices + gateway + shared-lib) + `payment-platform-ui` (Angular) + `payment-platform-mobile` (Flutter, parité) |
| Référentiels | `docs/security.md`, `docs/MULTI_TENANCY_ARCHITECTURE.md`, `FONCTIONAL_SPECIFICATION.md`, `AUDIT.md`, `COMPTE_RENDU_EXPERT.md` (vérifié 25/09/2026), `VALIDATION_REPORT.md`, OWASP Top 10 2021, B1-B5 / M1-M7, phases 0-3 |
| Commit audité | `[À compléter : git rev-parse HEAD le jour de l'audit]` |
| Environnement cible | prod (`application-prod.yml`, MySQL 8, gateway 8081) |
| Validité | 90 jours OU jusqu'au prochain changement auth/RBAC/isolation — le plus tôt des deux. Toute correction P0 impose ré-audit §2 + ré-émission. |

> Comment utiliser : l'`expert-securite` remplit §1/§2/§3 avec preuves `fichier:ligne` réelles, prononce le verdict §4, liste les ordres §5. Sans preuves ligne-à-ligne, ce document est NUL.

---

## §1 — Aspects sécurité existants (constaté, pas supposé)

| # | Mécanisme | Preuve (`fichier:ligne`) | Statut |
|---|---|---|---|
| S01 | JWT HS256, `JWT_SECRET` ≥ 32 bytes, fail-fast prod si absent/faible, exp 30 min, claims `sub/roles/organizationId/jti` | `shared-lib/.../security/JwtService.java:30-56`, `docs/security.md §JWT` | OK |
| S02 | Double validation JWT : gateway `JwtValidationFilter` + chaque microservice ; `CurrentUser` via SecurityContext | `api-gateway/.../JwtValidationFilter.java`, `shared-lib/.../security/JwtAuthenticationFilter.java` | OK |
| S03 | Inter-services `X-Internal-Token` = `INTERNAL_SECRET`, fail-fast `InternalSecretValidator`, gardes `InternalAuthGuard` / `PaymentInternalAuthGuard`, 401 unifié | `GatewayProxyController.java:191`, `InternalSecretValidator.java`, `docs/security.md §B3` | OK — à durcir (mTLS roadmap P3) |
| S04 | Gateway deny-by-default (plus de `permitAll /api/**`), 401 JSON unifié, E2E `16-gateway-security` | `GatewaySecurityConfig.java`, `16-gateway-security.cy.ts` (B3) | OK |
| S05 | RBAC `@PreAuthorize(hasAuthority(...))`, rôles SYSTEM_ADMIN / SUPPLIER_ADMIN/AGENT / SHOP_ADMIN/AGENT, `RoleGuard` + pages 403/404 | `docs/security.md §RBAC`, `app-routing.module.ts:63-64,108`, `role.guard.ts:34-35` (M4) | OK — vérifier résidu `SHOP_MANAGER` legacy |
| S06 | Register public naît DISABLED sans org, activation admin `PATCH /users/{id}/activate` | `RegisterUseCase.java:78-82`, `01-auth.cy.ts:109` (M5) | OK |
| S07 | Passwords BCrypt cost 12, refresh rotatif (hash SHA-256 seul, TTL 7j, `replaced_by`), révocation logout / changement password / désactivation, single-flight front | `PasswordConfig.java:13`, `RefreshTokenService.java`, `V6__refresh_tokens.sql`, `jwt.interceptor.ts` (M1) | OK |
| S08 | Rate-limit auth 10/min/IP + `Retry-After: 60` + `X-RateLimit-*`, bucket général 120/min | `RateLimitFilter.java`, `RateLimitFilterTest.java` (11e → 429), `15-rate-limit-auth.cy.ts` (B2) | OK |
| S09 | Idempotence paiements `Idempotency-Key` unique, double POST = 1 paiement, clé `asap-<orderId>` auto | `PaymentController.java:67-70`, `CreatePaymentUseCase.java:41-59` (B1) | OK |
| S10 | Pagination bornée (max 100, `{items,totalElements}`), agrégats SQL (plus de `MAX_VALUE`) | `ListPaymentsUseCase.java`, `17-pagination-aggregates.cy.ts` (B5) | OK |
| S11 | Validation DTO Bean Validation, erreurs sans stacktrace, logs sans secrets, correlation-id, headers nosniff/DENY/CSP base, CORS whitelist Angular | `GlobalExceptionHandler.java:21-85`, `docs/security.md §Sécurité applicative` | À durcir (CSP/HSTS complets → expert-devops) |
| S12 | Secrets par env, jamais committés ; Firebase/VAPID via `assets/env.js` + template | `env.template.js`, `push-notification.service.ts` (M7) | OK en code — **voir P0-01 ci-dessous pour le seed** |
| S13 | Soft-delete catalogue (`deletedAt` + filtre), audit écritures | `CatalogController.java:82,166`, `V10__catalog_soft_delete.sql` (M3) | OK |

**Légende :** OK = prouvé en code + test. À durcir = existe mais incomplet. Absent = finding §3.

---

## §2 — Assurance isolation inter-clients (garantie de non-fuite)

> Tenant = `organizationId` (JWT) décliné en `supplier_id` / `shop_id`. SYSTEM_ADMIN (`organizationId=NULL`) voit tout, c'est nominal. Tout autre rôle est scopé. Réf : `docs/MULTI_TENANCY_ARCHITECTURE.md`.

### 2.1 Matrice d'isolation (à compléter ligne-à-ligne par l'auditeur — extrait minimal exigible)

| Endpoint | Rôle requis | Check orgId ? | Test attaque ? | Verdict |
|---|---|---|---|---|
| `GET /api/orders/{id}` | SHOP/SUPPLIER (VIEW) | `OrderController` isSupplier/isShop `current.organizationId().equals(...)` — VÉRIFIER | E2E IDOR Shop A → Order Shop B : 403/404 exigé | [OK / FUITE — preuve] |
| `GET /api/payments/{id}` | VIEW_PAYMENTS | `canBeViewedBy()` — VÉRIFIER | IDOR Payment A↔B | [OK / FUITE] |
| `GET /api/payments/reference/{ref}` | VIEW_PAYMENTS | ⚠️ historiquement SANS check (`VALIDATION_REPORT.md CRITIQUE-05`) — **RE-VÉRIFIER OBLIGATOIRE** | IDOR par référence | [OK après fix / P0 ouvert] |
| `GET /api/stock`, `GET /api/catalog/**` | SUPPLIER | `StockController`, `CatalogController` `organizationId.equals` — VÉRIFIER | Supplier A → catalogue Supplier B | [OK / FUITE] |
| `GET /api/balance/**` | SHOP/SUPPLIER lié | `BalanceController` check paire `supplier_id+shop_id` — VÉRIFIER | Shop A → balance Shop B | [OK / FUITE] |
| `GET /api/users`, `GET /api/organizations` (listes) | ADMIN / scopé | filtré par org ? `findAll` nu INTERDIT — VÉRIFIER | énumération cross-clients | [OK / FUITE] |
| `GET /api/supplier-shop-relations` | lié | `SupplierShopRelationController` check — VÉRIFIER | paire A↔X vs B↔Y | [OK / FUITE] |
| `POST /api/internal/payments/auto` | interne | JWT gateway + `X-Internal-Token` exigés, rejet si rejoué depuis front — VÉRIFIER | replay token interne | [OK / FUITE] |

Règle : tout endpoint `@PreAuthorize("isAuthenticated()")` sans check orgId manuel = **FUITE P0 présumée** jusqu'à preuve du contraire. Tout garde `if (organizationId != null)` qui laisse passer null = **bypass P0**.

### 2.2 Scénarios d'attaque joués à blanc (résultat exigé : 403 ou 404, jamais 200 + données d'autrui)

| # | Scénario | Requête | Résultat attendu | Résultat observé | Verdict |
|---|---|---|---|---|---|
| A | Shop A lit Order du Shop B | `GET /orders/{idB}` token Shop A | 403/404 | [à remplir + preuve log/test] | [PASS / FAIL] |
| B | Supplier A modifie catalogue Supplier B | `PUT/DELETE /catalog/{idB}` token Supplier A | 403/404 | [à remplir] | [PASS / FAIL] |
| C | orgId null / forgé | JWT sans orgId ou body orgId d'autrui | 401/403 | [à remplir] | [PASS / FAIL] |
| D | Escalade SHOP_AGENT → action SUPPLIER_ADMIN | `POST` réservé supplier avec token shop_agent | 403 | [à remplir] | [PASS / FAIL] |
| E | Liste globale sans filtre | `GET /users`, `/orders` sans scope | borné à mon org / 403 | [à remplir] | [PASS / FAIL] |
| F | Replay `X-Internal-Token` depuis front | `POST /internal/...` avec token fuité | 401 | [à remplir] | [PASS / FAIL] |

### 2.3 Verdict isolation (cocher UNE case, rayer l'autre)

- [ ] **AUCUNE fuite inter-clients prouvée** sur le périmètre + commit ci-dessus, scénarios A–F tous PASS avec preuves (logs/tests E2E Cypress `cy.*idor*` + JUnit `*Security*Test*`). Le système **garantit** qu'un client ne voit/modifie jamais les données d'un autre (hors SYSTEM_ADMIN nominal).
- [x] **CERTIFICATION REFUSÉE / SOUS CONDITIONS — fuites ou doutes subsistants (état au 25/09/2026) :** `CRITIQUE-05 getByReference` à re-prouver + seed B4 + listes à re-balayer. **Aucune mise en prod** tant que A–F ne sont pas tous PASS.

---

## §3 — Remarques (findings priorisés)

| ID | Gravité | Titre — Preuve | Impact | Recommandation + ordre |
|---|---|---|---|---|
| P0-01 | P0 Bloquant | Seed backdoor `Admin@123` + 10 hashes BCrypt identiques, `SEED_ADMIN_PASSWORD` en dur — `V4__seed_users.sql:5`, `DataInitializer.java:38` (B4) | Backdoor connue si seed en prod | Interdire seed hors `local/dev` (profile-guard + CI grep), secret admin via env obligatoire, rotation. `→ expert-devops [P0] + expert-test [P0] test boot prod sans env → fail` |
| P0-02 | P0 Bloquant (à re-prouver) | `GET /payments/reference/{ref}` sans `canBeViewedBy` — `PaymentController.java:78-82` (`VALIDATION_REPORT.md CRITIQUE-05`) | Fuite inter-org par référence | Ajouter check orgId paire + test IDOR A↔B. `→ expert-dev [P0] — critère : Shop A GET ref Shop B → 403/404 + test vert` |
| P0-03 | P0 conditionnel | Toute liste `findAll()` sans `where organizationId` / tout `isAuthenticated()` sans check manuel | Énumération cross-clients | Balayage `grep findAll + PreAuthorize(isAuthenticated)` + tests IDOR. `→ expert-test [P0]` |
| P1-01 | P1 Majeur | JWT default `dev-only-secret...` si `JWT_SECRET` absent ; `INTERNAL_SECRET` dur en compose (historique CRITIQUE-02/04) | Forge JWT, usurpation inter-services | Fail-fast prod déjà en place — re-vérifier `application-prod.yml` sans fallback + CI `grep dev-only`. `→ expert-devops [P1]` |
| P1-02 | P1 Majeur | CORS `*` / actuator ouvert / CSP-HSTS incomplets possibles | Exfiltration, clickjacking | Whitelist Angular seule, actuator `health|info` seul, CSP+HSTS gateway. `→ expert-devops [P1]` |
| P2-01 | P2 Dette | `organizationId != null` permissif, `Sort/Pageable` non-borné, CSV sans échappement, erreurs verbeuses | Contournement, injection | Durcir gardes (`equals` strict, null → 403), allowlist sort, borne page 100. `→ expert-dev [P2]` |
| P3-01 | P3 Roadmap | Pentest E2E, Testcontainers sécu, `npm audit` / OWASP-DC, SAST Sonar, rotation secrets, mTLS | Assurance continue | Plan 30/60/90j. `→ expert-test + expert-devops [P3]` |

Dette reconduite (phase 2) : `statusLabel` ×15, formulaires sans messages, `ngx-translate` décoratif, `console.*`/`any`, `FRONTEND_ROUTES.md` vs 42 routes — hors sécu, rappelés pour mémoire.

---

## §4 — Décision de certification

> **DÉCISION : CERTIFICATION REFUSÉE EN PROD — ACCEPTÉ SOUS CONDITIONS (staging/démo uniquement).**
> Le socle §1 est solide (B1/B2/B3/B5, M1/M4/M5/M6/M7 soldés, preuves + E2E). Mais les P0-01/02/03 interdisent toute mise en production avec données réelles : risque avéré ou non-écarté de **passage d'informations d'un client à un autre** (IDOR référence, seed, listes).
> La certification PLATE (données réelles, multi-clients) ne sera prononcée qu'après : P0 soldés + matrice §2.1 complète + scénarios A–F tous PASS + tests d'attaque verts en CI.

Conditions de maintien (si certifié un jour) : aucun changement auth/RBAC/JWT/tenant sans ré-audit §2 ; secrets par vault/env ; logs sans PII ; revue sécu à chaque release mineure, ré-audit complet à chaque release majeure / 90 j.

---

## §5 — Ordres émis aux experts (format imposé)

- `[P0][expert-devops] Neutraliser seed B4 (profile-guard local/dev + CI fail si seed en prod) — critère : boot prod sans env → fail + test vert`
- `[P0][expert-dev] Corriger IDOR getByReference (canBeViewedBy paire supplier+shop) — critère : Shop A GET ref B → 403/404 + JUnit + Cypress verts`
- `[P0][expert-test] Balayer IDOR inter-clients (A–F : orders/payments/stock/catalog/balance/lists + orgId null + replay interne) — critère : suite attaque 100% PASS, 0 fuite`
- `[P1][expert-devops] Re-vérifier fail-fast JWT/INTERNAL en prod + CORS/actuator/CSP-HSTS — critère : grep 0 défaut + headers en staging`
- `[P3][expert-test + expert-devops] Roadmap : Testcontainers sécu, scans dépendances, Sonar SAST, pentest — critère : plan 30/60/90j chiffré`

---

## Annexes

- A. Périmètre fichiers audités (liste `glob` + commit) — [à joindre]
- B. Sorties tests : JUnit `*Security*Test`, `RateLimitFilterTest`, Cypress `15-rate-limit`, `16-gateway-security`, `17-pagination`, `12-api-integration`, futurs `*idor*` — [logs à joindre]
- C. Scans : `npm audit`, OWASP Dependency-Check, Sonar — [rapports à joindre]
- D. Historique : `AUDIT.md` (historique), `VALIDATION_REPORT.md` 07/09 (72/100 conditionnel, obsolète comme verdict), `COMPTE_RENDU_EXPERT.md` 24-25/09 (référence d'acceptation).

**Signatures :** Auditeur (`expert-securite`, date) / Responsable produit (nom, date, décision GO / NO-GO prod) — sans double signature, ce document reste un **rapport**, pas une certification.

*Fin — toute copie sans commit hash + preuves §2 est invalide.*
