# Dashboard des Améliorations Fonctionnelles - Payment Platform

> Backlog de prompts historique — antérieur aux phases 0/1, à ne pas exécuter tel quel
> (plusieurs items B/M sont soldés). Suivi à jour : [COMPTE_RENDU_EXPERT.md](COMPTE_RENDU_EXPERT.md).
> Index : [docs/README.md](docs/README.md).

> Review fonctionnel expert — Chaque amélioration est formatée comme un **prompt** prêt à exécuter.

---

## Statut global

| Catégorie | Total | P0 Critique | P1 Haute | P2 Moyenne | P3 Basse |
|-----------|-------|-------------|----------|------------|----------|
| 🔴 Gestion des commandes | 12 | 2 | 4 | 4 | 2 |
| 💰 Paiements | 8 | 1 | 3 | 3 | 1 |
| 🚚 Livraison | 9 | 1 | 3 | 3 | 2 |
| 📊 Reporting & Analytics | 7 | 0 | 2 | 3 | 2 |
| 🔔 Notifications | 5 | 0 | 2 | 2 | 1 |
| 👤 Gestion utilisateurs | 6 | 0 | 1 | 3 | 2 |
| 🏪 Catalogue & Stock | 8 | 0 | 2 | 4 | 2 |
| 🎨 UX/UI Globale | 10 | 0 | 3 | 4 | 3 |
| **TOTAL** | **65** | **4** | **20** | **26** | **15** |

---

## 🔴 P0 — Critique (Bloquant)

---

### P0-01 : Création de commande impossible sans produit disponible

**Problème :** Quand un fournisseur n'a aucun produit avec stock suffisant, la page de création de commande boutique affiche un formulaire vide sans aucun feedback. L'utilisateur ne comprend pas pourquoi il ne peut pas commander.

**Prompt :**
```
Dans le composant OrderCreateComponent (shop/order-create), ajouter un état "Aucun produit disponible" quand le catalogue est vide ou quand aucun produit n'a de stock suffisant (quantity - reservedQty < 1). Afficher un message explicatif avec un bouton pour contacter le fournisseur. Si des produits existent mais sans stock, les afficher en gris avec la mention "Rupture de stock" au lieu de les masquer. Ajouter aussi un compteur "X produit(s) disponible(s) sur Y" au-dessus du formulaire.
```

---

### P0-02 : Pas de validation de montant négatif dans les paiements

**Problème :** Le formulaire de création de paiement n'empêche pas de saisir un montant négatif ou zéro. Le backend accepte aussi ces valeurs. Un paiement de -500 TND pourrait être créé.

**Prompt :**
```
Dans PaymentCreateComponent (payment-create), ajouter une validation côté client : montant minimum = 0.01, maximum = 999999.99. Côté backend, dans CreatePaymentRequest.java, ajouter @DecimalMin("0.01") @DecimalMax("999999.99") sur le champ amount. Dans Payment.java (domain model), ajouter une validation dans la factory create() qui rejette les montants <= 0. Tester ces validations dans PaymentControllerTest et PaymentCreateComponent.spec.ts.
```

---

### P0-03 : Annulation de commande impossible après livraison acceptée

**Problème :** Un admin boutique peut annuler une commande même si elle est déjà ACCEPTÉE (livraison terminée et paiement potentiellement créé). Le bouton "Annuler" devrait être désactivé pour les statuts DELIVERED, ACCEPTED, et pour les commandes avec paiement confirmé.

**Prompt :**
```
Dans OrderManagementComponent, modifier la méthode canCancel() pour retourner false quand le statut est DELIVERED, ACCEPTED, ou REJECTED. Ajouter une vérification côté backend dans CancelOrderUseCase.java : si le statut est ACCEPTED ou DELIVERED, lever une ConflictException("Impossible d'annuler une commande déjà acceptée/livrée"). Aussi vérifier si un paiement CONFIRMÉ existe pour cette commande via PaymentClient et bloquer l'annulation si c'est le cas.
```

---

### P0-04 : Dashboard ne s'affiche pas pour les agents livreur

