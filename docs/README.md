# Index documentaire — Payment Platform

> Créé 25/09/2026. État **vérifié dans le code** (preuves `fichier:ligne`) pour les docs
> structurantes ; les autres sont classées **à jour / complément / historique-obsolète**.
> Règle : aucun `.md` supprimé — les redondances portent un bandeau « voir X ».
> Inventaire réel : **34 docs produit** (+ 2 consignes `.opencode/agents`, hors périmètre),
> pas 38 comme annoncé dans la mission.

## Canoniques (à jour 25/09/2026)

| Doc | But | État |
|---|---|---|
| [../README.md](../README.md) | Vision, stack, démarrage docker + IDE | À jour (4200 vs 8080, `.run/` local, env) |
| [../FRONTEND_ROUTES.md](../FRONTEND_ROUTES.md) | **41 URLs réelles** + guards + API paths | **Réécrit** depuis `app-routing.module.ts:58-108` |
| [../COMPTE_RENDU_EXPERT.md](../COMPTE_RENDU_EXPERT.md) | Acceptation B/M | **Corrigé** : M1/M3/M4/M5 au passé, B4 `[ ]` skip explicite |
| [api.md](api.md) | REST via gateway | À jour (`/refresh`, `/logout`, `/internal/payments/auto`, `?organizationId=`, enveloppes B5, summary SQL) |
| [security.md](security.md) | JWT/RBAC/rate-limit/secrets | À jour (B1/B2/B3/M1/M5) |
| [database.md](database.md) | Schéma MySQL | À jour (**Liquibase**, pas Flyway ; V3/B1, V6/M1, V10/M3) |
| [testing.md](testing.md) | Pyramide tests | À jour (Cypress 19 specs, Karma — plus de Jest/Playwright) |
| [LOCAL_BUILD_TEST_SONAR.md](LOCAL_BUILD_TEST_SONAR.md) | **Rebuild + tests + Sonar en local, pas à pas** | Procédure vérifiée (mvnw ciblé, Karma, Cypress, scan-monorepo) |
| [../payment-platform-ui/CYPRESS_E2E_GUIDE.md](../payment-platform-ui/CYPRESS_E2E_GUIDE.md) | Run E2E | À jour (specs 15-19, budget 1000, 246 `it(` statiques vs 244 verts annoncés) |
| [deployment.md](deployment.md) | Compose/CI/vars/PWA/push | À jour (`RATE_LIMIT_*`, `INTERNAL_SECRET`, `FIREBASE_*`+envsubst, `api-financial` 5m) |
| [../deploy/LOCAL_DEV.md](../deploy/LOCAL_DEV.md) | Run local détaillé | À jour (profil `local`, `.run/`, 4200/8081-8085, restart) |
| [ddd.md](ddd.md) | DDD court | Canonique |
| [business-rules.md](business-rules.md) | Règles métier | Canonique (+ statuts effectifs) |
| [events.md](events.md) | Outbox/événements | Canonique |
| [notifications.md](notifications.md) | Notifications/SSE/FCM | Canonique |

## Compléments (valables, non redondants)

| Doc | But | Note |
|---|---|---|
| [architecture.md](architecture.md) | Vue courte | Partiellement obsolète, renvoie ici |
| [bounded-contexts.md](bounded-contexts.md) | Résumé contextes | Résumé de `domain-model-complete.md` |
| [domain-model-complete.md](domain-model-complete.md) | Modèle exhaustif EN | Antérieur phases 0/1, compléter par compte-rendu |
| [MULTI_TENANCY_ARCHITECTURE.md](MULTI_TENANCY_ARCHITECTURE.md) | Isolation par `organizationId` | Schéma réel dans `database.md` |
| [TEST_COVERAGE_GUIDE.md](TEST_COVERAGE_GUIDE.md) | Coverage JaCoCo/Karma | Complément de `testing.md` |
| [../payment-platform-ui/CYpress_Component_Testing.md](../payment-platform-ui/CYpress_Component_Testing.md) | Component testing | **Incompatibilité documentée** Angular 16 + Cypress 15 (ne pas tenter) |
| [../payment-platform-ui/README.md](../payment-platform-ui/README.md) | Générique CLI | Complété par `LOCAL_DEV.md` |
| [../CHANGES.md](../CHANGES.md) | Changelog session 03/09 | Historique ponctuel |
| [../PRESENTATION_SCRIPT_FR.md](../PRESENTATION_SCRIPT_FR.md) | Script démo 4-5 min | Chiffres datés (100+ tests, Lighthouse 100 %) |

## Historiques — obsolètes comme plans/verdicts (bandeau posé, ne pas exécuter tels quels)

