# Plan d'Amélioration DDD - Payment Platform

> Audit historique — obsolète comme plan d'action. Ne pas exécuter tel quel :
> les constats B/M qu'il recoupe sont soldés et suivis dans
> [../COMPTE_RENDU_EXPERT.md](../COMPTE_RENDU_EXPERT.md). Référence DDD à jour :
> [ddd.md](ddd.md). Index : [README.md](README.md).

## Executive Summary

L'analyse du code source de la plateforme de paiement révèle une architecture hexagonale globalement bien structurée avec une séparation correcte entre les couches. Cependant, des **violations critiques du DDD** subsistent, principalement :

- **Modèles de domaine anémiques** dans l'Organization Service (entités JPA avec getters/setters, sans comportement métier)
- **Entités JPA dans le domain layer** (Notification, Order, Product, etc.)
- **Violation de la règle de dépendance** : le domaine dépend de l'infrastructure (ex: `AuditActions` utilisé dans `Payment.java:4`)
- **Abus de la réflexion** dans les mappers (`PaymentMapper.java:36-58`)
- **Repository dans le mauvais layer** (Notification Service)
- **Absence de domain events** émis par les agrégats eux-mêmes
- **Value objects inconsistants** (PhoneNumber modifie le paramètre record)

**État cible** : Un DDD strict avec des agrégats riches en comportement, des domain events émis par les agrégats, des value objects immuables avec validation, et une isolation totale du domaine de tout framework.

---

## 1. Audit DDD Actuel

### 1.1 Shared Library (shared-lib)

#### Points positifs
- `DomainEvent` interface bien définie (`shared-lib/.../domain/event/DomainEvent.java:12-23`)
- Value objects `UserId`, `OrganizationId`, `RoleCode` correctement modélisés
- `PermissionCatalog` et `Permissions` bien isolés
- Outbox pattern correctement implémenté (`OutboxEventStore`, `OutboxRelay`, `OutboxPublisher`)
- `EventDeduplicator` pour l'idempotence des consommateurs
- Exceptions domain bien typées (`DomainException`, `NotFoundException`, etc.)

#### Violations trouvées

| # | Violation | Fichier | Ligne | Sévérité |
|---|-----------|---------|-------|----------|
| S1 | `AuditActions` (constantes string) dans `infrastructure.audit` mais utilisé par le domaine | `Payment.java` | L4 | HAUTE |
| S2 | `AuditRecorder` (interface infrastructure) injecté dans les use cases domain | `RegisterUseCase.java` | L7 | MOYENNE |
| S3 | `OutboxEventStore` dans `infrastructure.outbox` mais devrait être un port dans `domain` | `OutboxEventStore.java` | L15-18 | MOYENNE |
| S4 | `AuthenticatedUser` dans `infrastructure.security` — devrait être un concept domaine | `AuthenticatedUser.java` | L9 | BASSE |
| S5 | `AmqpTopology` couplé aux types d'événements — devrait être dans chaque service | `AmqpTopology.java` | L19-33 | BASSE |
| S6 | Shared kernel trop large : contient de l'infrastructure (JPA, AMQP) partagée | Multiple | — | HAUTE |

### 1.2 Identity Service

#### Points positifs
- Aggregate Root `User` bien conçu avec comportement métier (`disable()`, `activate()`, `updateProfile()`, `assertCanManageOrganization()`)
- Value objects immuables et validés : `Email`, `Username`, `PhoneNumber`, `PasswordHash`
- Repository interface dans le domain layer (`UserRepository.java`)
- Use cases bien isolés et responsables
- Anti-corruption layer via `OrganizationStatusPort` et `OrganizationGatewayClient`
- Domain events correctement émis via outbox

#### Violations trouvées

| # | Violation | Fichier | Ligne | Sévérité |
|---|-----------|---------|-------|----------|
| I1 | `PhoneNumber` modifie le paramètre `value` du record dans le compact constructor | `PhoneNumber.java` | L12-26 | HAUTE |
| I2 | `PasswordSetupUseCase` importe directement `EmailService` (infrastructure) | `PasswordSetupUseCase.java` | L4-5 | MOYENNE |
| I3 | `PasswordSetupToken` est une entité JPA dans `infrastructure.email` mais devrait être dans un sous-package dédié | `PasswordSetupToken.java` | L1-51 | BASSE |
| I4 | Les use cases créent les événements directement (UUID.randomUUID, Instant.now) au lieu que l'agrégat les émette | `RegisterUseCase.java` | L77-78 | HAUTE |
| I5 | `User.roles()` retourne une copie mutable (`EnumSet.copyOf`) — les rôles devraient être immuables | `User.java` | L114 | BASSE |

### 1.3 Organization Service

#### Points positifs
- Aggregate Root `Organization` bien conçu avec comportement
- `SupplierShopRelation` correctement modélisé comme entity
- Value objects `OrganizationName`, `OrganizationType`, `OrganizationStatus` bien définis
- Moteur d'optimisation de stock (`StockOptimizationEngine`) dans le domaine

#### Violations trouvées

