# Compte rendu d'expert — Payment Platform B2B

**Date :** 24/09/2026 — **Auditeur :** expert applicatif (mandat d'acceptation/refus)
**Périmètre :** `payment-platform-ui` (Angular 21) + `backend` (5 microservices Spring Boot + gateway)
**Méthode :** inspection code + preuves `fichier:ligne`, exécutions réelles (Karma, Cypress, tsc)

---

## 1. VERDICT : ACCEPTÉ SOUS CONDITIONS (pas de mise en production en l'état)

L'application est **fonctionnellement riche et démontrable**, avec une couverture de tests
unitaires exemplaire. Mais **5 points bloquants sécurité/intégrité** interdisent une mise en
production. Ils sont tous corrigeables en ~2 semaines (voir plan phase 0-1).

---

## 2. CE QUI VA (constaté, pas supposé)

| Domaine | Constat | Preuve |
|---|---|---|
| Tests unitaires | **849/849 SUCCESS** (Karma headless, 24/09) | run complet ce jour |
| Tests E2E | spec `05-supplier-products` : **8/8 passing** après corrections | run Cypress ce jour |
| Compilation | `tsc` app + `tsconfig` Cypress : **0 erreur** | vérifié ce jour |
| Auth backend | BCrypt cost 12, `@Valid` systématique, JWT HS256 ≥ 32 bytes, expiration 30 min | `PasswordConfig.java:13`, `JwtService.java:30-56` |
| Erreurs backend | `@RestControllerAdvice` global (400/401/403/404/409/422/500) | `GlobalExceptionHandler.java:21-85` |
| Event-driven | Outbox + relay + déduplication d'événements | `OutboxEventStore.java:34`, `EventDeduplicator.java:25` |
| Guards front | 42 routes, `AuthGuard` + `RoleGuard` sur 34/36 routes dashboard, JWT expiré détecté côté client | `app-routing.module.ts:56-105`, `auth.guard.ts:26-34` |
| UX listes | 83 % des écrans gèrent le chargement, 65 % l'état vide, toasts globaux | mesure sur 48 templates |
| Dark mode | Système de variables `--card-bg/--text-*` + filet global ; vérifié en navigateur (carte `#1a1a2e`, texte `#e0e0e0`) | `styles.css`, run Cypress dark ce jour |
| Temps réel | SSE notifications + polling + FCM push câblé bannière → `POST /api/fcm-tokens` → gateway | `notification.service.ts`, `notification-banner.component.ts`, `GatewayProxyController.java` |
| PWA | Service worker, manifest, bannière de mise à jour | `ngsw-config.json`, `pwa-update.component.ts:57-64` |

Corrections déjà appliquées pendant l'audit : tour d'onboarding qui bloquait les clics E2E,
`fetchNotifications` qui ignorait le format page `{items}` (panneau notif vide), banner push
sans appel API, route gateway `/fcm-tokens/**` manquante, `tsconfig` Cypress manquant
(`cy.` rouge dans IntelliJ), `experimentalSessionAndOrigin` obsolète, navigation parasite
ligne→détail paiements au clic Confirmer/Annuler.

---

## 3. CE QUI NE VA PAS

### 3.1 BLOQUANT production (phase 0, ~3-5 j)

| # | Défaut | Impact | Preuve |
|---|---|---|---|
| B1 | `Idempotency-Key` désormais **traité** : clé persistée + vérification en use-case - double POST identique = 1 seul paiement | Financier direct | `PaymentController.java:67-70`, `CreatePaymentUseCase.java:41-59`, `Payment.java`, `PaymentRepository.java` |
| B2 | Rate-limit **exclut** `login/register/refresh` : brute-force non freiné | Sécurité | `RateLimitFilter.java:44-47,67-73` |
| B3 | `"/api/**".permitAll()` au gateway : sécurité = un seul filtre JWT ; routes `internal/**` accessibles sans JWT, protégées par un secret **par défaut committé** (`dev-internal-secret-change-me`) | Sécurité | `GatewaySecurityConfig.java:44`, `GatewayProxyController.java:45`, `InternalOrganizationController.java:21`, `PaymentInternalSecretConfig.java:10` |
| B4 | Seed `Admin@123` + 10 hashes BCrypt identiques + `SEED_ADMIN_PASSWORD` en dur : exécution accidentelle en prod = backdoor connue | Sécurité | `V4__seed_users.sql:5`, `DataInitializer.java:38` |
| B5 | Listes non paginées (`findAll()` users/orgs) + `listPayments(..., Integer.MAX_VALUE)` : OOM/DoS | Disponibilité | `PaymentController.java:190`, `JpaUserRepository.java:77-78`, `JpaOrganizationRepository.java:32-33` |

### 3.2 MAJEUR (phase 1, ~1 sem)

| # | Défaut | Preuve |
|---|---|---|
| M1 | Pas de refresh token alors que `/api/auth/refresh` est déclaré public partout : JWT 30 min non renouvelable, pas de révocation | `AuthController.java:30-51` (absent), `JwtValidationFilter.java:104` |
| M2 | Validation inter-services en HTTP synchrone sans timeout/retry/circuit-breaker, parsing par `body.contains("\"SHOP\"")`, 409 métier confondu avec 503 infra, fenêtre TOCTOU avant `@Transactional` | `OrganizationValidationClient.java:24,58-61,68,113`, `CreatePaymentUseCase.java:42-44` |
| M3 | Hard-delete catégories/familles (`deleteById`) sans soft-delete/audit : casse l'historique | `CatalogController.java:72,146` |
| M4 | `SHOP_MANAGER` visible dans la nav mais refusé par `RoleGuard` (redirect silencieux vers `/dashboard`), pas de page 403/404 | `layout.component.ts:53-55` vs `app-routing.module.ts:53,95-99` |
| M5 | Register public : un inscrit `SUPPLIER_ADMIN` n'est rattaché à **aucune** organisation (`organizationId=null`) — auto-élévation à valider métier | `RegisterUseCase.java:55-58,70-72` |
| M6 | Cache PWA `freshness 24 h` sur `/api/**` : données financières périmées servies offline | `ngsw-config.json:43-54` |
| M7 | Clés Firebase/VAPID `YOUR_API_KEY` en dur : push HS en prod | `environment.ts:4-12`, `environment.prod.ts:4-12` |

### 3.3 MINEUR / dette (phase 2)

- `statusLabel` dupliqué **15× dans 14 fichiers** + `StatusLabelPipe` déclaré mais jamais utilisé ; `getTimeAgo` dupliqué 2× → extraire pipe/service unique.
- Formulaires 100 % template-driven, **0 message d'erreur par champ** (`invalid/touched` = 0 hit).
- `ngx-translate` **décoratif** : 0 usage de `| translate`, module jamais importé, switcher ni déclaré ni instancié → décider : activer FR/EN ou supprimer la dépendance.
- Mobile : variantes `hide-mobile/show-mobile` sur **6/48 écrans (12,5 %)** seulement.
- `console.*` en prod (9 hits), `any` abusifs (72 hits), 10 composants sans `unsubscribe`.
- Docs `FRONTEND_ROUTES.md` obsolètes (10 routes listées contre 42 réelles).

---

## 4. CE QU'IL FAUT AJOUTER (fonctionnel manquant)

1. **Paiements** : idempotence réelle (B1), rapprochement bancaire/export comptable, relances d'impayés.
2. **Commandes** : annulation partielle, avoirs, bons de livraison PDF (facture existe côté commande, pas le BL).
3. **Stock** : inventaire physique (comptage + écart), traçabilité n° de lot/DLC, import CSV catalogue.
4. **Pilotage** : dashboard dirigeant (CA, impayés, rotation stock), alertes seuils configurables par produit.
5. **Exploitation** : journal d'audit consultable côté métier (existe admin), purge/archivage, sauvegarde/restauration documentée, runbook + supervision (actuator exposé mais pas d'alerting).
6. **Comptes** : refresh + révocation tokens (M1), 2FA admin, politique mot de passe visible côté inscription (seul `minlength=8` au register).

