# Audit fonctionnel et sécurité — payment-platform

- **Date** : 2026-10-23
- **Périmètre** : frontend Angular/PWA et backend Spring Boot
- **Version auditée** : `release/2.0.0`, commit `fc49a32`
- **Type** : audit statique du code, lecture seule, sans test runtime
- **CI de référence** : run `39979219360`, statut `SUCCESS`
- **Environnement TEST vérifié** : `https://nextera.payment.tn`
- **Limites** : aucun scénario E2E runtime n'a été exécuté ; les tests existants n'ont pas été rejoués pendant cet audit. Les numéros de ligne correspondent au commit `fc49a32`.
- **Confidentialité** : aucun secret n'est reproduit dans ce document. `backend/recent.txt`, les fichiers `.env`, `.sonar-token` et `INFOS_VM_TEST.local.txt` n'ont pas été lus.

## 1. Synthèse exécutive

L'audit confirme que l'isolation multi-tenant est correcte sur plusieurs lectures unitaires, mais qu'elle est absente sur les mutations de commande, la création de commande, la création de paiement, les litiges et l'ajustement de solde. Un utilisateur authentifié disposant d'un UUID peut donc agir hors de son organisation.

Le second risque majeur est la chaîne des comptes auto-inscrits sans organisation : si un `SYSTEM_ADMIN` les active, ils peuvent se connecter et déclencher plusieurs fallbacks qui retournent les données de toutes les organisations.

Trois autres écarts sont confirmés : les montants des devis ne sont pas figés à l'acceptation, les contre-passations financières sont incomplètes après annulation/rejet, et les notifications FCM/emails sont mal ciblées ou invalides lorsque les canaux sont activés.

### Répartition

| Sévérité | Nombre | Thèmes |
|---|---:|---|
| Critique | 6 | Isolation tenant des mutations, création cross-tenant, comptes null-org, intégrité financière, devis |
| Majeur | 8 | Litiges, statistiques globales, notifications, WebPush/FCM, solde, annuaire/assignation livreur, RBAC frontend, référence externe |
| Mineur | 5 | Livraisons boutique, rôle fantôme, événement identité null-org, passkeys, documentation |

## 2. Matrice des rôles et des périmètres

### 2.1 Mutations backend et contrôle tenant

| Opération | Rôles autorisés | Contrôle tenant constaté | Constat |
|---|---|---|---|
| `POST /api/orders` | `SUPPLIER_ADMIN`, `SHOP_ADMIN`, `SHOP_MANAGER` | Non | C-02 |
| `PUT /api/orders/{id}` | `SUPPLIER_ADMIN`, `SHOP_ADMIN`, `SHOP_MANAGER` | Non | C-01 |
| `confirm`, `prepare`, `ready`, `assign-delivery` | `SUPPLIER_ADMIN` | Non | C-01 |
| `accept-delivery`, `confirm-delivery`, `deliver`, `delivery-reject` | `SUPPLIER_ADMIN`, `SUPPLIER_AGENT` | Non | C-01 |
| `accept`, `reject` réception | `SHOP_MANAGER`, `SHOP_ADMIN`, `SHOP_AGENT` | Non | C-01 |
| `cancel` commande | `SUPPLIER_ADMIN`, `SHOP_ADMIN`, `SHOP_MANAGER` | Non | C-01 |
| `reorder` | `SHOP_ADMIN`, `SHOP_MANAGER` | Non | C-01 |
| `PATCH /api/orders/{id}/external-reference` | `SUPPLIER_ADMIN`, `SHOP_ADMIN`, `SHOP_MANAGER`, `SYSTEM_ADMIN` | Non | H-08 |
| `POST /api/payments` | `SHOP_CREATE_PAYMENTS` | Non | C-03 |
| `confirm`, `reject` paiement | `SUPPLIER_MANAGE_PAYMENTS` | Fail-open si org nulle | C-04 |
| `cancel` paiement | `SHOP_CANCEL_PAYMENTS`, `SUPPLIER_MANAGE_PAYMENTS` | Fail-open si org nulle | C-04 |
| Litiges : création, liste, détail, messages, résolution | Rôles fournisseur/boutique/admin selon endpoint | Non | H-01 |
| `POST /api/balances/adjust` | `SYSTEM_ADMIN`, `SUPPLIER_ADMIN` | Fournisseur seul, boutique arbitraire | H-05 |
| `GET /api/payments/agent-summary` | `SUPPLIER_ADMIN`, `SYSTEM_ADMIN` | Fournisseur courant | H-07 |
| `GET /api/payments/stats` | `VIEW_PAYMENTS` | Global | H-02 |
| `GET /api/orders/shop-agents` | Rôles fournisseur/boutique/admin | Non | H-06 |