| # | Violation | Fichier | Ligne | Sévérité |
|---|-----------|---------|-------|----------|
| O1 | **CRITIQUE** : `Order` est une entité JPA (`@Entity`, `@Table`) dans `domain/model/` | `Order.java` | L1-207 | CRITIQUE |
| O2 | **CRITIQUE** : `Product` est une entité JPA dans `domain/model/` | `Product.java` | L1-109 | CRITIQUE |
| O3 | **CRITIQUE** : `OrderItem` est une entité JPA dans `domain/model/` | `OrderItem.java` | L1-77 | CRITIQUE |
| O4 | **CRITIQUE** : `StockMovement` est une entité JPA dans `domain/model/` | `StockMovement.java` | L1-59 | CRITIQUE |
| O5 | **CRITIQUE** : `BalanceEntry` est une entité JPA dans `domain/model/` | `BalanceEntry.java` | L1-95 | CRITIQUE |
| O6 | **CRITIQUE** : `OrderEvent` est une entité JPA dans `domain/model/` | `OrderEvent.java` | L1-51 | CRITIQUE |
| O7 | **CRITIQUE** : `ProductFamily` est une entité JPA dans `domain/model/` | `ProductFamily.java` | L1-67 | CRITIQUE |
| O8 | **CRITIQUE** : `ProductCategory` est une entité JPA dans `domain/model/` | `ProductCategory.java` | L1-55 | CRITIQUE |
| O9 | `Product` est un anemic model : getters/setters, aucun comportement métier | `Product.java` | L79-108 | HAUTE |
| O10 | `Order` mélange comportement métier et annotations JPA (`@PrePersist`, `@PreUpdate`) | `Order.java` | L81-96 | HAUTE |
| O11 | `StockService` contient de la logique métier (gestion des stocks) au lieu d'être dans le domaine | `StockService.java` | L101-133 | HAUTE |
| O12 | `CreateOrderUseCase` contient de la logique métier complexe (validation, calculs) | `CreateOrderUseCase.java` | L50-138 | MOYENNE |
| O13 | Aucun domain event n'est émis par les agrégats Organization/Order | Multiple | — | HAUTE |
| O14 | `SupplierShopRelation` a un setter `setId(Long)` — les identifiants ne devraient pas être modifiables | `SupplierShopRelation.java` | L57 | BASSE |

### 1.4 Payment Service

#### Points positifs
- Aggregate Root `Payment` bien conçu avec machine à états dans l'agrégat
- Value objects `Money`, `PaymentReference`, `RejectionReason` avec validation
- Repository interface dans le domain layer
- Outbox pattern correctement utilisé
- Machine à états dans `PaymentStatus` avec transitions validées

#### Violations trouvées

| # | Violation | Fichier | Ligne | Sévérité |
|---|-----------|---------|-------|----------|
| P1 | **CRITIQUE** : `PaymentMapper` utilise la réflexion pour instancier `Payment` | `PaymentMapper.java` | L36-58 | CRITIQUE |
| P2 | `Payment` a un constructeur privé mais le mapper contourne l'encapsulation via réflexion | `PaymentMapper.java` | L41 | HAUTE |
| P3 | `PaymentEvent` est un record mais stocké comme entité JPA séparée — concept confus | `PaymentEvent.java` | L5-9 | MOYENNE |
| P4 | `PaymentReference` n'a pas de validation (uniquement un générateur) | `PaymentReference.java` | L6-12 | BASSE |
| P5 | `OrganizationValidationClient` est directement injecté dans les use cases au lieu d'utiliser un port | `CreatePaymentUseCase.java` | L27 | MOYENNE |
| P6 | `PaymentIndexerService` (infrastructure) injecté dans les use cases | `CreatePaymentUseCase.java` | L30 | MOYENNE |
| P7 | `PaymentController` contient de la logique d'autorisation métier (vérification shop/supplier) | `PaymentController.java` | L73-82 | MOYENNE |

### 1.5 Notification Service

#### Points positifs
- Consumers bien structurés avec déduplication
- API REST simple et cohérente

#### Violations trouvées

| # | Violation | Fichier | Ligne | Sévérité |
|---|-----------|---------|-------|----------|
| N1 | **CRITIQUE** : `Notification` est une entité JPA (`@Entity`, `@Table`) dans `domain/model/` | `Notification.java` | L1-70 | CRITIQUE |
| N2 | **CRITIQUE** : `NotificationRepository` est un `JpaRepository` dans `domain/model/` | `NotificationRepository.java` | L1-36 | CRITIQUE |
| N3 | Le domain layer n'a aucun comportement métier — pas de aggregate root | `Notification.java` | — | HAUTE |
| N4 | `PaymentEventConsumer` crée directement des `Notification` sans passer par un use case | `PaymentEventConsumer.java` | L63-75 | MOYENNE |
| N5 | `NotificationController` accède directement au repository sans use case | `NotificationController.java` | L20 | MOYENNE |
| N6 | Pas de value objects pour `readStatus`, `type` — utilisation de String | `Notification.java` | L26-27 | MOYENNE |

### 1.6 API Gateway

#### Points positifs
- Filtre JWT de validation
- Rate limiting
- Proxy controller simple

#### Violations trouvées

| # | Violation | Fichier | Ligne | Sévérité |
|---|-----------|---------|-------|----------|
| G1 | `GatewayProxyController` potentiellement trop générique — pourrait contourner les autorisations | `GatewayProxyController.java` | — | MOYENNE |

---

## 2. Violations Critiques DDD

### 2.1 Aggregate Design

#### 2.1.1 Entités JPA dans le Domain Layer (CRITIQUE)

**Problème** : 8 classes dans `organization-service/domain/model/` et 1 dans `notification-service/domain/model/` portent des annotations JPA (`@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`, `@Version`, `@PrePersist`, `@PreUpdate`). Cela viole la règle fondamentale du DDD : **le domaine ne connaît aucun framework**.