---

## 5. PLAN D'ACTION CLAIR

### Phase 0 — Feu vert sécurité (3-5 j, BLOQUANT, critères : re-audit OK)
- [x] B1 : persister `Idempotency-Key` (contrainte d'unicité + table ou cache) et la transmettre au use-case. **Critère :** double POST identique = 1 seul paiement (test E2E).
- [ ] B2 : inclure `login/register` dans le rate-limit (compteur par IP, ex. 10/min) + délai progressif.
- [ ] B3 : supprimer `"/api/**".permitAll()`, auth par défaut ; `INTERNAL_SECRET` obligatoire au boot (échec si absent) ; unifier 401/403 inter-services.
- [ ] B4 : seed réservé au profil `dev/local` (garde `spring.profiles`), mot de passe admin initial généré et affiché une seule fois au premier boot.
- [ ] B5 : paginer `list users/orgs` (`Pageable`, max 100) ; remplacer `Integer.MAX_VALUE` par agrégats SQL (`SUM/COUNT`).

### Phase 1 — Robustesse & cohérence (1 sem)
- [ ] M1 : refresh-token (rotation + révocation, table ou Redis) ; front : intercepteur de refresh silencieux.
- [ ] M2 : timeouts + retry + circuit-breaker sur `OrganizationValidationClient`, parsing JSON typé, distinguer 409/503.
- [ ] M3 : soft-delete catalogue (`deletedAt` + filtre) + audit.
- [ ] M4 : aligner `SHOP_MANAGER` (nav ou rôles) + pages 403/404 dédiées.
- [ ] M5 : rattachement org obligatoire à l'inscription `SUPPLIER_ADMIN` (ou workflow de validation admin).
- [ ] M6 : PWA `networkFirst` sans cache > 5 min sur `/api/payments/**`, `/api/orders/**`.
- [ ] M7 : clés Firebase via variables d'environnement build, jamais committées.

### Phase 2 — Qualité & finition (2 sem, en parallèle du fonctionnel)
- [ ] Factoriser `statusLabel`/`getTimeAgo` (pipe + service partagés), supprimer `StatusLabelPipe` mort ou l'utiliser.
- [ ] trancher i18n : activer `TranslateModule` + `| translate` partout, ou supprimer `ngx-translate` + `assets/i18n`.
- [ ] Messages d'erreur par champ (reactive forms sur login/register/paiement), responsive cards sur les 10 écrans les plus utilisés (mesure analytics ou top E2E).
- [ ] Nettoyer `console.*`, typer les `any` critiques, `takeUntilDestroyed` systématique.
- [ ] Mettre à jour `FRONTEND_ROUTES.md` (42 routes) + runbook d'exploitation.

### Phase 3 — Fonctionnel à valeur (roadmap)
Inventaire physique → rapprochement bancaire → dashboard dirigeant → BL PDF → import CSV.

**Règle d'acceptation finale :** phase 0 soldée + E2E 14/14 verts + `sonar` 0 vulnérabilité bloquante + revue de ce plan en démo.