### 2.2 Écarts routes frontend / autorisations backend

| Parcours | Route frontend | Rôles UI | Backend | Résultat |
|---|---|---|---|---|
| Liste des commandes boutique | `shop/orders` | `SHOP_ADMIN`, `SHOP_AGENT` | Exclut `SHOP_AGENT` | 403 agent |
| Création de commande boutique | `shop/orders/create` | `SHOP_ADMIN`, `SHOP_AGENT` | Exclut `SHOP_AGENT` | 403 agent |
| Détail commande boutique | `shop/orders/:id` | `SHOP_ADMIN`, `SHOP_AGENT` | Exclut `SHOP_AGENT` | 403 agent |
| Litiges boutique | `shop/disputes`, `shop/disputes/:id` | `SHOP_ADMIN`, `SHOP_AGENT` | Exclut `SHOP_AGENT` sur liste/détail | 403 agent |
| Solde boutique | `shop/balance` | `SHOP_ADMIN`, `SHOP_AGENT` | `SHOP_ADMIN` uniquement | 403 agent |
| Livraisons boutique | `shop/deliveries` | `SHOP_ADMIN`, `SHOP_AGENT` | `myDeliveries` accepte les deux | OK si agent assigné |
| Commandes fournisseur | `supplier/orders` | `SUPPLIER_ADMIN`, `SUPPLIER_AGENT` | Liste OK ; actions réservées admin | 403 agent sur actions |
| Paiements par agent | `supplier/agent-payments` | `SUPPLIER_ADMIN`, `SUPPLIER_AGENT` | `agent-summary` réservé admin | 403 agent |
| Statistiques paiements | `payments/stats` | Tous les rôles | `VIEW_PAYMENTS` | 200, mais global |
| Détail livraison boutique | `shop/deliveries` | `SHOP_ADMIN`, `SHOP_AGENT` | Actions livraison fournisseur | 403 boutique |

## 3. Constats critiques

### C-01 — Mutations de commande sans isolation tenant

- **Sévérité** : Critique
- **Fichiers** : `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/OrderController.java:232-254,484-690,1084-1089` ; use cases `ConfirmOrderUseCase`, `PrepareOrderUseCase`, `AcceptOrderUseCase`, `AcceptDeliveryUseCase`, `ConfirmDeliveryUseCase`, `DeliverOrderUseCase`, `RejectOrderUseCase`, `DeliveryRejectOrderUseCase`, `CancelOrderUseCase`, `ReorderUseCase`, `UpdateOrderUseCase`.
- **Constat** : les endpoints de mutation chargent la commande par UUID et transmettent uniquement `current.userId()` au use case. Aucun ne compare `order.shopId()` ou `order.supplierId()` à `current.organizationId()`. `assignDeliveryAgent` (`OrderController.java:524-540`) ne vérifie ni l'organisation de la commande ni l'appartenance de l'agent. `updateExternalReference` (`:1084-1089`) ne transmet même pas l'acteur.
- **Impact** : un admin/agent authentifié peut confirmer, préparer, livrer, accepter, rejeter, annuler ou recréer une commande d'une autre organisation dès qu'il en connaît l'UUID. Les effets incluent le crédit de solde (`ConfirmOrderUseCase.java:69`), le décrément de stock (`AcceptOrderUseCase.java:65`), la création de paiement (`DeliverOrderUseCase.java:96-106`) et l'assignation d'un livreur étranger.
- **Recommandation** : charger la commande dans le use case et appliquer un garde tenant explicite selon l'action (`supplierId == actorOrg` ou `shopId == actorOrg`, ou `deliveryAgentId == actorUserId`). Traiter `SYSTEM_ADMIN` comme exception explicite. Ajouter des tests deux tenants pour chaque endpoint.

### C-02 — Création de commande cross-tenant