**Fichiers concernés** :
- `organization-service/.../domain/model/Order.java:1-12` — `@Entity`, `@Table(name = "orders")`
- `organization-service/.../domain/model/Product.java:1-9` — `@Entity`, `@Table(name = "products")`
- `organization-service/.../domain/model/OrderItem.java:1-10` — `@Entity`, `@Table(name = "order_items")`
- `organization-service/.../domain/model/StockMovement.java:1-8` — `@Entity`, `@Table(name = "stock_movements")`
- `organization-service/.../domain/model/BalanceEntry.java:1-9` — `@Entity`, `@Table(name = "balance_ledger")`
- `organization-service/.../domain/model/OrderEvent.java:1-9` — `@Entity`, `@Table(name = "order_events")`
- `organization-service/.../domain/model/ProductFamily.java:1-8` — `@Entity`, `@Table(name = "product_families")`
- `organization-service/.../domain/model/ProductCategory.java:1-8` — `@Entity`, `@Table(name = "product_categories")`
- `notification-service/.../domain/model/Notification.java:1-6` — `@Entity`, `@Table(name = "notifications")`

**Impact** : Le domaine dépend de Jakarta Persistence. Impossible de tester le domaine sans JPA. Violation du principe de dépendance directionnelle.

#### 2.1.2 Agrégats sans comportement (Anemic Domain Model)

**Problème** : `Product`, `StockMovement`, `BalanceEntry`, `OrderEvent`, `ProductFamily`, `ProductCategory` n'ont aucun comportement métier — ce sont des containers de getters/setters.

**Exemple concret** (`Product.java:79-108`) :
```java
// ANTI-PATTERN : Anemic Domain Model
public void setName(String name) { this.name = name; }
public void setQuantity(Integer quantity) { this.quantity = quantity; }
public void setReservedQty(Integer reservedQty) { this.reservedQty = reservedQty; }
// Aucune validation, aucune règle métier
```

**Exemple corrigé** :
```java
// Domain Model riche
public class Product {
    // ... champs ...
    
    public void reserve(int quantity) {
        if (quantity <= 0) throw new DomainException("La quantité doit être positive");
        int available = this.quantity - this.reservedQty;
        if (available < quantity) {
            throw new InsufficientStockException(this.sku, available, quantity);
        }
        this.reservedQty += quantity;
        this.updatedAt = Instant.now();
    }
    
    public void releaseReservation(int quantity) {
        if (quantity > this.reservedQty) {
            throw new DomainException("Impossible de libérer plus que la réservation");
        }
        this.reservedQty -= quantity;
        this.updatedAt = Instant.now();
    }
    
    public void adjustStock(int newQuantity, String reason) {
        if (newQuantity < 0) throw new DomainException("Le stock ne peut pas être négatif");
        this.quantity = newQuantity;
        this.updatedAt = Instant.now();
    }
}
```

#### 2.1.3 Absence de Domain Events émis par les agrégats

**Problème** : Les agrégats `Organization`, `Order`, `Product` n'émettent aucun domain event. Les événements sont créés manuellement dans les use cases avec `UUID.randomUUID()` et `Instant.now()`.

**Exemple** (`RegisterUseCase.java:77-78`) :
```java
// Les use cases créent les événements manuellement
outbox.append(new UserCreatedEvent(UUID.randomUUID(), Instant.now(), 
    user.id().value(), null, List.of(role.name())), String.valueOf(user.id().value()));
```

**Devrait être** :
```java
// L'agrégat émet l'événement
user.getDomainEvents().forEach(outbox::append);
```

### 2.2 Domain Layer Integrity

#### 2.2.1 Dépendance du domaine vers l'infrastructure

**Problème** : `Payment.java:4` importe `AuditActions` depuis `infrastructure.audit` :
```java
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
```

Cela signifie que le domaine (Payment aggregate) dépend de l'infrastructure — violation directe de la règle de dépendance.

**Impact** : Le domaine ne peut pas être compilé ou testé sans l'infrastructure.

#### 2.2.2 Use cases dépendant de l'infrastructure

**Fichiers concernés** :
- `CreatePaymentUseCase.java:15` — importe `OrganizationValidationClient` (infrastructure)
- `CreatePaymentUseCase.java:17` — importe `PaymentIndexerService` (infrastructure)
- `ListPaymentsUseCase.java:11` — importe `OrganizationValidationClient` (infrastructure)
- `PasswordSetupUseCase.java:3-4` — importe `EmailService` et `PasswordSetupTokenRepository` (infrastructure)

### 2.3 Value Objects

#### 2.3.1 PhoneNumber modifie le paramètre record

**Problème** (`PhoneNumber.java:12-26`) :
```java
public PhoneNumber {
    if (value != null && !value.isBlank()) {
        // ...
        value = normalized;  // MODIFICATION DU PARAMÈTRE DU RECORD
    } else {
        value = null;  // MODIFICATION DU PARAMÈTRE DU RECORD
    }
}
```

Les records sont censés être immuables. Modifier `value` dans le compact constructor est un anti-pattern qui peut causer des comportements inattendus.

**Correction** :
```java
public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value != null && !value.isBlank()) {
            String normalized = value.trim();
            if (!normalized.startsWith("+")) {
                if (normalized.startsWith("0")) normalized = normalized.substring(1);
                normalized = TUNISIA_PREFIX + normalized;
            }
            if (!normalized.matches(E164_PATTERN)) {
                throw new DomainException("Numéro de téléphone invalide");
            }
            value = normalized;
        } else {
            value = null;
        }
    }
    // Note: En Java 16+, on peut réassigner value dans le compact constructor
    // mais c'est une pratique discutable. Alternative: factory method.
}
```