**Problème :** La page Dashboard n'est pas accessible aux SUPPLIER_AGENT. La route `/dashboard` n'est pas dans la liste des routes autorisées pour ce rôle, ou le composant ne charge pas les bonnes données pour un agent.

**Prompt :**
```
Dans le routing de l'application, vérifier que /dashboard est accessible à tous les rôles authentifiés. Dans DashboardComponent, adapter l'affichage selon le rôle : pour SUPPLIER_AGENT, afficher uniquement "Mes livraisons en cours" et "Statistiques de livraison". Pour SUPPLIER_ADMIN, afficher les commandes, ventes, et alertes stock. Pour SHOP_ADMIN, afficher les commandes reçues et les paiements. Ajouter un champ `role` dans le dashboard qui conditionne les sections affichées.
```

---

## 🔴 P1 — Haute Priorité

---

### P1-01 : Workflow de commande incomplet — pas de suivi du préparateur

**Problème :** Quand un admin confirme une commande (DRAFT→CONFIRMED) puis la passe en préparation (CONFIRMED→PREPARING), on ne sait pas QUI a fait chaque action. Le createdBy est enregistré mais pas les acteurs intermédiaires.

**Prompt :**
```
Dans chaque UseCase (ConfirmOrderUseCase, PrepareOrderUseCase, DeliverOrderUseCase, AcceptOrderUseCase, CancelOrderUseCase, RejectOrderUseCase, AcceptDeliveryUseCase, ConfirmDeliveryUseCase), l'actorUserId est déjà passé. Vérifier que le OrderEvent créé contient bien l'userId de l'acteur. Dans OrderController.buildOrderResponse(), résoudre les noms des acteurs intermédiaires depuis les events et les ajouter à OrderResponse : confirmedByName, preparedByName, readyByName, assignedDeliveryByName, acceptedDeliveryByName, confirmedDeliveryByName, deliveredByName. Ajouter ces champs au record OrderResponse et au modèle Order Angular.
```

---

### P1-02 : Pas de possibility de modifier une commande brouillon

**Problème :** Une fois une commande créée en statut DRAFT, il est impossible de modifier les articles, les quantités, ou les notes. La seule option est d'annuler et recréer.

**Prompt :**
```
Créer un nouvel endpoint PUT /api/orders/{id} qui permet de modifier une commande en statut DRAFT uniquement. Côté backend : créer UpdateOrderRequest.java avec champs notes, asapPayment, items (liste de OrderItemRequest). Créer UpdateOrderUseCase.java qui vérifie le statut=DRAFT avant modification. Côté frontend : ajouter un bouton "Modifier" dans OrderManagementComponent pour les commandes DRAFT, qui ouvre un formulaire pré-rempli avec les données actuelles. Ajouter la méthode update() dans OrderService. Ajouter les tests correspondants.
```

---

### P1-03 : Facture/Reçu non générable ✅ Réalisé

**Problème :** Il n'existe aucun moyen de générer ou télécharger un reçu/facture PDF pour une commande ou un paiement confirmé.

**Prompt :**
```
Côté backend : créer un endpoint GET /api/orders/{id}/invoice qui génère un PDF avec les détails de la commande (référence, dates, articles, totaux, mentions légales). Utiliser une librairie comme OpenPDF ou iText. Côté frontend : ajouter un bouton "Télécharger facture" dans OrderDetailComponent (shop) et OrderManagementComponent (supplier) pour les statuts DELIVERED/ACCEPTED. Pour les paiements, ajouter le même bouton dans PaymentDetailComponent pour les statuts CONFIRMED. Stocker les PDF générés dans un dossier partagé ou un objet S3.
```