- **Sévérité** : Critique
- **Fichiers** : `OrderController.java:232-237` ; `CreateOrderUseCase.java:55-63,136-141`.
- **Constat** : `CreateOrderUseCase` valide l'existence des organisations et la relation fournisseur/boutique, mais ne vérifie pas que `request.shopId()` ou `request.supplierId()` correspond à l'organisation de l'acteur. Le rôle de l'acteur détermine seulement `source` (`SUPPLIER` ou `SHOP`). Une création `SUPPLIER` auto-confirme la commande et crédite le solde du fournisseur (`CreateOrderUseCase.java:136-141`).
- **Impact** : un admin fournisseur peut créer une commande au nom d'une boutique victime disposant d'une relation active avec lui, réserver le stock fournisseur et créditer le solde fournisseur. Un admin boutique peut créer une commande pour une autre boutique.
- **Recommandation** : vérifier `request.shopId().equals(actorOrg)` pour un acteur boutique et `request.supplierId().equals(actorOrg)` pour un acteur fournisseur, avant toute réservation ou écriture financière.

### C-03 — Création de paiement cross-tenant et idempotence globale

- **Sévérité** : Critique
- **Fichiers** : `backend/payment-service/src/main/java/com/paymentplatform/payment/interfaces/rest/PaymentController.java:72-80` ; `CreatePaymentUseCase.java:71-86,102`.
- **Constat** : `PaymentController.create` transmet `current.organizationId()` uniquement pour l'audit. `CreatePaymentUseCase` valide l'existence de la boutique, du fournisseur et leur relation, mais pas l'appartenance de la boutique à l'acteur. La recherche d'idempotence est globale (`findByIdempotencyKey`) et intervient avant ces validations (`:71-77`).
- **Impact** : un utilisateur `SHOP_CREATE_PAYMENTS` peut créer un paiement pour toute boutique ayant une relation active avec le fournisseur ciblé. Une clé d'idempotence connue peut retourner un paiement d'une autre organisation, avec un audit attribué à l'organisation de l'acteur.
- **Recommandation** : vérifier `request.shopId().equals(actorOrg)` pour les rôles boutique ; réserver l'idempotence au périmètre de l'organisation ; ne jamais retourner un paiement existant avant le contrôle tenant.

### C-04 — Comptes sans organisation et fallbacks globaux

- **Sévérité** : Critique
- **Fichiers** : `backend/identity-service/src/main/java/com/paymentplatform/identity/application/usecase/RegisterUseCase.java:84-88` ; `User.java:37` ; `UserStatusUseCase.java:52-57` ; `AuthUseCase.java:60-65` ; `OrderController.java:276-293,387-393,442-448,856-864` ; `PaymentController.java:139-165` ; `ConfirmPaymentUseCase.java:39-42` ; `RejectPaymentUseCase.java:41-44` ; `CancelPaymentUseCase.java:39-42` ; `QuoteController.java:121-125,180-184`.
- **Constat** : l'auto-inscription accepte `SUPPLIER_ADMIN`, `SUPPLIER_AGENT`, `SHOP_ADMIN` et `SHOP_AGENT`, crée un compte `DISABLED` avec `organizationId = null`, puis `activateUser` active le compte sans rattachement. Aucun endpoint de rattachement d'organisation n'a été identifié. `AuthUseCase` ne contrôle le statut de l'organisation que si elle existe. Une fois activé, un compte null-org avec rôle admin :
  - obtient `findAll` sur `listOrders`/`recent`/`search` et l'export CSV ;
  - obtient `executeAll` sur la liste des paiements et les statistiques globales ;
  - passe les contrôles `organizationId != null` de confirm/reject/cancel paiement ;
  - contourne le contrôle de `QuoteController.getRequest` et `downloadQuotePdf`, car la condition d'org est ignorée quand elle est nulle.
  Un compte null-org `SUPPLIER_AGENT` n'obtient pas `findAll` sur les commandes, mais conserve l'accès global aux paiements et aux devis via les branches ci-dessus.
- **Impact** : fuite de données inter-organisations et actions financières non scopées. La chaîne est conditionnée à l'activation par un `SYSTEM_ADMIN`, mais elle est silencieuse et aucun contrôle de cohérence ne la bloque.
- **Recommandation** : refuser l'activation d'un compte sans organisation ; ajouter un workflow explicite de rattachement ; supprimer les fallbacks `findAll`/`executeAll` pour les rôles tenant ; rendre les contrôles de paiement fail-closed. Auditer les comptes null-org existants.

