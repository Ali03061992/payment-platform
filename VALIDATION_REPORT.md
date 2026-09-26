# RAPPORT DE VALIDATION - Payment Platform

> Audit historique du 2026-09-07 (72/100, PASS CONDITIONNEL) — **obsolète comme verdict** :
> les 5 critiques (gateway permitAll, INTERNAL_SECRET en dur, rate-limit absent, etc.) sont
> soldées en phases 0/1. Référence d'acceptation à jour :
> [COMPTE_RENDU_EXPERT.md](COMPTE_RENDU_EXPERT.md) (vérifié 25/09/2026). Index : [docs/README.md](docs/README.md).

**Date** : 2026-09-07  
**Audit format** : Revue de code complète (Backend Java/Spring Boot, Frontend Angular, Cypress E2E, Infrastructure Docker)  
**Auditeur** : opencode QA Agent

---

## 1. RÉSUMÉ EXÉCUTIF

| Critère | Verdict |
|---|---|
| **Note globale** | **72/100 — PASS CONDITIONNEL** |
| Critique (Critical) | 5 |
| Élevé (High) | 8 |
| Moyen (Medium) | 9 |
| Faible (Low) | 7 |

Le projet présente une **architecture globalement solide** (DDD/Clean Architecture, outbox pattern, audit trail, RBAC, déduplication d'événements). Cependant, **5 failles critiques de sécurité et d'architecture** doivent être corrigées avant toute mise en production. Le frontend souffre de fuites de mémoire et d'un manque de validation. Les tests E2E sont bien structurés mais couvrent mal les cas limites.

---

## 2. DÉTAIL DES CONSTATS

### 2.1 SÉCURITÉ — FAILLES CRITIQUES

#### [CRITIQUE-01] API Gateway : toutes les routes sont `permitAll()`
- **Fichier** : `backend/api-gateway/src/main/java/com/paymentplatform/gateway/GatewaySecurityConfig.java:32`
- **Problème** : `.anyRequest().permitAll()` signifie qu'**aucune** requête n'est protégée au niveau du gateway. L'authentification est repoussée sur les microservices individuels. Un attaquant peut contourner le gateway et appeler directement les services internes, ou exploiter des routes non sécurisées.
- **Impact** : Contournement d'authentification, accès non autorisé aux APIs internes.
- **Correction** : Implémenter la validation JWT au niveau du gateway (comme le fait déjà `SharedSecurityConfig` dans les microservices), ou utiliser Spring Cloud Gateway avec un filtre de sécurité.

#### [CRITIQUE-02] Secret interne (`INTERNAL_SECRET`) hardcodé dans `docker-compose.yml`
- **Fichier** : `deploy/docker-compose.yml:72,105,132`
- **Problème** : `INTERNAL_SECRET: dev-internal-secret-change-me` est en dur dans le fichier docker-compose. Ce secret protège les appels inter-services (`X-Internal-Token`).
- **Impact** : Si le conteneur est exposé, le secret est visible. En production, un attaquant peut usurper des appels internes.
- **Correction** : Utiliser les secrets Docker (`secrets:`) ou des vaults. Ne jamais hardcoder de secrets.

#### [CRITIQUE-03] Mot de passe par défaut du compte admin dans `DataInitializer`
- **Fichier** : `backend/identity-service/src/main/java/com/.../DataInitializer.java:38`
- **Problème** : `SEED_ADMIN_PASSWORD` vaut `Admin@123` par défaut. Ce mot de passe faible est aussi utilisé dans les scripts SQL d'initialisation (`deploy/init_users.sql`).
- **Impact** : Compte SYSTEM_ADMIN accessible avec des identifiants connus.
- **Correction** : Supprimer le mot de passe par défaut ; forcer la configuration via variables d'environnement obligatoires.

#### [CRITIQUE-04] JWT secret par défaut identique dans tous les services
- **Fichier** : Tous les `application.yml` (identity:41, payment:32, org:30, notification:30, gateway:10)
- **Problème** : `dev-only-secret-change-me-0123456789abcdef0123456789abcdef` est la valeur par défaut. Si la variable `JWT_SECRET` n'est pas définie en production, **tous les tokens sont signés avec le même secret public**.
- **Impact** : Forgery de tokens JWT, usurpation d'identité totale.
- **Correction** : Lever une erreur au démarrage si le secret est la valeur par défaut en profile `prod`.

#### [CRITIQUE-05] `PaymentController.getByReference` ne vérifie pas l'autorisation
- **Fichier** : `backend/payment-service/src/main/java/com/.../PaymentController.java:78-82`
- **Problème** : L'endpoint `GET /api/payments/reference/{reference}` retourne le paiement sans vérifier que l'utilisateur authentifié a le droit de le voir. Contrairement à `getById` qui vérifie l'organisation, `getByReference` expose tous les paiements.
- **Impact** : Fuite de données inter-organisations.
- **Correction** : Ajouter la vérification `canBeViewedBy()` comme dans `getById`.

---

### 2.2 SÉCURITÉ — FAILLES ÉLEVÉES

#### [ÉLEVÉ-01] Elasticsearch sans sécurité
- **Fichier** : `deploy/docker-compose.yml:42`
- **Problème** : `xpack.security.enabled=false` — Elasticsearch accessible sans authentification sur le port 9200.
- **Impact** : Lecture/modification/suppression des index de paiements.
- **Correction** : Activer la sécurité Elasticsearch avec authentification basic.

#### [ÉLEVÉ-02] CORS wildcard `"*"` en production
- **Fichier** : `deploy/docker-compose.yml:205`
- **Problème** : `CORS_ALLOWED_ORIGINS: "*"` accepte les requêtes depuis n'importe quel domaine.
- **Impact** : Attaques CSRF depuis des sites malveillants.
- **Correction** : Restreindre aux domaines frontend autorisés.

#### [ÉLEVÉ-03] Absence de rate limiting au niveau API Gateway
- **Fichier** : `deploy/docker-compose.yml:206` — la config existe (`RATE_LIMIT_PER_MINUTE: 120`) mais **aucune implémentation** n'est visible dans `GatewaySecurityConfig` ou `GatewayProxyController`.
- **Impact** : Brute-force sur les endpoints de login, déni de service.
- **Correction** : Implémenter le rate limiting (Bucket4j, Resilience4j).

#### [ÉLEVÉ-04] `AuthGuard` Angular ne valide pas l'expiration du JWT
- **Fichier** : `payment-platform-ui/src/app/core/auth.guard.ts:10-11`
- **Problème** : Vérifie uniquement si le token existe dans localStorage, pas s'il est expiré.
- **Impact** : Un utilisateur avec un token expiré reste connecté jusqu'à la prochaine requête 401.
- **Correction** : Décoder le JWT et vérifier la claim `exp`.

#### [ÉLEVÉ-05] `RoleGuard` Angular sans `try-catch` autour de `JSON.parse`
- **Fichier** : `payment-platform-ui/src/app/core/role.guard.ts:22`
- **Problème** : Si `localStorage` contient du JSON corrompu, `JSON.parse(userJson)` lance une exception non rattrapée, crashant l'application.
- **Impact** : Crash de l'application Angular, déni de service utilisateur.
- **Correction** : Ajouter un try-catch avec redirection vers `/login`.

#### [ÉLEVÉ-06] `CatalogController` retourne des entités JPA directement
- **Fichier** : `backend/organization-service/src/main/java/com/.../CatalogController.java:42,62,115`
- **Problème** : Les endpoints retournent `ProductCategory` et `ProductFamily` (entités JPA avec annotations `@Entity`) directement dans la réponse HTTP, sans mapper vers des DTOs.
- **Impact** : Fuite de la structure interne de la BDD, risque de sérialisation infinie, couplage fort.
- **Correction** : Créer des DTOs de réponse et mapper les entités.

#### [ÉLEVÉ-07] `Order` est à la fois entité JPA et agrégat domaine
- **Fichier** : `backend/organization-service/src/main/java/com/.../Order.java:10-12`
- **Problème** : La classe `Order` porte à la fois les annotations JPA (`@Entity`, `@Table`, `@Column`, `@Version`) et la logique métier domaine (`create()`, `confirm()`, `reject()`, `recalculateTotals()`). Cela viole la séparation DDD domain/infrastructure.
- **Impact** : Couplage domaine-persistence, impossible de tester le domaine sans JPA, difficulté à changer de persistence store.
- **Correction** : Séparer en `Order` (domain) et `OrderJpaEntity` (infrastructure), avec un mapper.

#### [ÉLEVÉ-08] `OrganizationValidationClient` crée `HttpClient` et `ObjectMapper` inline
- **Fichier** : `backend/payment-service/src/main/java/com/.../OrganizationValidationClient.java:23-24`
- **Problème** : `HttpClient.newHttpClient()` et `new ObjectMapper()` ne sont pas gérés par Spring. Pas de pool de connexions partagé, pas de configuration Jackson cohérente.
- **Impact** : Fuite de connexions, sérialisation incohérente avec le reste de l'app.
- **Correction** : Injecter `HttpClient` (bean Spring) et `ObjectMapper` (bean existant `JacksonConfig`).

---

### 2.3 ARCHITECTURE (DDD / CLEAN ARCHITECTURE)

#### [MÉDIUM-01] `CatalogController` et `StockController` contournent la couche Use Case
- **Fichiers** : `CatalogController.java`, `StockController.java`
- **Problème** : Ces contrôleurs injectent directement des repositories (`ProductCategoryRepository`, `ProductFamilyRepository`) et effectuent la logique métier dans le contrôleur.
- **Correction** : Créer des use cases (`CreateCategoryUseCase`, etc.) pour isoler la logique métier.

#### [MÉDIUM-02] `OrderController` injecte des repositories directement
- **Fichier** : `OrderController.java:28-30`
- **Problème** : `orderRepository` et `orderItemRepository` sont injectés directement dans le contrôleur. Le filtrage par rôle et organisation est fait dans le contrôleur (lignes 66-80).
- **Correction** : Déplacer la logique de filtrage dans un use case.

#### [MÉDIUM-03] `OrderController.assignDeliveryAgent` et `deliver` utilisent `Map<String, Long>` sans validation
- **Fichiers** : `OrderController.java:133,146`
- **Problème** : `@RequestBody Map<String, Long> body` — pas de validation des entrées. Un body vide ou avec des clés manquantes provoquera un NPE.
- **Correction** : Créer un DTO avec validation `@NotNull`.

#### [MÉDIUM-04] Absence de pattern Circuit Breaker pour les appels inter-services
- **Fichiers** : `OrganizationValidationClient.java`, `PaymentNameResolver.java`
- **Problème** : Les appels HTTP synchrones vers les autres microservices n'ont aucun mécanisme de fallback ou circuit breaker.
- **Impact** : Si un service est down, les appels bloquent (timeout par défaut) ou échouent silencieusement.
- **Correction** : Utiliser Resilience4j (CircuitBreaker, Retry, TimeLimiter).

#### [MÉDIUM-05] Code dupliqué dans `OrganizationValidationClient`
- **Fichier** : `OrganizationValidationClient.java:44-71` vs `74-101`
- **Problème** : `validateShop` et `validateSupplier` sont quasi identiques (seul le message d'erreur change).
- **Correction** : Extraire une méthode privée générique.

---

### 2.4 FRONTEND ANGULAR

#### [ÉLEVÉ-09] Fuite de mémoire dans `NotificationService` — interval non stocké
- **Fichier** : `payment-platform-ui/src/app/services/notification.service.ts:27-29`
- **Problème** : Le deuxième `interval(intervalMs)` pour `fetchUnreadCount` n'est **pas assigné** à une variable. `stopPolling()` ne peut pas le désabonner. Fuite de mémoire à chaque navigation.
- **Correction** : Stocker les deux subscriptions et les désabonner dans `stopPolling()`.

#### [MÉDIUM-06] Composants Angular sans `OnDestroy` ni désabonnement
- **Fichiers** : `DashboardComponent`, `PaymentListComponent`, `PaymentDetailComponent`, `UserManagementComponent`, `OrderListComponent`, `StockManagementComponent`
- **Problème** : Ces composants s'abonnent à des Observables dans `ngOnInit` mais n'implémentent pas `OnDestroy` et ne désabonnent pas les subscriptions.
- **Impact** : Fuites de mémoire si le composant est détruit avant la complétion de l'observable (navigation rapide).
- **Correction** : Utiliser `takeUntil` / `DestroyRef` ou implémenter `OnDestroy`.

#### [MÉDIUM-07] `user: any` dans `LayoutComponent`
- **Fichier** : `payment-platform-ui/src/app/layout/layout.component.ts:14`
- **Problème** : `user: any` — aucune vérification de type, possible runtime error si la structure change.
- **Correction** : Utiliser le modèle `User` défini dans `models/user.model.ts`.

#### [MÉDIUM-08] `LoginComponent` imbrique deux `subscribe` sans gestion d'erreur robuste
- **Fichier** : `payment-platform-ui/src/app/login/login.component.ts:25-46`
- **Problème** : Le premier `subscribe` appelle `getMe()` dans le callback `next`. Si l'utilisateur navigue pendant le premier appel, le deuxième subscribe tourne dans le vide. Pas de `unsubscribe` ni `takeUntil`.
- **Correction** : Utiliser `switchMap` pour enchaîner les deux requêtes de manière propre.

#### [MÉDIUM-09] Le token est stocké deux fois (localStorage dans `LoginService.login` et `LoginComponent`)
- **Fichiers** : `login.service.ts:15`, `login.component.ts:27`
- **Problème** : `LoginService.login()` fait `localStorage.setItem('token', res.accessToken)` dans un `tap()`, puis `LoginComponent.onSubmit()` fait `localStorage.setItem('token', res.accessToken)` à nouveau. Redondant.
- **Correction** : Supprimer le `tap()` dans `LoginService.login` ou le set dans le composant.

---

### 2.5 TESTS (CYPRESS E2E)

#### Points forts :
- **13 fichiers de tests** couvrant auth, admin, RBAC, paiements, commandes, stock, notifications, navigation
- **Bonnes pratiques** : custom commands réutilisables (`cy.login()`, `cy.loginAsAdmin()`), fixtures centralisées, `ensureTestUsers` pour setup
- **Tests de sécurité RBAC** : vérification des restrictions cross-rôle (supplier ne peut pas accéder admin pages)
- **Tests de flux de paiement** : création, confirmation, rejet, annulation avec vérification des droits
- **Tests d'états terminaux** : vérification qu'on ne peut pas annuler un paiement déjà confirmé

#### Lacunes identifiées :

| Catégorie | Ce qui manque |
|---|---|
| **Cas limites paiements** | Montant négatif, montant=0, shop=supplier, devise invalide, montant très élevé |
| **Concurrence** | Tests de race condition (deux users confirment le même paiement) |
| **Timeout/erreurs réseau** | Aucun test de comportement en cas d'indisponibilité backend |
| **Pagination** | Aucun test avec beaucoup de données (>100 éléments) |
| **XSS/Injection** | Aucun test de输入 avec du HTML/JS dans les champs |
| **Notifications** | Testés uniquement pour le polling, pas pour la création/réception réelle |
| **Auth token expiré** | Aucun test de refresh ou de redirection sur token expiré |
| **Edge cases formulaires** | SQL injection, caractères spéciaux, strings vides avec espaces |
| **Tests de performance** | Aucun test de charge ou de temps de réponse |

#### Observations sur la fiabilité :
- **Aucun retry** configuré (`retries: { runMode: 0, openMode: 0 }`) — les tests échouent au premier essai, ce qui est bon pour détecter les flaky tests.
- **Temps de timeout** correctement configurés (10s command, 30s response).
- Les tests créent des données via API puis vérifient le UI — bonne pratique.

---

### 2.6 INFRASTRUCTURE

#### Points forts :
- Docker multi-stage build (build + runtime léger)
- Health checks sur MySQL, RabbitMQ, Elasticsearch
- Nginx avec headers de sécurité (X-Content-Type-Options, X-Frame-Options, etc.)
- Réseau isolé `payment-net`
- Flyway pour les migrations de BDD
- `ddl-auto: validate` (pas de modification auto du schéma)
- `open-in-view: false` (bonne pratique JPA)

#### Constats négatifs :

| Problème | Fichier | Ligne |
|---|---|---|
| MySQL root password en env | `docker-compose.yml` | 6 |
| Pas de backup strategy pour MySQL | `docker-compose.yml` | - |
| `mvn -pl ${SERVICE_NAME} -am package -DskipTests` dans le Dockerfile | `Dockerfile` | 16 |
| Pas de `.dockerignore` pour le frontend | `frontend/` | - |
| Pas de resource limits (CPU/memory) dans docker-compose | `docker-compose.yml` | - |
| `xpack.security.enabled=false` pour Elasticsearch | `docker-compose.yml` | 42 |

---

### 2.7 CONCOURS TRANSVERSAUX

#### Logging
- **Constat** : Logging cohérent avec SLF4J, messages en français, levels appropriés (error pour échecs, warn pour dégradations, debug pour idempotence). Bonne pratique.

#### Gestion des exceptions
- **Constat** : `GlobalExceptionHandler` complet avec mapping en `ApiError` uniforme. Gère les exceptions métier, validation, optimistic locking, et les erreurs inattendues. **Bon**.

#### Transaction management
- **Constat** : `@Transactional` utilisé correctement sur les use cases. `OutboxEventStore.append()` est dans la même transaction que l'agrégat — pattern outbox correct. **Bon**.

#### Fiabilité des événements
- **Constat** : Pattern outbox + relay + déduplication (`EventDeduplicator`). Les événements ratés sont réessayés au prochain tick. Les consommateurs sont idempotents. **Bon**.

#### Audit trail
- **Constat** : `AuditRecorder` utilisé pour les actions critiques (login, création, confirmation, rejet). Correlation ID filter présent. **Bon**.

---

## 3. RECOMMANDATIONS PRIORITAIRES

### Priorité 1 (Avant mise en production) — CRITIQUE

1. **Sécuriser le Gateway** : Implémenter la validation JWT au niveau `GatewaySecurityConfig` (comme `SharedSecurityConfig`)
2. **Externaliser les secrets** : Utiliser Docker secrets ou un vault pour `JWT_SECRET`, `INTERNAL_SECRET`, `MYSQL_PASSWORD`
3. **Forcer le changement de secrets** : Lancer une erreur si les secrets par défaut sont utilisés en profile `prod`
4. **Corriger `getByReference`** : Ajouter la vérification d'autorisation organisation
5. **Activer le rate limiting** sur le Gateway

### Priorité 2 (1-2 sprints)

6. **Séparer les entités JPA du domaine** : Extraire `OrderJpaEntity` et mapper vers `Order`
7. **Créer des DTOs** pour `CatalogController` et les endpoints retournant des entités
8. **Déplacer la logique métier** des contrôleurs vers des use cases
9. **Ajouter un Circuit Breaker** (Resilience4j) pour les appels inter-services
10. **Corriger les fuites mémoire** Angular (NotificationService, subscriptions)

### Priorité 3 (Amélioration continue)

11. Ajouter des tests E2E pour les cas limites (montants invalides, XSS, timeout)
12. Ajouter `OnDestroy` + désabonnement sur tous les composants Angular
13. Améliorer le typage Angular (éliminer les `any`)
14. Ajouter des tests unitaires pour `OrganizationService` et `PaymentService` (couverture actuelle très faible)
15. Ajouter des resource limits dans docker-compose

---

## 4. ÉVALUATION GLOBALE

| Domaine | Score | Commentaire |
|---|---|---|
| Architecture DDD | **8/10** | Bonne structure domain/application/infrastructure, mais quelques fuites dans les contrôleurs et le modèle Order |
| Sécurité | **5/10** | Failles critiques au gateway, secrets hardcodés, autorisation incomplète |
| Code Backend | **7/10** | Propre, bien structuré, patterns corrects (outbox, audit, déduplication) |
| Code Frontend | **6/10** | Fonctionnel mais fuites mémoire, manque de type safety, login nesting |
| Tests E2E | **7/10** | Bonne couverture fonctionnelle, mais cas limites manquants |
| Infrastructure | **7/10** | Docker bien configuré, mais sécurité Elasticsearch et secrets à améliorer |
| Logging/Observabilité | **8/10** | Correlation ID, audit trail, messages structurés |

### Verdict final : **PASS CONDITIONNEL**

Le codebase est **fonctionnellement complet** et présente des bonnes pratiques architecturales. Les failles critiques de sécurité doivent être traitées avant la mise en production. Une passe de correction sur les points Critiques + Élevés (estimée à 2-3 sprints) porterait le score à **85+/100**.