| Doc | Pourquoi obsolète | Voir |
|---|---|---|
| [COMPLETE_ARCHITECTURE.md](COMPLETE_ARCHITECTURE.md) | Passages datés (rate-limit 120/60, pagination `{content}`, refresh sans rotation, secret en dur) | `security.md`, `api.md`, `database.md` |
| [DDD_IMPROVEMENT_PLAN.md](DDD_IMPROVEMENT_PLAN.md) | Audit DDD pré-corrections | `COMPTE_RENDU_EXPERT.md`, `ddd.md` |
| [ANALYSE_FONCTIONNELLE_DDD.md](ANALYSE_FONCTIONNELLE_DDD.md) | Spec FR pré-phases (SHOP_MANAGER 16 UC) | `COMPTE_RENDU_EXPERT.md`, `FRONTEND_ROUTES.md` |
| [../VALIDATION_REPORT.md](../VALIDATION_REPORT.md) | Verdict 07/09 (72/100) pré-soldes | `COMPTE_RENDU_EXPERT.md` |
| [../TECHNICAL_IMPROVEMENT_PLAN.md](../TECHNICAL_IMPROVEMENT_PLAN.md) | Plan SEC pré-soldes | `COMPTE_RENDU_EXPERT.md` |
| [../AUDIT.md](../AUDIT.md) | Audit pré-phases (rate-limit permissif, SHOP_MANAGER) | `COMPTE_RENDU_EXPERT.md` |
| [../FONCTIONAL_SPECIFICATION.md](../FONCTIONAL_SPECIFICATION.md) | Backlog (refresh « manquant » alors que M1 fait) | `COMPTE_RENDU_EXPERT.md` §4-§5 |
| [../IMPROVEMENTS.md](../IMPROVEMENTS.md) | Backlog prompts pré-soldes | `COMPTE_RENDU_EXPERT.md` |
| [RENDER_CONFIG.md](RENDER_CONFIG.md) | Snapshot Render `Failed`/`ac72e61` | `deployment.md` |
| [sonar.md](sonar.md) | En-tête daté (65 tests, 54 specs, Angular 21) | `testing.md` |
| [../PLAN.md](../PLAN.md) | Plan i18n P3-02 **non exécuté** (dette phase 2) | trancher activer/supprimer `ngx-translate` |

## Recouvrements / contradictions relevées

- **Archi en triple** : `architecture.md` (72 l.) ⊂ `COMPLETE_ARCHITECTURE.md` (1991 l., EN) + `MULTI_TENANCY_ARCHITECTURE.md` (425 l.). → canonique court : README + `LOCAL_DEV.md` ; détail : COMPLETE (avec bandeau daté).
- **DDD en quintuple** : `ddd.md` + `bounded-contexts.md` + `domain-model-complete.md` + `ANALYSE_FONCTIONNELLE_DDD.md` + `DDD_IMPROVEMENT_PLAN.md`. → canonique : `ddd.md` + `business-rules.md`.
- **Audits en quadruple** : `VALIDATION_REPORT.md` vs `AUDIT.md` vs `TECHNICAL_IMPROVEMENT_PLAN.md` vs `COMPTE_RENDU_EXPERT.md`. → seul le dernier fait foi (25/09/2026).
- **Tests en triple** : `testing.md` (faux Jest/Playwright — corrigé) vs `TEST_COVERAGE_GUIDE.md` vs `CYPRESS_E2E_GUIDE.md` (faux 12 specs/183 tests — corrigé 19 specs).
- **Contradictions corrigées** : `database.md` disait Flyway → Liquibase ; `COMPLETE_ARCHITECTURE.md` rate-limit/pagination/refresh/secret ; `sonar.md` versions/chiffres ; `FRONTEND_ROUTES.md` 10 routes + rôle SALES fantôme + SHOP_MANAGER (purgé, `layout.component.ts:33-62` sans SHOP_MANAGER).

## Fusions proposées (sans exécution — l'orchestrateur décide)

1. `architecture.md` → section résumée de `COMPLETE_ARCHITECTURE.md` (ou suppression, bandeau posé en attendant).
2. `bounded-contexts.md` → §1 de `domain-model-complete.md` ; `ddd.md` reste la porte d'entrée courte.
3. `DDD_IMPROVEMENT_PLAN.md` + `ANALYSE_FONCTIONNELLE_DDD.md` → archivage (`docs/archive/`) après lecture du compte-rendu.
4. `VALIDATION_REPORT.md` + `AUDIT.md` + `TECHNICAL_IMPROVEMENT_PLAN.md` + `IMPROVEMENTS.md` + `FONCTIONAL_SPECIFICATION.md` (partie backlog) → un seul `docs/archive/audits-2026-09/` + le compte-rendu fait foi.
5. `TEST_COVERAGE_GUIDE.md` → § coverage de `testing.md`.
6. `RENDER_CONFIG.md` → annexe datée de `deployment.md` ou archive.
7. `PLAN.md` (i18n) → issue phase 2, pas un doc racine permanent.