### C-05 — Contre-passations financières incomplètes et paiement ASAP

- **Sévérité** : Critique
- **Fichiers** : `ConfirmOrderUseCase.java:69` ; `RejectOrderUseCase.java:47-76` ; `CancelOrderUseCase.java:44-60` ; `DeliverOrderUseCase.java:96-106` ; `RejectPaymentUseCase.java:37-56` ; `PaymentClient.java:43-47`.
- **Constat** : `ConfirmOrderUseCase` et `AcceptQuoteUseCase` créditent le solde fournisseur. `RejectOrderUseCase` restitue le stock mais ne débite pas le solde et n'annule pas le paiement. `CancelOrderUseCase` ne compense pas le crédit. `RejectPaymentUseCase` rejette le paiement sans modifier la commande ni le solde. Pour une commande ASAP, `DeliverOrderUseCase` peut créer le paiement dès la livraison, avant l'acceptation boutique ; `accept-asap` le crée aussi de façon idempotente (`asap-<orderId>`).
- **Impact** : après une annulation ou un rejet, le fournisseur peut conserver un crédit de solde et un paiement encore confirmable. Le paiement ASAP peut être traité avant l'acceptation de la réception par la boutique.
- **Recommandation** : définir une machine à états financière unique et idempotente ; contre-passer le solde et annuler le paiement sur rejet/annulation ; ne créer le paiement ASAP qu'après l'événement métier validé ; verrouiller la double exécution.

### C-06 — Devis accepté à un prix non figé et sans contrôle de version

- **Sévérité** : Critique
- **Fichiers** : `AcceptQuoteUseCase.java:76-84,97-131` ; `SupplierQuote.java:8-23` ; `SendRequestQuoteUseCase.java:160-167`.
- **Constat** : `SupplierQuote` documente un « contenu figé » et porte `version`, `subtotal`, `taxAmount`, `total`, `linesJson`. `AcceptQuoteUseCase` récupère le devis par UUID, vérifie seulement que la demande est `QUOTED`, puis recalcule chaque ligne avec `product.getUnitPrice()` courant. Le prix unitaire et les totaux du devis ne sont pas utilisés ; seule la remise en pourcentage est reprise, mais appliquée au prix courant. La version n'apparaît que dans les notes. Aucun contrôle ne vérifie que le devis accepté est la dernière version.
- **Impact** : une commande et un crédit de solde sont créés à un prix différent de l'offre acceptée. Un ancien devis peut être accepté après qu'une version plus récente a été émise.
- **Recommandation** : créer la commande depuis les montants figés du devis ; vérifier que `quote.version` est la version courante et que la demande n'a pas déjà été acceptée ; conserver un snapshot immuable des lignes et totaux.

## 4. Constats majeurs

### H-01 — Litiges sans isolation tenant

- **Sévérité** : Majeur
- **Fichiers** : `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/DisputeController.java:40-95` ; `CreateDisputeUseCase.java:32-44` ; `AddDisputeMessageUseCase` ; `ResolveDisputeUseCase.java:29-41`.
- **Constat** : `listDisputes` retourne `findAll()` ou `findByStatus...` sans filtre d'org. `getDisputesByOrder` et `getDispute` chargent par UUID sans vérifier que la commande/litige appartient à l'acteur. `createDispute` accepte n'importe quelle commande rejetée. `addMessage` et `resolveDispute` n'ont aucun contrôle d'appartenance. Les rôles `SUPPLIER_ADMIN`/`SHOP_ADMIN` peuvent résoudre n'importe quel litige.
- **Impact** : lecture inter-organisations des litiges et messages, création de litige sur une commande étrangère, modification de statut par un admin d'une autre organisation.
- **Recommandation** : charger le litige/commande et vérifier l'appartenance à l'org avant toute lecture ou mutation ; filtrer les listes par org ; ajouter un test deux tenants.

### H-02 — Statistiques de paiement globales

