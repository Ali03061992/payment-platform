# Plan d'Ameliorations Techniques — Payment Platform

> Document genere a partir de l'analyse complete du code source et de la specification fonctionnelle.
> Concentre uniquement sur les problemes REELS identifies dans le code, avec des correctifs CONCRETS.

---

## Table des Matieres

1. [Problemes Critiques de Securite](#1-problemes-critiques-de-securite)
2. [Failles d'Autorisation](#2-failles-dautorisation)
3. [Problemes de Performance et Scalabilite](#3-problemes-de-performance-et-scalabilite)
4. [Defauts Architecturaux](#4-defauts-architecturaux)
5. [Fonctionnalites Manquantes par Rapport a la Spec](#5-fonctionnalites-manquantes-par-rapport-a-la-spec)
6. [Qualite du Code](#6-qualite-du-code)
7. [Frontend et UX](#7-frontend-et-ux)
8. [DevOps et Fiabilite](#8-devops-et-fiabilite)

---

## 1. Problemes Critiques de Securite

### SEC-01 : Le Gateway ne valide PAS les JWT — Contournement de la securite

**Probleme :** `GatewaySecurityConfig.java:32` definit `.anyRequest().permitAll()`. Le Gateway laisse passer TOUTES les requetes sans verification d'authentification. Bien que les services backend aient leur propre filtre JWT, le Gateway est le point d'entre unique : un attaquant peut envoyer des requetes directement aux microservices en contourant le Gateway si les ports sont accessibles.

**Fichier :** `backend/api-gateway/src/main/java/com/paymentplatform/gateway/GatewaySecurityConfig.java:32`

**Fix concret :**
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/api/auth/login",
        "/api/auth/register",
        "/api/auth/refresh",
        "/api/auth/password-setup/**",
        "/actuator/health",
        "/actuator/info"
    ).permitAll()
    .anyRequest().authenticated())
```
Ajouter le filtre JWT au Gateway SecurityFilterChain, ou utiliser Spring Cloud Gateway avec `GlobalFilter` pour valider le token avant le proxy.

**Priorite :** Haute
**Effort :** 4h

---

### SEC-02 : Secrets JWT avec valeurs par defaut faibles

**Probleme :** Tous les `application.yml` definissent `dev-only-secret-change-me-0123456789abcdef0123456789abcdef` comme secret par defaut. Si la variable d'environnement n'est pas positionnee, le systeme demarre avec un secret previsible. De meme, `docker-compose.yml:72,105,132` contient `INTERNAL_SECRET: dev-internal-secret-change-me` en dur.

**Fichiers :**
- `backend/payment-service/src/main/resources/application.yml:32`
- `backend/identity-service/src/main/resources/application.yml:41`
- `backend/organization-service/src/main/resources/application.yml:30`
- `deploy/docker-compose.yml:72,105,132`

**Fix concret :**
- Supprimer les valeurs par defaut des secrets dans les `application.yml`. Forcer le demarrage si la variable d'environnement n'est pas definie.
- Extraire `INTERNAL_SECRET` dans le fichier `.env` au lieu de le hardcoder dans `docker-compose.yml`.
- Ajouter un demarrage conditionnel :
```java
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
    @NotBlank String secret,
    Duration expiration
) {}
```

**Priorite :** Haute
**Effort :** 2h

---

### SEC-03 : Absence de Rate Limiting

**Probleme :** Aucune limitation du taux de requetes n'est implementee. Le spec (S1) impose un rate limiting different par role.

**Fix concret :**
- Ajouter `Bucket4j` ou un filtre Spring personnalise dans `GatewayProxyController` ou `GatewaySecurityConfig`.
- Stocker les compteurs dans Redis (a terme) ou en memoire ( ConcurrentHashMap avec TTL).
- Exemple de configuration :
```
SYSTEM_ADMIN: 120 req/min
SUPPLIER_ADMIN: 60 req/min
SHOP_ADMIN: 30 req/min
SHOP_AGENT: 10 req/min
```

**Priorite :** Haute
**Effort :** 8h

---

### SEC-04 : Pas de Password Policy

**Probleme :** Aucune verification de la complexite du mot de passe lors de l'inscription ou de la configuration du mot de passe.

**Fichier :** `RegisterUseCase` et `PasswordSetupUseCase`

**Fix concret :**
Ajouter un validateur de mot de passe :
```java
public class PasswordPolicyValidator {
    public static void validate(String password) {
        if (password.length() < 8)
            throw new ConflictException("Le mot de passe doit contenir au moins 8 caracteres");
        if (!password.matches(".*[A-Z].*"))
            throw new ConflictException("Le mot de passe doit contenir au moins une majuscule");
        if (!password.matches(".*[0-9].*"))
            throw new ConflictException("Le mot de passe doit contenir au moins un chiffre");
        if (!password.matches(".*[!@#$%^&*()].*"))
            throw new ConflictException("Le mot de passe doit contenir au moins un caractere special");
    }
}
```
Appeler cette methode dans `RegisterUseCase` et `PasswordSetupUseCase`.

**Priorite :** Moyenne
**Effort :** 3h

---

### SEC-05 : Pas de Refresh Token

**Probleme :** Le JWT expire apres 30 minutes sans mecanisme de rafraichissement. L'utilisateur est deconnecte brutalement. Le spec (S2) demande un Refresh Token.

**Fichier :** `backend/shared-lib/src/main/java/com/paymentplatform/shared/infrastructure/security/JwtService.java`

**Fix concret :**
- Ajouter un endpoint `POST /api/auth/refresh` qui accepte un refresh token et genere un nouveau JWT.
- Stocker les refresh tokens dans une table `refresh_tokens` (user_id, token_hash, expires_at, revoked).
- Cote frontend, intercepter le 401 et tenter un refresh avant de rediriger vers `/login`.

**Priorite :** Haute
**Effort :** 12h

---

### SEC-06 : Stockage du token JWT dans localStorage (XSS)

**Probleme :** `login.service.ts:15` stocke le token dans `localStorage`, qui est accessible par tout script JavaScript injecte (XSS).

**Fichier :** `payment-platform-ui/src/app/services/login.service.ts:15`

**Fix concret :**
- Utiliser un cookie `HttpOnly` + `Secure` + `SameSite=Strict` pour le token.
- Ou utiliser le pattern `BFF (Backend For Frontend)` ou le token est stocke dans une session cote serveur.
- A minima, ajouter `httpOnly` cookies depuis le backend.

**Priorite :** Moyenne
**Effort :** 6h

---

## 2. Failles d'Autorisation

### AUTH-01 : PaymentController.getByReference() sans verification d'organisation

**Probleme :** `PaymentController.java:78-82` — L'endpoint `GET /api/payments/reference/{reference}` retourne un paiement sans aucune verification que l'utilisateur connecte a le droit de voir ce paiement. N'importe quel utilisateur authentifie peut consulter n'importe quel paiement.

**Fichier :** `backend/payment-service/src/main/java/com/paymentplatform/payment/interfaces/rest/PaymentController.java:78-82`

**Fix concret :**
```java
@GetMapping("/reference/{reference}")
@PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
public ResponseEntity<PaymentResponse> getByReference(@PathVariable String reference) {
    var current = CurrentUser.get();
    var response = getPayment.execute(reference);
    if (!current.roles().contains("SYSTEM_ADMIN")) {
        Long orgId = current.organizationId();
        if (orgId == null) return ResponseEntity.status(403).build();
        boolean isShop = response.shopId() == orgId;
        boolean isSupplier = response.supplierId() == orgId;
        if (!isShop && !isSupplier) return ResponseEntity.status(403).build();
    }
    return ResponseEntity.ok(response);
}
```

**Priorite :** Haute
**Effort :** 1h

---

### AUTH-02 : NotificationController.markAsRead() sans verification de propriete

**Probleme :** `NotificationController.java:61-67` — L'endpoint `POST /api/notifications/{id}/read` permet a n'importe quel utilisateur authentifie de marquer n'importe quelle notification comme lue.

**Fichier :** `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/rest/NotificationController.java:61-67`

**Fix concret :**
```java
@PostMapping("/{id}/read")
public ResponseEntity<Void> markAsRead(@PathVariable Long id,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
    Notification n = notifications.findById(id).orElseThrow(() ->
            new java.util.NoSuchElementException("Notification not found"));
    // Verifier que la notification appartient a l'utilisateur
    if (user.organizationId() == null || !user.organizationId().equals(n.recipientOrganizationId())) {
        if (!user.userId().equals(n.recipientUserId())) {
            throw new ForbiddenException("Acces non autorise");
        }
    }
    n.markAsRead();
    notifications.save(n);
    return ResponseEntity.noContent().build();
}
```

**Priorite :** Haute
**Effort :** 2h

---

### AUTH-03 : Gestion des roles hardcodes en chaines de caracteres

**Probleme :** `OrderController.java:57` — `current.roles().contains("SUPPLIER_ADMIN")` utilise des chaines de caracteres literal au lieu de l'enum `RoleCode`. Meme chose dans `PaymentController.java:89` : `role.contains("SHOP")`.

**Fichiers :**
- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/OrderController.java:57`
- `backend/payment-service/src/main/java/com/paymentplatform/payment/interfaces/rest/PaymentController.java:89-91`

**Fix concret :** Utiliser `RoleCode` enum dans tout le code et desactiver `USER_LOGIN` dans `AuditActions` pour les actions critiques.

**Priorite :** Moyenne
**Effort :** 4h

---

### AUTH-04 : SYSTEM_ADMIN ne peut pas voir l'audit

**Probleme :** `AuditActions.java` definit les constantes d'audit mais il n'existe aucun endpoint pour consulter les logs d'audit. Le spec (S4) exige un audit trail complet et consultable.

**Fichier :** `backend/shared-lib/src/main/java/com/paymentplatform/shared/infrastructure/audit/AuditActions.java`

**Fix concret :**
- Creer un endpoint `GET /api/admin/audit-logs` dans l'organization-service ou le shared-lib.
- Filtrable par date, utilisateur, organisation, type d'action.
- Restreint a `ADMIN_VIEW_AUDIT`.

**Priorite :** Moyenne
**Effort :** 8h

---

## 3. Problemes de Performance et Scalabilite

### PERF-01 : N+1 appels HTTP pour la resolution des noms

**Probleme :** `ListPaymentsUseCase.java:48-83` — Pour chaque paiement, le code fait un appel HTTP individuel a l'organization-service ET a l'identity-service pour resoudre les noms. Pour 100 paiements, cela genere 200+ appels HTTP synchrones.

**Fichier :** `backend/payment-service/src/main/java/com/paymentplatform/payment/application/usecase/ListPaymentsUseCase.java:48-83`

**Fix concret :**
- Batch les appels : collecter tous les IDs d'organisations et d'utilisateurs, puis faire 2 appels HTTP au maximum avec des listes d'IDs.
- Ou mieux : implementer un cache Redis pour les noms d'organisations/utilisateurs avec TTL de 5 minutes.
- Ou copier les noms directement dans l'entite Payment au moment de la creation (denormalisation).

**Priorite :** Haute
**Effort :** 6h

---

### PERF-02 : Pas de pagination reelle

**Probleme :**
- `ListPaymentsUseCase.java:43-45` : `executeAll()` charge TOUS les paiements sans pagination.
- `OrderController.java:64-89` : `listOrders()` charge toutes les commandes sans pagination.
- `StockController.java:82-91` : `listMovements()` charge tous les mouvements.

**Fichiers :**
- `backend/payment-service/src/main/java/com/paymentplatform/payment/application/usecase/ListPaymentsUseCase.java:43-45`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/OrderController.java:64-89`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/StockController.java:82-91`

**Fix concret :**
- Remplacer les methodes `findAll()` par des methodes avec `Pageable` Spring Data.
- Retourner des reponses `Page<T>` ou `PageResponse<T>` contenant `data`, `totalElements`, `totalPages`, `page`, `size`.
- Cote frontend, implementer la pagination avec des boutons precedent/suivant.

**Priorite :** Haute
**Effort :** 12h

---

### PERF-03 : OrganizationValidationClient fait des appels HTTP bloquants synchrones

**Probleme :** `OrganizationValidationClient.java` utilise `java.net.http.HttpClient` pour des appels HTTP synchrones dans des transactions. Ces appels bloquent le thread du pool de connexion pendant l'attente de la reponse. Si le service cible est lent, le thread est bloque.

**Fichier :** `backend/payment-service/src/main/java/com/paymentplatform/payment/infrastructure/http/OrganizationValidationClient.java`

**Fix concret :**
- Utiliser `WebClient` de Spring WebFlux pour des appels non-bloquants.
- Ou mieux : valider les organisations via des appels directs en base (via Feign Client avec circuit breaker) plutot que par HTTP.
- Ajouter un timeout de 3 secondes sur tous les appels HTTP.

**Priorite :** Moyenne
**Effort :** 6h

---

### PERF-04 : Pas de Cache Redis

**Probleme :** Aucun cache n'est implemente. Les donnees frequentes (catalogues, stats, noms d'organisations) sont recuperees depuis la base a chaque requete.

**Fix concret :**
- Ajouter Spring Cache + Redis pour les donnees en lecture seule.
- Cacheable sur les methodes : `getOrganizationName`, `getUserName`, `listProducts`, `stats()`.
- Utiliser `@Cacheable` et `@CacheEvict`.

**Priorite :** Haute
**Effort :** 8h

---

## 4. Defauts Architecturaux

### ARCH-01 : Le Gateway utilise un proxy HTTP manuel

**Probleme :** `GatewayProxyController.java` implemente un proxy HTTP personnalise avec `java.net.http.HttpClient`. C'est fragile (pas de load balancing, pas de retry, pas de circuit breaker, pas de timeout configurable).

**Fichier :** `backend/api-gateway/src/main/java/com/paymentplatform/gateway/GatewayProxyController.java`

**Fix concret :**
- Migrer vers Spring Cloud Gateway avec configuration YAML de routes :
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: identity
          uri: http://identity-service:8082
          predicates:
            - Path=/api/auth/**,/api/users/**
        - id: organization
          uri: http://organization-service:8083
          predicates:
            - Path=/api/orders/**,/api/balances/**
```
- Cela ajoute automatiquement le load balancing, les timeouts, les retries.

**Priorite :** Haute
**Effort :** 16h

---

### ARCH-02 : CatalogController contredit le DDD — repository direct

**Probleme :** `CatalogController.java` injecte directement les repositories (`ProductCategoryRepository`, `ProductFamilyRepository`) au lieu d'utiliser des Use Cases. C'est incoherent avec le reste de l'architecture DDD/Clean.

**Fichier :** `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/CatalogController.java:24-25`

**Fix concret :**
- Creer `CreateCategoryUseCase`, `ListCategoriesUseCase`, `DeleteCategoryUseCase`.
- Creer `CreateFamilyUseCase`, `UpdateFamilyUseCase`, `DeleteFamilyUseCase`.
- Transferer la logique du controller vers les use cases.

**Priorite :** Moyenne
**Effort :** 6h

---

### ARCH-03 : OrganizationValidationClient — appel direct au lieu de passer par le Gateway

**Probleme :** `OrganizationValidationClient.java:130` appelle directement `http://organization-service:8083` pour `getOrganizationName` et `getUserName`, mais passe par le Gateway pour `validateShop`/`validateSupplier`. C'est incohrent et casse l'isolation des services.

**Fichier :** `backend/payment-service/src/main/java/com/paymentplatform/payment/infrastructure/http/OrganizationValidationClient.java:130`

**Fix concret :** Uniformiser tous les appels inter-services en passant par le Gateway, ou les faire tous en interne (direct-to-service) avec un Shared Secret uniforme.

**Priorite :** Moyenne
**Effort :** 2h

---

### ARCH-04 : Transactionnalite incomplète sur ConfirmOrderUseCase

**Probleme :** `ConfirmOrderUseCase.java:42-49` verifie la disponibilite du stock mais ne reserve PAS le stock. Seul `CreateOrderUseCase` reserve le stock. Cela signifie qu'entre la confirmation et la preparation, le stock pourrait etre utilise par une autre commande.

**Fichier :** `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/ConfirmOrderUseCase.java:42-49`

**Fix concret :**
Ajouter la reservation du stock dans `ConfirmOrderUseCase` :
```java
for (OrderItem item : items) {
    Product product = products.findByIdForUpdate(item.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
    int availableQty = product.getQuantity() - product.getReservedQty();
    if (availableQty < item.getQuantity()) {
        throw new ConflictException("Stock insuffisant pour le produit " + product.getName());
    }
    product.setReservedQty(product.getReservedQty() + item.getQuantity());
    products.save(product);
}
```

**Priorite :** Haute
**Effort :** 2h

---

## 5. Fonctionnalites Manquantes par Rapport a la Spec

### SPEC-01 : Pas de notifications de commande (C1)

**Probleme :** Le spec (section 9.1) exige des notifications pour chaque etat de commande (confirme, prepare, livre, etc.). Actuellement, seuls les evenements de paiement declenchent des notifications.

**Fix concret :**
- Ajouter un `OrderEventConsumer` dans le notification-service, similaire a `PaymentEventConsumer`.
- Ecouter les queues `notification.orders`.
- Declencher des notifications pour : `ORDER_CONFIRMED`, `ORDER_PREPARING`, `ORDER_READY_FOR_DELIVERY`, `ORDER_DELIVERED`, `ORDER_ACCEPTED`, `ORDER_CANCELLED`, `ORDER_REJECTED`.
- Le `CreateOrderUseCase`, `ConfirmOrderUseCase`, etc. doivent emettre des evenements via l'Outbox.

**Priorite :** Haute
**Effort :** 10h

---

### SPEC-02 : Prix unitaire non stocke a la creation de la commande (C2)

**Probleme :** `CreateOrderUseCase.java:92` copie `product.getUnitPrice()` dans `OrderItem.unitPrice`. C'est correct. Mais il n'y a pas de mecanisme pour garder un historique des prix (C3). Si le prix du produit change, les anciennes commandes afficheront le nouveau prix.

**Fichier :** `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/CreateOrderUseCase.java:92`

**Note :** Le prix est bien stocke a la creation. Le probleme est l'absence d'historique de prix (C3).

**Fix concret :**
- Creer une table `product_price_history` (product_id, price, effective_from, effective_to).
- Enregistrer chaque changement de prix dans `StockService.updateProduct()`.
- C'est un sujet Moyen terme.

**Priorite :** Moyenne
**Effort :** 4h

---

### SPEC-03 : AcceptOrderUseCase ne cree PAS de paiement automatique

**Probleme :** `OrderController.java:164` a un `// TODO: auto-create payment for ASAP orders` mais ce n'est pas implemente. Le spec (section 4.3, scenario 1) indique que l'acceptation devrait debiter le stock definitivement (ce qui est fait), mais ne mentionne pas explicitement la creation auto de paiement. Cependant, le endpoint `accept-asap` existe avec ce TODO.

**Fichier :** `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/OrderController.java:164`

**Fix concret :**
Appeler le payment-service pour creer un paiement quand `asapPayment=true` lors de l'acceptation.

**Priorite :** Moyenne
**Effort :** 6h

---

### SPEC-04 : Pas d'export CSV/Import (K2)

**Probleme :** Le spec (K2) exige l'import/export CSV pour les produits et stocks. Rien n'est implemente.

**Fix concret :**
- Ajouter des endpoints : `POST /api/suppliers/{id}/products/import` et `GET /api/suppliers/{id}/products/export`.
- Utiliser Apache Commons CSV ou OpenCSV.
- Cote frontend, ajouter des boutons d'import/export.

**Priorite :** Haute
**Effort :** 10h

---

### SPEC-05 : Pas d'alertes de stock bas (K3)

**Probleme :** Le spec (K3) exige des notifications automatiques quand le stock passe sous le seuil minimum. Actuellement, `Product.minQuantity` existe mais n'est jamais verifie pour declencher une alerte.

**Fix concret :**
- Dans `StockService.createMovement()`, apres mise a jour du stock, verifier `product.getQuantity() < product.getMinQuantity()`.
- Si c'est le cas, envoyer un evenement `stock.low` via l'Outbox vers le notification-service.
- Creer un `StockEventConsumer` dans le notification-service.

**Priorite :** Haute
**Effort :** 6h

---

### SPEC-06 : Pas de gestion du stock pour RejectOrderUseCase / DeliveryRejectOrderUseCase

**Probleme :**
- `RejectOrderUseCase.java` : Quand une commande DELIVERED est rejetee, le stock reserve n'est pas libere. Les items ont deja ete retires du stock physiquement dans `AcceptOrderUseCase`, mais le rejet apres livraison devrait restaurer le stock.
- `DeliveryRejectOrderUseCase.java` : Quand une livraison est rejetee (IN_DELIVERY → DELIVERY_REJECTED), le stock reserve n'est pas libere. La spec (section 4.4) indique que l'annulation libere le stock reserve.

**Fichiers :**
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/RejectOrderUseCase.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/DeliveryRejectOrderUseCase.java`

**Fix concret :**
Dans `RejectOrderUseCase`, restaurer le stock :
```java
for (OrderItem item : items) {
    Product product = products.findByIdForUpdate(item.getProductId())
            .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
    product.setQuantity(product.getQuantity() + item.getQuantity());
    product.setReservedQty(Math.max(0, product.getReservedQty() - item.getQuantity()));
    products.save(product);
}
```

**Priorite :** Haute
**Effort :** 3h

---

### SPEC-07 : Pas de circuit breaker (R1) ni retry (R2)

**Probleme :** Aucune resiliance n'est implementee pour les appels inter-services. Si le organization-service est down, le payment-service echoue silencieusement.

**Fix concret :**
- Ajouter Resilience4j (`@CircuitBreaker`, `@Retry`) sur `OrganizationValidationClient`.
- Configurer : retry 3 fois avec backoff exponentiel, circuit breaker a 50% d'echecs.
- Ajouter le dependance dans le `pom.xml` shared-lib.

**Priorite :** Haute
**Effort :** 6h

---

## 6. Qualite du Code

### QC-01 : Object Mapper non partage dans OrganizationValidationClient

**Probleme :** `OrganizationValidationClient.java:25` cree un `new ObjectMapper()` au lieu d'utiliser le bean Spring. Cela peut causer des incompatibilites de configuration JSON.

**Fichier :** `backend/payment-service/src/main/java/com/paymentplatform/payment/infrastructure/http/OrganizationValidationClient.java:25`

**Fix concret :** Injecter `ObjectMapper` via le constructeur.

**Priorite :** Basse
**Effort :** 0.5h

---

### QC-02 : Reference de commande generee avec `System.currentTimeMillis()`

**Probleme :** `Order.java:107` genere la reference avec `"ORD-" + System.currentTimeMillis()`. En cas de creation concurrente, deux commandes peuvent recevoir la meme reference.

**Fichier :** `backend/organization-service/src/main/java/com/paymentplatform/organization/domain/model/Order.java:107`

**Fix concret :**
```java
order.reference = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
    + "-" + System.currentTimeMillis();
```
Ou mieux, utiliser une sequence database MySQL AUTO_INCREMENT ou un generateur de UUID.

**Priorite :** Moyenne
**Effort :** 1h

---

### QC-03 : Absence de tests d'integration (T2)

**Probleme :** Le projet ne contient que des tests unitaires (6 fichiers de test). Pas de tests d'integration avec Testcontainers, bien que la dependance soit declaree dans le `pom.xml`.

**Fichiers de test existants :**
- `PaymentTest.java` (12 tests unitaires)
- `AuthUseCaseTest.java` (4 tests unitaires)
- `AgentManagementUseCaseTest.java`
- `OrganizationCascadeUseCaseTest.java`
- `UserTest.java`
- `OrganizationTest.java`

**Fix concret :**
- Creer des classes d'integration test pour chaque service avec `@SpringBootTest` + `Testcontainers`.
- Tester les flux complets : creation de commande, paiement, livraison.
- Objectif : couverture > 80% sur les Use Cases.

**Priorite :** Haute
**Effort :** 20h

---

### QC-04 : OutboxRelay ne fait pas de batch update

**Probleme :** `OutboxRelay.java:39-48` parcourt les evenements un par un et les marque individuellement. Apres 100 evenements, cela fait 100 appels UPDATE au lieu d'un seul batch.

**Fichier :** `backend/shared-lib/src/main/java/com/paymentplatform/shared/infrastructure/outbox/OutboxRelay.java:39-48`

**Fix concret :**
```java
@Transactional
public void relay() {
    List<OutboxEventEntity> pending = repository.findTop100ByProcessedAtIsNullOrderByIdAsc();
    if (pending.isEmpty()) return;

    List<String> successIds = new ArrayList<>();
    for (OutboxEventEntity event : pending) {
        try {
            publisher.publish(topology.exchangeFor(event.getEventType()),
                    event.getEventType(), event.getEventId(), event.getPayload());
            successIds.add(event.getEventId());
        } catch (Exception e) {
            log.error("Echec publication event {}", event.getEventId(), e);
        }
    }
    if (!successIds.isEmpty()) {
        repository.markBatchProcessed(successIds, Instant.now());
    }
}
```

**Priorite :** Basse
**Effort :** 2h

---

### QC-05 : Delete physique des produits avec stock reserve

**Probleme :** `StockController.java:70-79` — `DELETE /api/suppliers/{supplierId}/products/{productId}` supprime physiquement un produit. Si le produit a du stock reserve par des commandes en cours, la suppression casse les commandes.

**Fichier :** `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/StockController.java:70-79`

**Fix concret :**
Implementer un soft delete : changer le status du produit a `INACTIVE` au lieu de le supprimer. Verifier qu'aucune commande active n'utilise le produit avant de le desactiver.

**Priorite :** Moyenne
**Effort :** 3h

---

### QC-06 : PAS de validateur de reference pour getByReference

**Probleme :** `PaymentController.java:80` — Le parametre `reference` dans `getByReference` n'a pas de validation (pas de `@Pattern`). Un attaquant pourrait injecter du SQL ou des caracteres dangereux.

**Fichier :** `backend/payment-service/src/main/java/com/paymentplatform/payment/interfaces/rest/PaymentController.java:80`

**Fix concret :**
Ajouter `@PathVariable @Pattern(regexp = "PAY-\\d+-\\d{6}") String reference`.

**Priorite :** Moyenne
**Effort :** 0.5h

---

## 7. Frontend et UX

### FE-01 : Fuite de memoire dans NotificationService

**Probleme :** `notification.service.ts:27-29` cree un deuxieme `interval()` sans le stocker dans une variable. Ce subscription ne peut jamais etre arretee, causant une fuite de memoire.

**Fichier :** `payment-platform-ui/src/app/services/notification.service.ts:27-29`

**Fix concret :**
```typescript
private unreadCountSub?: Subscription;

startPolling(intervalMs = 30000): void {
    this.fetchNotifications().subscribe();
    this.fetchUnreadCount().subscribe();

    this.pollingSub = interval(intervalMs).pipe(
        switchMap(() => this.fetchNotifications())
    ).subscribe();
    this.unreadCountSub = interval(intervalMs).pipe(
        switchMap(() => this.fetchUnreadCount())
    ).subscribe();
}

stopPolling(): void {
    this.pollingSub?.unsubscribe();
    this.unreadCountSub?.unsubscribe();
}
```

**Priorite :** Moyenne
**Effort :** 1h

---

### FE-02 : AuthService et LoginService separes sans raison

**Probleme :** `AuthService` ne contient que `register()`, tandis que `LoginService` contient `login()`, `getMe()`, `logout()`, `isLoggedIn()`, `getCurrentUser()`, `hasRole()`. C'est confusant.

**Fichiers :**
- `payment-platform-ui/src/app/services/auth.service.ts`
- `payment-platform-ui/src/app/services/login.service.ts`

**Fix concret :** Fusionner les deux services en un seul `AuthService` complet.

**Priorite :** Basse
**Effort :** 1h

---

### FE-03 : Absence de gestion d'expiration du token cote frontend

**Probleme :** `jwt.interceptor.ts` ne verifie pas si le token JWT est expire. Il ne fait que rediriger sur 401. Le spec (S2) exige un refresh automatique.

**Fichier :** `payment-platform-ui/src/app/core/jwt.interceptor.ts`

**Fix concret :**
- Decoder le token JWT cote frontend pour verifier l'expiration.
- Si expire, tenter un refresh automatique.
- Si le refresh echoue, rediriger vers `/login`.

**Priorite :** Moyenne
**Effort :** 4h

---

### FE-04 : Pas de gestion d'erreur centralisee

**Probleme :** Chaque service Angular gere les erreurs individuellement. Il n'y a pas de gestion centralisee des erreurs API (intercepteur global).

**Fix concret :** Creer un `ErrorInterceptor` qui intercepte les erreurs HTTP et affiche des toast messages contextuels.

**Priorite :** Basse
**Effort :** 3h

---

## 8. DevOps et Fiabilite

### DEVOPS-01 : Pas de CI/CD Pipeline (E1)

**Probleme :** Aucun pipeline CI/CD n'est configure (pas de `.github/workflows/`, pas de `Jenkinsfile`).

**Fix concret :**
Creer un pipeline GitHub Actions :
1. `build` : `mvn clean compile`
2. `test` : `mvn test`
3. `integration-test` : `mvn verify` avec Testcontainers
4. `build-docker` : `docker build`
5. `deploy` : Push vers registry + deploy sur environnement staging

**Priorite :** Haute
**Effort :** 8h

---

### DEVOPS-02 : Pas d'environment staging (E2)

**Probleme :** `docker-compose.yml` definit un seul environnement. Pas de copie staging pour les tests.

**Fix concret :**
Creer `docker-compose.staging.yml` avec des configurations specifiques (secrets differents, replicas, monitoring active).

**Priorite :** Haute
**Effort :** 4h

---

### DEVOPS-03 : Secrets en dur dans docker-compose.yml (E5)

**Probleme :** `docker-compose.yml:72,105,132` contient `INTERNAL_SECRET: dev-internal-secret-change-me` en dur. Les secrets doivent etre geres via un vault ou au minimum via `.env`.

**Fichier :** `deploy/docker-compose.yml`

**Fix concret :**
- Extraire `INTERNAL_SECRET` dans `.env` (qui est deja dans `.gitignore`).
- Ajouter des commentaires pour提醒 de changer les secrets en production.
- A terme, migrer vers HashiCorp Vault ou AWS Secrets Manager.

**Priorite :** Haute
**Effort :** 2h

---

### DEVOPS-04 : Pas de Health Checks detailles (O4)

**Probleme :** Les actuator health checks sont exposes (`/actuator/health`) mais pas configurees pour verifier MySQL, RabbitMQ, Elasticsearch individuellement.

**Fichier :** Tous les `application.yml` — `management.endpoints.web.exposure.include: health,info`

**Fix concret :**
Activer les health checks details :
```yaml
management:
  endpoint:
    health:
      show-details: when-authorized
  health:
    db:
      enabled: true
    rabbit:
      enabled: true
```

**Priorite :** Moyenne
**Effort :** 2h

---

### DEVOPS-05 : Pas de Dead Letter Queue (R4)

**Probleme :** Aucune Dead Letter Queue n'est configuree pour RabbitMQ. Si un message ne peut pas etre traite, il est perdu.

**Fix concret :**
- Configurer les DLQ dans `AmqpTopology.java` :
```java
@Bean
public Queue dlqQueue() {
    return QueueBuilder.durable("notification.payments.dlq").build();
}
```
- Ajouter un consumer DLQ pour les messages echoues.

**Priorite :** Moyenne
**Effort :** 4h

---

## Recapitulatif par Priorite

### Haute Priorite (28h total)
| ID | Description | Effort |
|---|---|---|
| SEC-01 | Gateway ne valide pas les JWT | 4h |
| SEC-02 | Secrets JWT faibles | 2h |
| SEC-03 | Rate Limiting | 8h |
| SEC-05 | Refresh Token | 12h |
| AUTH-01 | getByReference sans verification | 1h |
| AUTH-02 | markAsRead sans verification | 2h |
| PERF-01 | N+1 appels HTTP noms | 6h |
| PERF-02 | Pagination reelle | 12h |
| PERF-04 | Cache Redis | 8h |
| ARCH-01 | Migrer vers Spring Cloud Gateway | 16h |
| ARCH-04 | Reservation stock dans ConfirmOrder | 2h |
| SPEC-01 | Notifications de commande | 10h |
| SPEC-05 | Alertes de stock bas | 6h |
| SPEC-06 | Stock pour Reject/DeliveryReject | 3h |
| SPEC-07 | Circuit Breaker + Retry | 6h |
| SPEC-04 | Import/Export CSV | 10h |
| QC-03 | Tests d'integration | 20h |
| DEVOPS-01 | CI/CD Pipeline | 8h |
| DEVOPS-02 | Environment staging | 4h |
| DEVOPS-03 | Secrets management | 2h |

### Moyenne Priorite (49h total)
| ID | Description | Effort |
|---|---|---|
| SEC-04 | Password Policy | 3h |
| SEC-06 | Token dans localStorage | 6h |
| AUTH-03 | Roles hardcodes | 4h |
| AUTH-04 | Audit trail | 8h |
| PERF-03 | Appels HTTP bloquants | 6h |
| ARCH-02 | CatalogController DDD | 6h |
| ARCH-03 | ValidationClient incohérent | 2h |
| SPEC-02 | Historique de prix | 4h |
| SPEC-03 | Paiement auto pour ASAP | 6h |
| QC-02 | Reference commande unique | 1h |
| QC-05 | Soft delete produits | 3h |
| QC-06 | Validation reference | 0.5h |
| FE-01 | Fuite memoire notification | 1h |
| FE-03 | Expiration token frontend | 4h |
| DEVOPS-04 | Health checks detailles | 2h |
| DEVOPS-05 | Dead Letter Queue | 4h |

### Basse Priorite (6.5h total)
| ID | Description | Effort |
|---|---|---|
| QC-01 | ObjectMapper non partage | 0.5h |
| QC-04 | Batch update outbox | 2h |
| FE-02 | Fusionner auth services | 1h |
| FE-04 | Error interceptor | 3h |

---

*Document genere le 07/09/2026 — Analyse basee sur la specification fonctionnelle et le code source existant.*