#### 2.3.2 PaymentReference sans validation

`PaymentReference.java:6-12` n'a aucune validation — le constructeur accepte n'importe quelle string. Seul le générateur crée des références valides.

### 2.4 Repository Pattern

#### 2.4.1 Repository dans le domaine layer (Notification Service)

**Problème critique** (`NotificationRepository.java:1-36`) :
```java
package com.paymentplatform.notification.domain.model;

import org.springframework.data.jpa.repository.JpaRepository;
// ...

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // ...
}
```

Un `JpaRepository` dans le domaine layer est une violation fondamentale. Le domaine dépend de Spring Data JPA.

#### 2.4.2 Use cases accédant directement aux repositories

`NotificationController.java:20` injecte `NotificationRepository` directement sans passer par un use case :
```java
private final NotificationRepository notifications;
```

### 2.5 Domain Events

#### 2.5.1 Events non émis par les agrégats

Les agrégats n'ont pas de liste de domain events. Les events sont créés dans les use cases.

#### 2.5.2 Shared events dans le shared-lib

Tous les events (`IdentityEvents`, `OrganizationEvents`, `PaymentEvents`, `OrderEvents`) sont dans le shared-lib. Cela crée un couplage fort entre les bounded contexts. Chaque contexte devrait définir ses propres events.

### 2.6 Bounded Contexts

#### 2.6.1 Shared Kernel trop large

Le shared-lib contient :
- Domain events pour TOUS les bounded contexts
- Infrastructure partagée (outbox, audit, security, web)
- Value objects partagés (UserId, OrganizationId, RoleCode)

Cela crée un couplage fort. Un changement dans un bounded context affecte tous les autres.

#### 2.6.2 Couplage via le shared-lib

`Payment.java:4` dépend de `AuditActions` du shared-lib. Si `AuditActions` change, le payment service est affecté.

---

## 3. Plan de Refactoring

### 3.1 Phase 1: Notification Service (Priority: HIGH)

**Objectif** : Séparer le modèle de domaine du modèle de persistence.

#### Étape 1.1 : Créer le modèle de domaine pur

Créer `NotificationDomain.java` dans `domain/model/` (sans annotations JPA) :

```java
package com.paymentplatform.notification.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Notification {
    
    public enum ReadStatus { UNREAD, READ }
    public enum NotificationType { 
        PAYMENT_CREATED, PAYMENT_CONFIRMED, PAYMENT_REJECTED, PAYMENT_CANCELLED,
        ORDER_CREATED, ORDER_CONFIRMED, ORDER_PREPARING, ORDER_READY,
        ORDER_DELIVERED, ORDER_ACCEPTED, ORDER_CANCELLED, ORDER_REJECTED,
        ORDER_DELIVERY_REJECTED, LOW_STOCK_ALERT
    }

    private final Long id;
    private final Long recipientUserId;
    private final Long recipientOrganizationId;
    private final NotificationType type;
    private final String message;
    private ReadStatus readStatus;
    private final Instant createdAt;
    private Instant readAt;
    private final String relatedEntityType;
    private final String relatedEntityId;

    private Notification(Long id, Long recipientUserId, Long recipientOrganizationId,
                         NotificationType type, String message, ReadStatus readStatus,
                         Instant createdAt, Instant readAt,
                         String relatedEntityType, String relatedEntityId) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.recipientOrganizationId = recipientOrganizationId;
        this.type = type;
        this.message = message;
        this.readStatus = readStatus;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
    }

    public static Notification create(Long recipientUserId, Long recipientOrganizationId,
                                      NotificationType type, String message,
                                      String relatedEntityType, String relatedEntityId) {
        Objects.requireNonNull(type, "Le type est requis");
        Objects.requireNonNull(message, "Le message est requis");
        if (message.isBlank()) throw new IllegalArgumentException("Le message ne peut pas être vide");
        return new Notification(null, recipientUserId, recipientOrganizationId,
                type, message, ReadStatus.UNREAD, Instant.now(), null,
                relatedEntityType, relatedEntityId);
    }

    public void markAsRead() {
        if (readStatus == ReadStatus.READ) return; // idempotent
        this.readStatus = ReadStatus.READ;
        this.readAt = Instant.now();
    }

    public boolean belongsTo(Long userId, Long organizationId) {
        return (recipientUserId != null && recipientUserId.equals(userId))
            || (recipientOrganizationId != null && recipientOrganizationId.equals(organizationId));
    }

    // Getters (pas de setters)
    public Long id() { return id; }
    public Long recipientUserId() { return recipientUserId; }
    public Long recipientOrganizationId() { return recipientOrganizationId; }
    public NotificationType type() { return type; }
    public String message() { return message; }
    public ReadStatus readStatus() { return readStatus; }
    public Instant createdAt() { return createdAt; }
    public Instant readAt() { return readAt; }
    public String relatedEntityType() { return relatedEntityType; }
    public String relatedEntityId() { return relatedEntityId; }
}
```

#### Étape 1.2 : Créer l'entité JPA de persistence

Déplacer l'actuel `Notification.java` vers `infrastructure/persistence/NotificationJpaEntity.java` et renommer les champs.

#### Étape 1.3 : Créer un port repository