- **Sévérité** : Majeur
- **Fichiers** : `PaymentController.java:161-165` ; `ListPaymentsUseCase.java:161-171` ; `payment-platform-ui/src/app/app-routing.module.ts:93`.
- **Constat** : `GET /api/payments/stats` est accessible à tout rôle `VIEW_PAYMENTS` et retourne les compteurs de toute la plateforme. La route frontend `payments/stats` est ouverte à tous les rôles. `SHOP_AGENT` possède `VIEW_PAYMENTS`.
- **Impact** : fuite du volume et de la répartition des paiements de toutes les organisations.
- **Recommandation** : soit réserver l'endpoint à `SYSTEM_ADMIN`, soit calculer les statistiques par organisation avec le même périmètre que la liste.

### H-03 — Notifications FCM et emails cassées lorsque activées

- **Sévérité** : Majeur
- **Fichiers** : `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/messaging/OrderEventConsumer.java:102,118,165,167,183,302,321,339` ; `IdentityEventConsumer.java:66-76` ; `EmailNotificationService.java:36-55,80-92` ; `PushNotificationService.java:34-54`.
- **Constat** : les événements commande/devis appellent `pushService.sendToUser(shopId/supplierId, ...)` avec des UUID d'organisation, alors que `PushNotificationService` recherche les tokens FCM par `user_id` (`:46`). Aucun token n'est trouvé, donc le push FCM est perdu. Les emails commande, livraison et invitation agent sont appelés avec `to = null` (`OrderEventConsumer.java:118,165`, `IdentityEventConsumer.java:76`) ; `MimeMessageHelper.setTo(null)` est invalide et l'échec n'est pas couvert par le `catch (MessagingException)` (`EmailNotificationService.java:90-92`). Les notifications in-app restent persistées par organisation (`OrderEventConsumer.java:90-113`) et le WebPush par organisation est correct (`WebPushSender.java:71-80`).
- **Impact** : perte silencieuse des notifications push FCM et des emails métier lorsque `app.push.enabled` ou `app.mail.enabled` est activé.
- **Recommandation** : résoudre les destinataires user/email côté consommateur ; utiliser `sendToOrganization` pour le WebPush et des IDs utilisateur réels pour FCM ; valider les destinataires ; élargir la gestion d'erreur et ajouter des métriques.

### H-04 — Réassignation d'abonnements et SSRF WebPush

- **Sévérité** : Majeur
- **Fichiers** : `WebPushController.java:40-63` ; `WebPushSubscriptionService.java:26-39` ; `WebPushSender.java:86-108,145-155` ; `FcmTokenController.java:27-46`.
- **Constat** : l'endpoint WebPush est seulement validé par `@NotBlank`/`@Size(max=2048)`, sans contrôle de schéma ou d'hôte. `register` réassigne un endpoint existant au user/org courant (`:30-33`). `WebPushSender` envoie ensuite une requête serveur vers cet endpoint. Le `sanitizeUrl` de `WebPushSender` protège uniquement le deep-link, pas l'endpoint push. `FcmTokenController` réassigne de la même façon un token existant à l'utilisateur courant.
- **Impact** : un endpoint interne peut être enregistré et appelé par le serveur (SSRF potentiel) ; si un endpoint/token fuite, un autre compte peut s'approprier l'abonnement ou le token et détourner les notifications.
- **Recommandation** : valider l'endpoint (HTTPS, hôte autorisé, résolution DNS) ; ne jamais réassigner un abonnement/token existant sans preuve de possession ; lier l'abonnement à l'utilisateur et à l'organisation ; ajouter une limite et une journalisation.

### H-05 — Ajustement de solde sur une boutique arbitraire

- **Sévérité** : Majeur
- **Fichiers** : `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/BalanceController.java:63-79`.
- **Constat** : l'acteur `SUPPLIER_ADMIN` est bien borné à son propre `supplierId`, mais `request.shopId()` n'est soumis à aucune vérification de relation. Le montant doit être positif, mais la boutique cible est libre.
- **Impact** : un fournisseur peut créditer le solde de n'importe quelle boutique pour son propre compte fournisseur, avec un impact comptable et fiscal.
- **Recommandation** : vérifier l'existence de la relation active fournisseur/boutique et, si nécessaire, le sens métier de l'ajustement ; journaliser l'acteur et la cible.

### H-06 — Énumération des utilisateurs et assignation arbitraire d'un livreur