**Réalisé :**
- Backend : `GET /api/orders/{id}/invoice` (`OrderController`) et `GET /api/payments/{id}/invoice` (`PaymentController`) avec OpenPDF (`InvoicePdfService`, `PaymentInvoicePdfService`).
- Contrôle de statut backend : 409 si la commande n'est pas DELIVERED/ACCEPTED ou le paiement non CONFIRMED.
- Frontend : boutons « Télécharger facture » dans `OrderDetailComponent` (DELIVERED/ACCEPTED), `OrderManagementComponent` (liste + détail, DELIVERED/ACCEPTED) et `PaymentDetailComponent` (CONFIRMED).
- Tests unitaires PDF : `InvoicePdfServiceTest`, `PaymentInvoicePdfServiceTest`.
- Note : les PDF sont générés à la volée (pas de stockage S3 — non nécessaire pour l'usage actuel).

---

### P1-04 : Gestion des litiges — pas de processus de réclamation

**Problème :** Quand une boutique rejette une livraison (REJECTED), il n'y a aucun processus de médiation ou de réclamation. Le fournisseur ne sait pas pourquoi et ne peut pas répondre.

**Prompt :**
```
Créer un module de litiges (disputes) : nouvelle entité Dispute liée à une Order, avec champs reason, status (OPEN/IN_PROGRESS/RESOLVED/CLOSED), messages (liste de DisputeMessage avec sender, content, timestamp). Côté backend : créer DisputeController, CreateDisputeUseCase, AddDisputeMessageUseCase, ResolveDisputeUseCase. Côté frontend : ajouter un bouton "Ouvrir un litige" dans OrderDetailComponent (shop) quand le statut est REJECTED. Créer un composant DisputeDetailComponent avec les messages. Notifications AMQP pour informer les deux parties.
```

---

### P1-05 : Absence de tableau de bord financier pour les fournisseurs

**Problème :** Les fournisseurs n'ont aucune visibilité sur leurs revenus, marges, ou l'évolution de leurs ventes dans le temps.

**Prompt :**
```
Créer un endpoint GET /api/reports/supplier-financial qui retourne : chiffre d'affaires par mois (12 derniers mois), nombre de commandes par statut, top 10 produits vendus, montant total des paiements confirmés/en attente/rejetés. Côté frontend : créer un composant SupplierFinancialComponent dans /dashboard/supplier/financial avec des graphiques (utiliser Chart.js ou ng2-charts) : courbe CA mensuel, camembert statuts commandes, barres top produits. Ajouter un onglet "Finance" dans la sidebar pour SUPPLIER_ADMIN.
```

---

### P1-06 : Pas de gestion des retours produits

**Problème :** Quand une boutique rejette une livraison, les produits ne sont pas réintégrés en stock automatiquement.

**Prompt :**
```
Dans RejectOrderUseCase.java, quand le statut passe à REJECTED, créer automatiquement des StockMovement de type RETURN pour chaque item de la commande, avec une quantité positive (réintégration en stock). Ajouter un champ `returnReason` à l'entité Order. Côté frontend : afficher un badge "Retour en stock" dans l'historique des mouvements de stock quand un mouvement est lié à un retour.
```

---

### P1-07 : Notifications push manquantes pour les livraisons

**Problème :** Les notifications sont uniquement visibles dans l'application. Un livreur qui n'a pas l'application ouverte ne sera pas notifié d'une nouvelle assignation.

**Prompt :**
```
Intégrer les Push Notifications via le API Notification du navigateur (Service Worker). Côté frontend : créer un service PushNotificationService qui demande la permission au login, enregistre le token FCM, et écoute les événements SW. Quand une notification AMQP arrive, le backend envoie aussi un push via FCM. Côté backend : ajouter un FirebaseAdminSDK ou un client HTTP FCM dans NotificationService. Notifier l'agent quand une commande lui est assignée (ORDER_ASSIGNED), la boutique quand la livraison est terminée (ORDER_DELIVERED), et le fournisseur quand un paiement est confirmé (PAYMENT_CONFIRMED).
```

---

### P1-08 : Export CSV/Excel des commandes

**Problème :** Les fournisseurs et boutiques ne peuvent pas exporter leurs listes de commandes pour analyse externe.

**Prompt :**
```
Côté backend : ajouter un endpoint GET /api/orders/export/csv qui accepte les mêmes filtres que GET /api/orders (status, dateFrom, dateTo) et retourne un CSV avec toutes les colonnes. Utiliser OpenCSV ou un writer custom. Côté frontend : ajouter un bouton "Exporter CSV" dans OrderManagementComponent et OrderListComponent, à côté du filtre. Utiliser FileSaver.js pour déclencher le téléchargement. Même chose pour les paiements avec GET /api/payments/export/csv (déjà partielle dans CsvExportService).
```

---

### P1-09 : Recherche de commandes par référence

**Problème :** Il n'y a aucun champ de recherche pour trouver une commande par sa référence. L'utilisateur doit scroller ou utiliser les filtres de statut.

**Prompt :**
```
Côté backend : ajouter un endpoint GET /api/orders/search?q=REF-XXX qui recherche par référence (LIKE) ou par shopName. Côté frontend : ajouter un champ de recherche en haut de OrderManagementComponent et OrderListComponent, avec autocomplete. Utiliser debounce (300ms) pour ne pas surcharger l'API. Afficher les résultats dans un dropdown avec la référence, le statut, et le nom de la boutique.
```

---

### P1-10 : Alertes stock bas non visibles dans l'UI

**Problème :** Le LowStockAlertScheduler envoie des événements AMQP mais il n'y a pas de composant UI qui affiche les alertes stock bas au fournisseur.

**Prompt :**
```
Côté backend : créer un endpoint GET /api/suppliers/{id}/low-stock-alerts qui retourne les produits dont quantity - reservedQty < minQuantity. Côté frontend : créer un composant LowStockAlertsComponent avec une liste de produits en alerte, triés par urgence. Ajouter un badge rouge dans la sidebar fournisseur avec le nombre d'alertes. Intégrer dans le DashboardComponent une section "Alertes stock" visible uniquement pour SUPPLIER_ADMIN.
```

---

## 🟡 P2 — Priorité Moyenne

---

### P2-01 : Historique des prix produits

**Problème :** Quand le prix d'un produit est modifié, les anciennes commandes affichent le nouveau prix au lieu de celui au moment de la commande.

**Prompt :**
```
Vérifier que OrderItem.unitPrice est bien sauvegardé au moment de la création de la commande (dans CreateOrderUseCase). Si c'est le cas, c'est déjà correct. Sinon, corriger pour que le prix soit copié depuis Product au moment de la création, pas lu dynamiquement. Ajouter un champ `productSnapshot` dans OrderItem avec le nom, prix, et SKU au moment de la commande.
```

---

### P2-02 : Notes/commentaires sur les commandes

**Problème :** Le champ `notes` existe dans Order mais n'est pas éditable après création. Les parties ne peuvent pas communiquer sur une commande spécifique.

**Prompt :**
```
Créer une entité OrderComment liée à Order (orderId, authorId, authorName, content, createdAt). Endpoint POST /api/orders/{id}/comments et GET /api/orders/{id}/comments. Côté frontend : ajouter une section "Commentaires" dans OrderDetailComponent avec un formulaire d'ajout et la liste des commentaires existants. Notifier l'autre partie quand un commentaire est ajouté.
```

---

### P2-03 : Conditions de paiement configurables

**Problème :** Les paiements sont soit immédiats (ASAP) soit manuels. Il n'y a pas de conditions de paiement comme "30 jours", "à réception", etc.

**Prompt :**
```
Ajouter un champ `paymentTerms` (enum: IMMEDIATE, NET_15, NET_30, NET_60) au modèle Order. Modifier CreateOrderRequest pour accepter ce champ. Créer un endpoint GET /api/payments/overdue qui retourne les paiements dont la date d'échéance est dépassée. Côté frontend : ajouter un select "Conditions de paiement" dans OrderCreateComponent, et afficher la date d'échéance calculée dans OrderDetailComponent.
```

---

### P2-04 : Multi-boutique pour un même fournisseur

**Problème :** Un fournisseur ne peut pas voir les commandes de toutes ses boutiques en un seul tableau de bord consolidé.

**Prompt :**
```
Dans OrderController, le endpoint GET /api/orders retourne déjà toutes les commandes du fournisseur (filtré par supplierId). Vérifier que le frontend affiche bien toutes les commandes sans filtrer par boutique. Ajouter un filtre "Boutique" dans OrderManagementComponent avec un select qui liste les boutiques liées au fournisseur. Récupérer les boutiques via GET /api/organizations/{supplierId}/relations.
```

---

### P2-05 : Gestion des promotions/remises globales

**Problème :** Les remises sont uniquement au niveau article. Il n'y a pas de remise globale sur la commande (ex: -10% sur tout le panier).

**Prompt :**
```
Ajouter un champ `globalDiscount` (BigDecimal, pourcentage 0-100) au modèle Order. Modifier le calcul des totaux dans Order.recalculateTotals() pour appliquer la remise globale après la TVA. Modifier CreateOrderRequest pour accepter globalDiscount. Côté frontend : ajouter un champ "Remise globale (%)" dans OrderCreateComponent. Afficher la remise globale dans le récapitulatif de commande.
```

---

### P2-06 : Statut de livraison en temps réel

**Problème :** Le statut de livraison n'est mis à jour que quand une action est faite. Il n'y a pas de suivi GPS ou de mise à jour de position en temps réel.

**Prompt :**
```
Pour une V2 avancée : ajouter des champs `currentLat`, `currentLng`, `lastLocationUpdate` au modèle Order. Créer un endpoint POST /api/orders/{id}/location pour que le livreur mette à jour sa position (appelé toutes les 30 secondes depuis l'app mobile). Côté frontend : dans le détail d'une commande IN_DELIVERY, afficher une carte (Leaflet/OpenStreetMap) avec la position actuelle du livreur. Pour la V1 simple : ajouter un champ `estimatedArrival` calculé à partir de la date confirmée et de la position actuelle.
```

---

### P2-07 : Annulation automatique des commandes non traitées

**Problème :** Les commandes en statut DRAFT ou CONFIRMED depuis plus de X jours ne sont jamais annulées automatiquement.

**Prompt :**
```
Créer un scheduler Spring @Scheduled(cron = "0 0 2 * * ?") qui annule automatiquement les commandes en DRAFT depuis plus de 7 jours et en CONFIRMED depuis plus de 3 jours. Créer CancelExpiredOrdersUseCase.java. Publier un event ORDER_AUTO_CANCELLED. Notifier la boutique par notification.
```

---

### P2-08 : Dashboard personnel par rôle

**Problème :** Le dashboard est le même pour tous les rôles. Un livreur n'a pas besoin de voir les stats de vente.

**Prompt :**
```
Créer des composants de dashboard spécifiques : DashboardSupplierComponent (stats ventes, alertes stock, commandes récentes), DashboardShopComponent (commandes reçues, paiements, livraisons en cours), DashboardAgentComponent (livraisons assignées, en cours, terminées aujourd'hui), DashboardAdminComponent (stats globales, nombre d'organisations, activité récente). Le routing sélectionne le bon composant selon le rôle de l'utilisateur connecté.
```

---

## 🟢 P3 — Basse Priorité

---

### P3-01 : Mode sombre (Dark mode)

**Prompt :**
```
Ajouter un thème sombre à l'application. Créer un service ThemeService avec toggle dark/light. Utiliser CSS variables pour les couleurs (background, text, cards, borders). Sauvegarder la préférence dans localStorage. Ajouter un toggle dans le header ou le profil utilisateur. Adapter tous les composants existants pour utiliser les variables CSS au lieu de couleurs hardcodées.
```

---

### P3-02 : Internationalisation (i18n)

**Prompt :**
```
Internationaliser l'application avec Angular i18n ou ngx-translate. Créer les fichiers de traduction fr.json et en.json. Remplacer tous les textes hardcodés par des clés de traduction. Ajouter un sélecteur de langue dans le header. Adapter les formats de date et de nombre selon la locale.
```

---

### P3-03 : Accessibilité (a11y)

**Prompt :**
```
Améliorer l'accessibilité : ajouter des labels aria sur tous les boutons et inputs, des rôles ARIA sur les modals (role="dialog", aria-modal="true"), un focus trap dans les modals, un skip-to-content link, des contrastes de couleurs conformes WCAG AA, des descriptions aria sur les badges de statut. Tester avec un lecteur d'écran.
```

---

### P3-04 : Impression de commande

**Prompt :**
```
Ajouter un bouton "Imprimer" dans OrderDetailComponent qui ouvre une version imprimable de la commande (format A4). Utiliser window.print() avec une feuille de style @media print qui masque la sidebar, le header, et les boutons d'action. Afficher uniquement les détails de la commande, les articles, et les totaux.
```

---

### P3-05 : Favoris / Commandes récentes

**Prompt :**
```
Ajouter une section "Commandes récentes" dans le dashboard de la boutique, affichant les 5 dernières commandes avec un bouton "Recommander" qui recrée une commande avec les mêmes articles. Côté backend : endpoint GET /api/orders/recent?limit=5 et POST /api/orders/{id}/reorder qui crée une nouvelle commande basée sur les items d'une commande existante.
```

---

### P3-06 : Tableau de bord notifications

**Prompt :**
```
Créer une page /dashboard/notifications qui affiche toutes les notifications de l'utilisateur avec pagination, filtrage par type (ORDER, PAYMENT, DELIVERY), et marquer comme lu. Endpoint GET /api/notifications avec pagination. Badge non-lus dans le header.
```

---

### P3-07 : Gestion des photos produits

**Prompt :**
```
Ajouter un champ `imageUrl` au modèle Product. Endpoint POST /api/suppliers/{id}/products/{productId}/image pour upload d'image (max 5MB, JPG/PNG). Afficher les images dans le catalogue boutique (OrderCreateComponent). Utiliser un stockage local ou S3.
```

---

### P3-08 : Configuration des emails transactionnels

**Prompt :**
```
Créer un service EmailTemplate avec des templates HTML pour : confirmation de commande, notification de livraison, reçu de paiement, invitation agent. Utiliser Thymeleaf ou des templates string. Envoyer les emails via le service existant EmailService quand les statuts changent.
```

---

### P3-09 : Audit trail UI

**Prompt :**
```
Créer un endpoint GET /api/audit-logs qui retourne les logs d'audit (qui a fait quoi, quand). Côté frontend : créer un composant AuditLogComponent dans la section admin qui affiche un tableau avec timestamp, utilisateur, action, entité concernée, détails. Filtrable par date, utilisateur, type d'action.
```

---

### P3-10 : Onboarding / Tutoriel

**Prompt :**
```
Créer un parcours d'onboarding pour les nouveaux utilisateurs : à la première connexion, afficher un stepper guidé qui explique les fonctionnalités selon le rôle. Utiliser un composant TourComponent avec des tooltips qui pointent vers les éléments clés de l'interface. Marquer l'onboarding comme terminé dans le profil utilisateur.
```

---

## Résumé exécutif

```
┌─────────────────────────────────────────────────────┐
│           DASHBOARD DES AMÉLIORATIONS                │
├─────────────────────────────────────────────────────┤
│  P0 Critique :  4 améliorations  ▓▓▓▓░░░░░░  6%    │
│  P1 Haute :    20 améliorations  ▓▓▓▓▓▓▓▓░░ 31%    │
│  P2 Moyenne :  26 améliorations  ▓▓▓▓▓▓▓▓▓░ 40%    │
│  P3 Basse :    15 améliorations  ▓▓▓▓▓░░░░░ 23%    │
├─────────────────────────────────────────────────────┤
│  TOTAL :       65 améliorations                     │
│                                                     │
│  Estimation effort :                                │
│  P0 : ~2 semaines                                   │
│  P1 : ~6 semaines                                   │
│  P2 : ~8 semaines                                   │
│  P3 : ~4 semaines                                   │
│  TOTAL : ~20 semaines (5 mois)                      │
└─────────────────────────────────────────────────────┘
```