```java
package com.paymentplatform.notification.domain.repository;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findById(Long id);
    List<Notification> findByRecipientUserId(Long userId);
    List<Notification> findByRecipientOrganizationId(Long organizationId);
    List<Notification> findByRecipientUserIdAndStatus(Long userId, Notification.ReadStatus status);
    List<Notification> findByRecipientOrganizationIdAndStatus(Long organizationId, Notification.ReadStatus status);
    long countByRecipientUserIdAndStatus(Long userId, Notification.ReadStatus status);
    long countByRecipientOrganizationIdAndStatus(Long organizationId, Notification.ReadStatus status);
    int markAllAsReadByUserId(Long userId);
    int markAllAsReadByOrgId(Long organizationId);
}
```

#### Étape 1.4 : Créer l'adapter JPA

```java
package com.paymentplatform.notification.infrastructure.persistence;

@Component
public class JpaNotificationRepository implements NotificationRepository {
    // ... implémentation avec mappers
}
```

#### Étape 1.5 : Créer des use cases

```java
package com.paymentplatform.notification.application.usecase;

@Service
public class GetNotificationsUseCase {
    private final NotificationRepository repository;
    
    public List<NotificationResponse> execute(Long userId, Long organizationId) {
        // ... logique
    }
}

@Service
public class MarkNotificationReadUseCase {
    public void execute(Long notificationId, Long userId, Long organizationId) {
        Notification n = repository.findById(notificationId)...;
        if (!n.belongsTo(userId, organizationId)) throw new ForbiddenException(...);
        n.markAsRead();
        repository.save(n);
    }
}
```

#### Étape 1.6 : Mettre à jour les consumers

Les consumers devraient passer par un use case au lieu de créer directement des notifications :

```java
@Service
public class HandlePaymentEventUseCase {
    private final NotificationRepository repository;
    
    public void handlePaymentCreated(JsonNode event) {
        // Créer les notifications via le modèle de domaine
        Notification supplierNotif = Notification.create(
            null, supplierId, NotificationType.PAYMENT_CREATED,
            message, "PAYMENT", reference);
        repository.save(supplierNotif);
    }
}
```

#### Fichiers à modifier
- `notification-service/.../domain/model/Notification.java` → Supprimer annotations JPA
- `notification-service/.../domain/model/NotificationRepository.java` → Déplacer vers `domain/repository/`
- `notification-service/.../infrastructure/rest/NotificationController.java` → Utiliser des use cases
- `notification-service/.../infrastructure/messaging/PaymentEventConsumer.java` → Utiliser des use cases
- `notification-service/.../infrastructure/messaging/OrderEventConsumer.java` → Utiliser des use cases

### 3.2 Phase 2: Identity Service

#### Étape 2.1 : Corriger PhoneNumber

```java
public record PhoneNumber(String value) {
    public PhoneNumber {
        if (value != null && !value.isBlank()) {
            String normalized = normalize(value);
            if (!normalized.matches(E164_PATTERN)) {
                throw new DomainException("Numéro de téléphone invalide");
            }
            value = normalized;
        } else {
            value = null;
        }
    }
    
    private static String normalize(String raw) {
        String trimmed = raw.trim();
        if (!trimmed.startsWith("+")) {
            if (trimmed.startsWith("0")) trimmed = trimmed.substring(1);
            trimmed = TUNISIA_PREFIX + trimmed;
        }
        return trimmed;
    }
    
    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }
}
```

#### Étape 2.2 : Déplacer les domain events dans le service

Créer `identity-service/.../domain/event/IdentityDomainEvents.java` au lieu de shared-lib.

#### Étape 2.3 : Extraire un port pour EmailService

```java
package com.paymentplatform.identity.application.port;

public interface PasswordSetupEmailPort {
    void sendPasswordSetupEmail(String to, String firstName, String token);
}
```

L'adapter dans `infrastructure/email/` implémentera ce port.

### 3.3 Phase 3: Organization Service

#### Étape 3.1 : Créer les modèles de domaine purs

Pour chaque entité JPA dans `domain/model/`, créer un modèle de domaine pur :

**Order** (domain pur) :
```java
package com.paymentplatform.organization.domain.model;

public class Order {
    private final OrderId id;
    private final OrderReference reference;
    private final OrganizationId supplierId;
    private final OrganizationId shopId;
    private final UserId createdBy;
    private final OrderSource source;
    private OrderStatus status;
    private Money subtotal;
    private TaxInfo taxInfo;
    private Money total;
    private final Currency currency;
    private final List<OrderLine> lines;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;
    
    // Comportement métier
    public void confirm() { transitionTo(OrderStatus.CONFIRMED); }
    public void prepare() { transitionTo(OrderStatus.PREPARING); }
    public void readyForDelivery() { transitionTo(OrderStatus.READY_FOR_DELIVERY); }
    public void deliver(Long receivedBy) { 
        transitionTo(OrderStatus.DELIVERED);
        this.receivedBy = receivedBy;
    }
    public void accept() { transitionTo(OrderStatus.ACCEPTED); }
    public void cancel() { transitionTo(OrderStatus.CANCELLED); }
    
    // Domain events
    private final List<DomainEvent> domainEvents = new ArrayList<>();
    public List<DomainEvent> getDomainEvents() { return List.copyOf(domainEvents); }
    public void clearDomainEvents() { domainEvents.clear(); }
    
    private void transitionTo(OrderStatus newStatus) {
        OrderStatus current = OrderStatus.valueOf(this.status);
        current.assertCanTransitionTo(newStatus);
        this.status = newStatus;
        this.updatedAt = Instant.now();
        this.domainEvents.add(new OrderStateChangedEvent(...));
    }
}
```