- **Sévérité** : Majeur
- **Fichiers** : `OrderController.java:344-370,524-540`.
- **Constat** : `GET /api/orders/shop-agents?shopId=...` appelle `identityClient.getUsersByOrganization(shopId)` sans vérifier que la boutique appartient à l'acteur ou à son périmètre. `assignDeliveryAgent` accepte n'importe quel `agentId` sans vérifier l'org, le rôle ou la relation.
- **Impact** : énumération des noms/rôles des utilisateurs de toute boutique ; assignation d'un utilisateur étranger comme livreur, ce qui peut lui donner accès à la commande via `myDeliveries`/`getOrder` et à la mise à jour GPS.
- **Recommandation** : filtrer `shop-agents` par org/relation ; vérifier que l'agent appartient au fournisseur ou à la boutique et possède le rôle attendu ; journaliser l'assignation.

### H-07 — Incohérences RBAC frontend/backend

- **Sévérité** : Majeur
- **Fichiers** : `payment-platform-ui/src/app/app-routing.module.ts:92-125` ; `supplier/order-management/order-management.component.html:93-113` ; `supplier/order-management/order-management.component.ts:571-597` ; `supplier/delivery-management/delivery-management.component.html:12-87,598-613` ; `supplier/agent-payments/agent-payments.component.ts:58` ; `PaymentController.java:196-197`.
- **Constat** : les routes et composants exposent des parcours que le backend refuse : `SHOP_AGENT` sur commandes/création/détail/litiges/solde ; `SUPPLIER_AGENT` sur les actions de commande ; `SUPPLIER_AGENT` sur `agent-payments` ; la boutique peut voir les sections et boutons fournisseur de livraison. `canConfirm`/`canPrepare`/`canReady`/`canAssign` ne dépendent que du statut, pas du rôle.
- **Impact** : erreurs 403 fréquentes, confusion opérationnelle, et exposition visuelle d'actions interdites. Le backend reste l'autorité, mais l'UX ne reflète pas la matrice réelle.
- **Recommandation** : générer la matrice de rôles côté UI depuis une source unique ; masquer les actions par rôle ; ajouter des tests E2E de navigation et d'action pour chaque rôle.

### H-08 — Référence électronique externe modifiable hors tenant

- **Sévérité** : Majeur
- **Fichiers** : `OrderController.java:1084-1089` ; `UpdateOrderExternalReferenceUseCase.java`.
- **Constat** : l'endpoint `PATCH /api/orders/{id}/external-reference` ne récupère pas `CurrentUser` et ne contrôle ni l'org ni l'appartenance de la commande. Tout `SUPPLIER_ADMIN`, `SHOP_ADMIN`, `SHOP_MANAGER` ou `SYSTEM_ADMIN` peut modifier la référence TTN/TEJ de n'importe quelle commande.
- **Impact** : altération d'une donnée fiscale/facture cross-tenant, sans trace d'acteur au niveau du use case.
- **Recommandation** : appliquer le garde tenant, limiter l'écriture au fournisseur émetteur, auditer l'acteur et la valeur précédente.

## 5. Constats mineurs

### M-01 — `listDeliveries` boutique utilise le fournisseur

- **Sévérité** : Mineur
- **Fichiers** : `OrderController.java:306-336`.
- **Constat** : pour un `SHOP_ADMIN` sans `agentId`, la branche `current.organizationId() != null` utilise `findBySupplierId(current.organizationId())` au lieu de `findByShopId`. L'UI utilise `myDeliveries`, qui est correct.
- **Impact** : endpoint secondaire potentiellement vide ou incohérent pour une boutique.
- **Recommandation** : utiliser `findByShopId` pour les rôles boutique et `findBySupplierId` pour les rôles fournisseur.

### M-02 — Rôle `SHOP_MANAGER` fantôme

- **Sévérité** : Mineur
- **Fichiers** : `backend/shared-lib/src/main/java/com/paymentplatform/shared/domain/model/RoleCode.java:7-9` ; `OrderController.java` ; `DisputeController.java` ; documentation.
- **Constat** : `SHOP_MANAGER` est référencé dans des `@PreAuthorize` et la documentation, mais absent de `RoleCode` et de `PermissionCatalog`.
- **Impact** : branches mortes, documentation trompeuse, futurs endpoints uniquement `SHOP_MANAGER` inaccessibles.
- **Recommandation** : supprimer les références ou ajouter le rôle avec ses permissions, puis aligner la documentation.

### M-03 — Événement identité sans organisation

- **Sévérité** : Mineur
- **Fichiers** : `IdentityEventConsumer.java:66-76`.
- **Constat** : `UUID.fromString(event.get("organizationId").asText())` est exécuté sans garde null. Pour un agent auto-inscrit sans org, le champ peut être absent/null et provoquer une NPE ou une `IllegalArgumentException` ; l'invitation email est aussi envoyée à `null`.
- **Impact** : traitement d'événement en échec et notification agent non délivrée pour les comptes sans org.
- **Recommandation** : garder le champ optionnel, ignorer ou router vers un workflow d'attente, et valider l'email avant envoi.

### M-04 — Passkeys désactivés et résidus UI

- **Sévérité** : Mineur
- **Fichiers** : `payment-platform-ui/src/app/core/passkeys.config.ts` ; composants login/security.
- **Constat** : `PASSKEYS_ENABLED=false` ; les parcours passkey sont masqués mais des libellés/sections `SECURITY` subsistent.
- **Impact** : dette produit et confusion limitée ; pas de vulnérabilité directe.
- **Recommandation** : décider du maintien de la fonctionnalité et aligner l'UI/la documentation.

### M-05 — Documentation et matrice de rôles non alignées

- **Sévérité** : Mineur
- **Fichiers** : `docs/FONCTIONNALITES.md` ; `docs/TECHNIQUE.md` ; `docs/PASSKEYS_PUSH.md`.
- **Constat** : la documentation mentionne des rôles/flux qui ne correspondent plus aux contrôleurs (notamment `SHOP_MANAGER`, les actions agent et les parcours de livraison).
- **Impact** : risque de reproduire les écarts lors des prochains développements.
- **Recommandation** : mettre à jour la documentation à partir de la matrice backend validée.

## 6. Constats corrigés par rapport aux premières hypothèses

- Les lectures unitaires de commande sont bien scopées : `getOrder`, `getOrderByReference`, commentaires, factures, TEIF et certificat de retenue vérifient l'org ou l'agent livreur. L'IDOR confirmé concerne les mutations, pas ces lectures.
- `myDeliveries` fonctionne pour `SHOP_AGENT` si l'agent est assigné : la branche finale utilise `findByDeliveryAgentId(current.userId())`.
- L'ajustement de solde n'est pas cross-supplier : le fournisseur est borné à son org ; seul le `shopId` cible est arbitraire.
- Le WebPush par organisation est correct et les notifications in-app sont persistées par organisation. Le défaut d'adressage concerne FCM (org ID au lieu de user ID) et les emails null, pas l'ensemble des notifications.
- Le paiement automatique n'est pas créé pour toutes les commandes : `DeliverOrderUseCase` le crée uniquement si `order.isAsapPayment()`. Le point à confirmer est le timing avant acceptation boutique et l'absence de contre-passation au rejet.

## 7. Plan de remédiation

### P0 — Bloquant avant mise en production

1. Introduire un contexte acteur/tenant central et l'appliquer dans les use cases, pas seulement dans les contrôleurs.
2. Refuser toute activation de compte sans organisation et ajouter un workflow de rattachement.
3. Supprimer les fallbacks globaux null-org et rendre confirm/reject/cancel paiement fail-closed.
4. Ajouter le contrôle tenant sur les mutations commande, la création commande/paiement, les litiges et l'ajustement de solde.
5. Vérifier l'agent assigné (org et rôle) et filtrer `shop-agents`.

### P1 — Intégrité métier

1. Contre-passer solde et paiement sur annulation/rejet, avec idempotence.
2. Revoir le timing du paiement ASAP et le rendre cohérent avec l'acceptation boutique.
3. Construire la commande depuis les montants figés du devis et vérifier la dernière version.
4. Corriger les destinataires FCM/email et la gestion d'erreur.
5. Valider les endpoints WebPush et empêcher la réassignation non prouvée.
6. Scoper les statistiques de paiement ou les réserver à `SYSTEM_ADMIN`.

### P2 — Qualité et cohérence

1. Aligner les routes/nav/actions frontend avec la matrice backend.
2. Corriger `listDeliveries`, le rôle `SHOP_MANAGER` et la documentation.
3. Décider du maintien des passkeys.
4. Ajouter des tests de non-régression multi-tenant et des métriques d'échec de notification.