**Product** (domain pur) :
```java
package com.paymentplatform.organization.domain.model;

public class Product {
    private final ProductId id;
    private final OrganizationId supplierId;
    private final ProductName name;
    private final SKU sku;
    private String description;
    private Money unitPrice;
    private StockQuantity quantity;
    private StockQuantity minQuantity;
    private StockQuantity reservedQty;
    private ProductStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    
    public void reserve(int qty) {
        int available = quantity.value() - reservedQty.value();
        if (available < qty) throw new InsufficientStockException(sku, available, qty);
        reservedQty = new StockQuantity(reservedQty.value() + qty);
    }
    
    public void releaseReservation(int qty) {
        if (qty > reservedQty.value()) throw new DomainException("...");
        reservedQty = new StockQuantity(reservedQty.value() - qty);
    }
    
    public void adjustStock(int newQty, String reason) {
        quantity = new StockQuantity(newQty);
    }
    
    public boolean isLowStock() {
        return quantity.value() <= minQuantity.value();
    }
}
```

#### Étape 3.2 : Créer les JPA entities dans infrastructure

Déplacer les entités JPA actuelles vers `infrastructure/persistence/` :
- `Order.java` → `OrderJpaEntity.java`
- `Product.java` → `ProductJpaEntity.java`
- etc.

#### Étape 3.3 : Créer les ports repository

```java
package com.paymentplatform.organization.domain.repository;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    Optional<Order> findByReference(OrderReference reference);
    List<Order> findBySupplierId(OrganizationId supplierId);
    List<Order> findByShopId(OrganizationId shopId);
}

public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(ProductId id);
    List<Product> findBySupplierId(OrganizationId supplierId);
    boolean existsBySupplierIdAndSku(OrganizationId supplierId, SKU sku);
}
```

#### Étape 3.4 : Extraire la logique métier des use cases

Déplacer la logique de `StockService.createMovement()` dans l'agrégat `Product` :

```java
// Avant (StockService.java:101-133)
@Transactional
public StockMovementResponse createMovement(Long supplierId, StockMovementRequest request) {
    Product product = products.findByIdForUpdate(request.productId())...;
    int currentQty = product.getQuantity();
    switch (request.type()) {
        case "IN" -> product.setQuantity(currentQty + delta);
        case "OUT" -> { if (currentQty < delta) throw...; product.setQuantity(currentQty - delta); }
        case "ADJUSTMENT" -> product.setQuantity(delta);
    }
    products.save(product);
    // ...
}

// Après (dans l'agrégat Product)
public StockMovement applyMovement(MovementType type, int quantity, String reference, String notes) {
    switch (type) {
        case IN -> this.quantity = this.quantity.add(quantity);
        case OUT -> {
            if (this.quantity.value() < quantity) throw new InsufficientStockException(...);
            this.quantity = this.quantity.subtract(quantity);
        }
        case ADJUSTMENT -> this.quantity = new StockQuantity(quantity);
    }
    return StockMovement.create(this.id, this.supplierId, type, quantity, reference, notes);
}
```

### 3.4 Phase 4: Payment Service

#### Étape 4.1 : Éliminer la réflexion dans PaymentMapper

**Problème critique** (`PaymentMapper.java:36-58`) :
```java
var ctor = Payment.class.getDeclaredConstructor(...);
ctor.setAccessible(true);  // ANTI-PATTERN
return ctor.newInstance(...);
```

**Solution** : Ajouter un factory method `reconstruct` dans `Payment` :

```java
// Dans Payment.java
public static Payment reconstruct(Long id, PaymentReference reference, long shopId, long supplierId,
                                   Money money, PaymentStatus status, RejectionReason rejectionReason,
                                   long createdBy, Instant createdAt, Instant updatedAt, long version,
                                   List<PaymentEvent> events) {
    return new Payment(id, reference, shopId, supplierId, money, status, rejectionReason,
            createdBy, createdAt, updatedAt, version, events);
}
```

Puis simplifier le mapper :
```java
public Payment fromFields(PaymentJpaEntity entity, List<PaymentEvent> events) {
    return Payment.reconstruct(
        entity.getId(),
        new PaymentReference(entity.getReference()),
        entity.getShopId(),
        entity.getSupplierId(),
        Money.of(entity.getAmount(), entity.getCurrency()),
        PaymentStatus.from(entity.getStatus()),
        entity.getRejectionReason() != null ? new RejectionReason(entity.getRejectionReason()) : null,
        entity.getCreatedBy(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        entity.getVersion() != null ? entity.getVersion() : 0L,
        events
    );
}
```

#### Étape 4.2 : Créer des ports pour les dépendances infrastructure

```java
package com.paymentplatform.payment.application.port;

public interface OrganizationValidationPort {
    void validateShop(long shopId);
    void validateSupplier(long supplierId);
    void validateRelation(long shopId, long supplierId);
    Optional<String> getOrganizationName(long organizationId);
    Optional<String> getUserName(long userId);
}

public interface PaymentIndexingPort {
    void indexPayment(Payment payment);
    int reindexAll();
}
```

#### Étape 4.3 : Déplacer AuditActions vers le domaine

Créer `payment-service/.../domain/event/PaymentAuditActions.java` ou mieux, émettre les events depuis l'agrégat.