## 8. Validation recommandée

### Tests d'intégration à ajouter

1. **Deux tenants** : token org A sur commande/paiement/litige org B → 403/404 pour chaque mutation.
2. **Création** : commande/paiement avec un `shopId`/`supplierId` étranger → 403.
3. **Null-org** : auto-inscription, activation admin, puis listes commandes/paiements/quotes/disputes → aucun accès global.
4. **Devis** : prix produit modifié après émission, acceptation d'un ancien devis → prix figé ou rejet explicite.
5. **Finance** : confirmer puis annuler/rejeter → solde et paiement compensés, une seule fois.
6. **Notifications** : FCM adressé au user, email avec destinataire réel, WebPush endpoint validé et non réassignable.
7. **Frontend** : navigation et actions par rôle ; aucun bouton interdit visible ; aucun 403 sur un parcours autorisé.

### Commandes existantes

Frontend, depuis `payment-platform-ui/` :

```powershell
npm run test:ci
npm run build:dev
```

Backend, depuis `backend/` :

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-27.0.1'
.\mvnw.cmd -pl organization-service -am test
```

Pour les correctifs concernés, étendre à `payment-service`, `notification-service` et `identity-service`.

### Résultats connus

Les résultats antérieurs disponibles sont : frontend `1101/1101`, backend organisation `232/232`, build production OK, CI `SUCCESS`. Ces résultats n'ont pas été rejoués pendant cet audit statique.

## 9. Fichiers clés examinés

- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/OrderController.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/DisputeController.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/BalanceController.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/interfaces/rest/QuoteController.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/AcceptQuoteUseCase.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/CreateOrderUseCase.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/ConfirmOrderUseCase.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/RejectOrderUseCase.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/CancelOrderUseCase.java`
- `backend/organization-service/src/main/java/com/paymentplatform/organization/application/usecase/DeliverOrderUseCase.java`
- `backend/payment-service/src/main/java/com/paymentplatform/payment/interfaces/rest/PaymentController.java`
- `backend/payment-service/src/main/java/com/paymentplatform/payment/application/usecase/CreatePaymentUseCase.java`
- `backend/payment-service/src/main/java/com/paymentplatform/payment/application/usecase/ConfirmPaymentUseCase.java`
- `backend/payment-service/src/main/java/com/paymentplatform/payment/application/usecase/RejectPaymentUseCase.java`
- `backend/payment-service/src/main/java/com/paymentplatform/payment/application/usecase/CancelPaymentUseCase.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/messaging/OrderEventConsumer.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/messaging/IdentityEventConsumer.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/email/EmailNotificationService.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/push/PushNotificationService.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/push/WebPushSender.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/application/WebPushSubscriptionService.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/rest/WebPushController.java`
- `backend/notification-service/src/main/java/com/paymentplatform/notification/infrastructure/rest/FcmTokenController.java`
- `backend/identity-service/src/main/java/com/paymentplatform/identity/application/usecase/RegisterUseCase.java`
- `backend/identity-service/src/main/java/com/paymentplatform/identity/application/usecase/UserStatusUseCase.java`
- `backend/identity-service/src/main/java/com/paymentplatform/identity/application/usecase/AuthUseCase.java`
- `backend/shared-lib/src/main/java/com/paymentplatform/shared/domain/model/RoleCode.java`
- `backend/shared-lib/src/main/java/com/paymentplatform/shared/domain/security/PermissionCatalog.java`
- `payment-platform-ui/src/app/app-routing.module.ts`
- `payment-platform-ui/src/app/supplier/order-management/order-management.component.ts`
- `payment-platform-ui/src/app/supplier/order-management/order-management.component.html`
- `payment-platform-ui/src/app/supplier/delivery-management/delivery-management.component.html`
- `payment-platform-ui/src/app/supplier/agent-payments/agent-payments.component.ts`
- `payment-platform-ui/src/app/shop/order-list/`
- `payment-platform-ui/src/app/shop/order-detail/`
- `payment-platform-ui/src/app/shop/dispute-list/`
- `payment-platform-ui/src/app/shop/balance-view/`
- `payment-platform-ui/src/app/core/passkeys.config.ts`