### 3.5 Phase 5: Shared Library

#### Étape 5.1 : Réduire le shared kernel

Le shared-lib devrait uniquement contenir :
- `DomainEvent` interface
- Value objects partagés (UserId, OrganizationId, RoleCode)
- Exceptions domain

Tout le reste (outbox, audit, security, web) devrait être dans chaque service ou dans un module infrastructure partagé séparé.

#### Étape 5.2 : Déplacer les domain events dans les services

Chaque service devrait définir ses propres événements dans son propre module, pas dans le shared-lib.

---

## 4. Architecture Cible

### 4.1 Diagramme des Bounded Contexts

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Payment Platform                               │
│                                                                     │
│  ┌────────────────────┐     ┌──────────────────────────────────┐    │
│  │  Identity Context  │     │     Organization Context         │    │
│  │  ────────────────  │     │     ──────────────────           │    │
│  │  User (AR)         │     │     Organization (AR)            │    │
│  │  ├─ Username (VO)  │     │     ├─ OrganizationName (VO)    │    │
│  │  ├─ Email (VO)     │     │     ├─ OrganizationType (VO)    │    │
│  │  ├─ PasswordHash   │     │     ├─ OrganizationStatus (VO)  │    │
│  │  └─ PhoneNumber    │     │     └─ SupplierShopRelation (E) │    │
│  │                    │     │                                  │    │
│  │  Events:           │     │     Order (AR)                   │    │
│  │  ├─ UserCreated    │     │     ├─ OrderReference (VO)      │    │
│  │  ├─ UserActivated  │     │     ├─ OrderStatus (VO)         │    │
│  │  └─ UserDisabled   │     │     ├─ Money (VO)               │    │
│  └────────┬───────────┘     │     └─ OrderLine (E)            │    │
│           │                  │                                  │    │
│           │                  │     Product (AR)                 │    │
│           │                  │     ├─ SKU (VO)                  │    │
│           │                  │     ├─ Money (VO)                │    │
│           │                  │     ├─ StockQuantity (VO)        │    │
│           │                  │     └─ ProductStatus (VO)        │    │
│           │                  │                                  │    │
│           │                  │     Events:                       │    │
│           │                  │     ├─ OrderCreated              │    │
│           │                  │     ├─ OrderConfirmed            │    │
│           │                  │     ├─ OrderDelivered            │    │
│           │                  │     ├─ SupplierCreated           │    │
│           │                  │     └─ ShopCreated               │    │
│           │                  └──────────────────────────────────┘    │
│           │                                                         │
│           │ events                                                  │
│           ▼                                                         │
│  ┌──────────────────┐     ┌──────────────────────────────────┐     │
│  │ Payment Context  │     │    Notification Context           │     │
│  │ ──────────────── │     │    ──────────────────            │     │
│  │ Payment (AR)     │     │    Notification (AR)              │     │
│  │ ├─ PaymentRef    │────▶│    ├─ NotificationType (VO)      │     │
│  │ ├─ Money (VO)    │     │    ├─ ReadStatus (VO)            │     │
│  │ ├─ PaymentStatus │     │    └─ Message (VO)               │     │
│  │ └─ RejectionReason│    │                                  │     │
│  │                  │     │    Events: (consommateur)         │     │
│  │ Events:          │     │    ├─ PaymentCreated → Notif     │     │
│  │ ├─ PaymentCreated│     │    ├─ OrderCreated → Notif       │     │
│  │ ├─ PaymentConfirmed│    │    └─ ...                        │     │
│  │ ├─ PaymentRejected│    └──────────────────────────────────┘     │
│  │ └─ PaymentCancelled│                                            │
│  └──────────────────┘                                              │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                    Shared Kernel                             │  │
│  │  ├─ DomainEvent (interface)                                  │  │
│  │  ├─ UserId, OrganizationId (VO)                              │  │
│  │  ├─ RoleCode (enum)                                          │  │
│  │  └─ DomainException, NotFoundException, etc.                 │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Interfaces entre Contextes

| De | Vers | Mécanisme | Contrat |
|----|------|-----------|---------|
| Identity → Organization | HTTP (via Gateway) | `OrganizationStatusPort` | `getOrganizationStatus(id)` |
| Organization → Identity | HTTP (interne) | `InternalUserCreationClient` | `createInternalUser(request)` |
| Payment → Organization | HTTP (via Gateway) | `OrganizationValidationPort` | `validateShop/Supplier/Relation()` |
| Organization → Payment | Events | `OrderCreatedEvent` | Via outbox |
| Payment → Notification | Events | `PaymentCreated/Confirmed/Rejected/Cancelled` | Via outbox |
| Organization → Notification | Events | `OrderCreated/Confirmed/...` | Via outbox |
| Identity → Notification | Events | `UserCreated/Activated/Disabled` | Via outbox |
| Organization → Identity | Events | `SupplierDisabled/ShopDisabled` | Via outbox |

### 4.3 Domain Events Flow

```
┌──────────────┐    events     ┌───────────┐    events     ┌──────────────────┐
│  Identity    │──────────────▶│           │──────────────▶│                  │
│  Service     │               │           │               │  Notification    │
└──────────────┘               │  RabbitMQ │               │  Service         │
                               │  (broker) │               │                  │
┌──────────────┐    events     │           │    events     │  Consomme tous   │
│  Organization│──────────────▶│           │──────────────▶│  les events et   │
│  Service     │               │           │               │  crée les        │
└──────────────┘               │           │               │  notifications   │
                               │           │               └──────────────────┘
┌──────────────┐    events     │           │
│  Payment     │──────────────▶│           │
│  Service     │               └───────────┘
└──────────────┘
```

---

## 5. Impact Analysis

### 5.1 Code Changes par Service

| Service | Fichiers à modifier | Fichiers à créer | Fichiers à supprimer | Effort estimé |
|---------|---------------------|------------------|----------------------|---------------|
| Notification Service | 5 | 4 | 0 | 2 jours |
| Identity Service | 3 | 2 | 0 | 1 jour |
| Organization Service | 15 | 12 | 0 | 5 jours |
| Payment Service | 8 | 3 | 0 | 3 jours |
| Shared Library | 4 | 2 | 0 | 2 jours |
| **Total** | **35** | **23** | **0** | **13 jours** |

### 5.2 Migration Steps

#### Phase 1 : Notification Service (2 jours)
1. Créer le modèle de domaine pur `Notification`
2. Créer le port `NotificationRepository`
3. Créer l'adapter JPA `JpaNotificationRepository`
4. Créer les use cases `GetNotificationsUseCase`, `MarkNotificationReadUseCase`
5. Créer les value objects `NotificationType`, `ReadStatus`, `Message`
6. Mettre à jour les consumers pour utiliser les use cases
7. Mettre à jour le controller pour utiliser les use cases
8. Tests

#### Phase 2 : Identity Service (1 jour)
1. Corriger `PhoneNumber` (validation correcte)
2. Créer le port `PasswordSetupEmailPort`
3. Créer l'adapter `JavaMailPasswordSetupEmailAdapter`
4. Mettre à jour `PasswordSetupUseCase` pour utiliser le port
5. Déplacer les domain events dans le service (optionnel)
6. Tests

#### Phase 3 : Organization Service (5 jours)
1. Créer les value objects manquants (`OrderId`, `OrderReference`, `SKU`, `StockQuantity`, `Money`, etc.)
2. Créer les modèles de domaine purs (`Order`, `Product`, `SupplierShopRelation`)
3. Créer les ports repository
4. Créer les JPA entities dans infrastructure
5. Créer les adapters repository
6. Extraire la logique métier des use cases vers les agrégats
7. Créer les domain events et les émettre depuis les agrégats
8. Mettre à jour les controllers et use cases
9. Tests

#### Phase 4 : Payment Service (3 jours)
1. Ajouter `Payment.reconstruct()` factory method
2. Simplifier `PaymentMapper` (supprimer la réflexion)
3. Créer les ports `OrganizationValidationPort`, `PaymentIndexingPort`
4. Créer les adapters
5. Supprimer `AuditActions` du domaine (émettre les events depuis l'agrégat)
6. Tests

#### Phase 5 : Shared Library (2 jours)
1. Déplacer `AuditActions` vers un module infrastructure
2. Créer `OutboxEventStore` comme port dans le domaine
3. Déplacer les domain events dans les services (optionnel, à valider)
4. Tests d'intégration

### 5.3 Risk Assessment

| Risque | Impact | Probabilité | Mitigation |
|--------|--------|-------------|------------|
| Régression dans les APIs REST | ÉLEVÉ | MOYENNE | Tests d'intégration complets avant/après |
| Perte de données lors de la migration JPA | ÉLEVÉ | FAIBLE | Pas de changement de schéma de base |
| Couplage cassé avec le frontend | MOYEN | FAIBLE | Les APIs REST ne changent pas |
| Performance dégradée (mappers supplémentaires) | FAIBLE | FAIBLE | Mappers simples, pas de réflexion |
| Complexité accrue du code | MOYEN | MOYENNE | Documenter l'architecture cible |

---

## 6. Recommendations Priorisées

### Top 10 actions immédiates

| Priorité | Action | Impact | Effort |
|----------|--------|--------|--------|
| 1 | **Séparer le Notification domain de JPA** | ÉLEVÉ | 2j |
| 2 | **Supprimer la réflexion dans PaymentMapper** | ÉLEVÉ | 0.5j |
| 3 | **Corriger PhoneNumber (validation record)** | MOYEN | 0.5j |
| 4 | **Créer des ports pour OrganizationValidationClient** | MOYEN | 1j |
| 5 | **Séparer Order/Product du JPA dans Organization** | ÉLEVÉ | 5j |
| 6 | **Ajouter du comportement aux agrégats** (Product.reserve()) | ÉLEVÉ | 3j |
| 7 | **Émettre les domain events depuis les agrégats** | MOYEN | 2j |
| 8 | **Créer des value objects manquants** (OrderId, SKU, etc.) | MOYEN | 2j |
| 9 | **Réduire le shared kernel** | MOYEN | 2j |
| 10 | **Ajouter des tests d'architecture** (ArchUnit) | HAUT | 1j |

### Quick Wins (1-2 jours)
- Corriger `PhoneNumber.java`
- Ajouter `Payment.reconstruct()` et simplifier `PaymentMapper`
- Créer le port `OrganizationValidationPort`
- Ajouter `Product.reserve()` et `Product.releaseReservation()`

### Valeur à long terme (1-2 semaines)
- Séparer complètement le domaine du JPA dans Organization Service
- Ajouter des domain events émis par les agrégats
- Créer des tests d'architecture avec ArchUnit pour empêcher les régressions

---

*Document généré le 2026-09-08*
*Dernière mise à jour : 2026-09-08*
