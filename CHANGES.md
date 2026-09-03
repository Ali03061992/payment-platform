# Payment Platform - Changelog

## Session de développement (2026-09-03)

### Résumé
Corrections critiques du flux de paiement, améliorations UI/UX, et préparation du déploiement réseau.

---

## 1. Corrections du flux de paiement

### BUG : Création de paiement impossible pour SHOP
**Fichier :** `create-payment.component.ts`
- **Problème :** Les utilisateurs SHOP_ADMIN/SHOP_MANAGER ne pouvaient pas créer de paiements car le formulaire appelait `/api/admin/shops` et `/api/admin/suppliers` (endpoints réservés SYSTEM_ADMIN)
- **Solution :**
  - Auto-remplissage du shop depuis le contexte utilisateur (`user.organizationId`)
  - Chargement des fournisseurs via `/api/admin/supplier-shop-relations/shop/{id}`
  - Seuls les fournisseurs liés (statut `ACTIVE`) sont affichés

### BUG : Balance Summary ne correspondait pas au backend
**Fichiers :** `BalanceController.java`, `BalanceUseCase.java`, `BalanceRepository.java`
- **Problème :** Le frontend attendait `BalanceSummary` (avec `supplierName`, `currentBalance`, etc.) mais le backend retournait `List<BalanceEntry>` (entrées brutes du ledger)
- **Solution :**
  - Ajout de requêtes SQL agrégées dans `BalanceRepository` :
    - `findDistinctSupplierIdsByShopId`
    - `findDistinctShopIdsBySupplierId`
    - `sumOrdersBySupplierAndShop`
    - `sumPaymentsBySupplierAndShop`
    - `findLatestBySupplierIdAndShopId`
  - Nouvelles méthodes `getShopBalanceSummaries()` et `getSupplierBalanceSummaries()` dans `BalanceUseCase`
  - `BalanceController` retourne maintenant `List<BalanceResponse>` au lieu de `List<BalanceEntry>`

### BUG : CSS class des événements de paiement
**Fichier :** `payment-detail.component.html`
- **Problème :** `e.action.toLowerCase().replace('payment.', '')` ne fonctionnait pas (les actions utilisent `PAYMENT_CREATED`, pas `payment.created`)
- **Solution :** Changé `replace('payment.', '')` → `replace('payment_', '')`

### BUG : rejectionReason null affiché
**Fichier :** `payment-detail.component.html`
- **Problème :** `*ngIf="payment.rejectionReason"` affichait "null" quand le champ est vide
- **Solution :** Ajout du test `&& payment.rejectionReason !== 'null'`

---

## 2. Service Worker (PWA)

### BUG : Strategy "network-first" inconnue
**Fichier :** `ngsw-config.json`
- **Problème :** Angular Service Worker ne supporte pas `"network-first"` (stratégie Workbox)
- **Solution :** Changé vers `"freshness"` (équivalent ngsw)
- **Impact :** Toutes les requêtes API échouaient avec `ERR_FAILED`

---

## 3. Améliorations UI/UX

### Navbar latérale collapsible
**Fichiers :** `layout.component.css`, `layout.component.html`
- Sidebar rétractable sur desktop (260px → 72px)
- Affiche uniquement les icônes en mode collapsed
- Toggle button dans le header
- Transition fluide avec animation

### Services ajoutés
- `ToastService` : Notifications toast (succès/erreur/info)
- `SupplierAgentService` : Gestion des agents fournisseur
- `NotificationBannerComponent` : Bannière consentement push

### Mobile responsive
- Swipe gesture pour ouvrir/fermer la sidebar
- Header mobile avec menu hamburger
- Overlay automatique

---

## 4. Fixes techniques

### Cypress Tests
- Suppression dépendance `@cypress/angular` cassée
- Fix `API_URL` redeclaration → `Cypress.env('apiUrl')`
- Exclusion des fichiers `.cy.ts` du `tsconfig.json`

### Swagger UI
- Ajout `springdoc-openapi-starter-webmvc-ui` dans le gateway
- Configuration Springdoc dans `application.yml`
- `GatewayOpenApiConfig.java` pour la documentation API

### Login sécurisé
- Suppression du fallback qui créait un utilisateur factice `SYSTEM_ADMIN`
- `getMe()`失败 retourne maintenant une erreur

---

## 5. Fichiers modifiés (76 fichiers)

### Backend (5 fichiers)
- `backend/api-gateway/pom.xml`
- `backend/api-gateway/src/main/resources/application.yml`
- `backend/organization-service/.../BalanceRepository.java`
- `backend/organization-service/.../BalanceUseCase.java`
- `backend/organization-service/.../BalanceController.java`
- `backend/api-gateway/.../GatewayOpenApiConfig.java` (nouveau)

### Frontend (70+ fichiers)
- Composants : layout, login, payments, orders, stock, products, balance
- Services : payment, balance, organization, notification, toast, supplier-agent
- Modèles : balance, payment, organization
- Styles : dashboard, payment-list, order-list, stock-management

---

## 6. Données de test

Le projet utilise les données Covale/Pointteck :
- **Fournisseurs :** COVALE SARL, POINTECK SAS
- **Boutiques :** Boutique Tunis, Boutique Sfax, Boutique Sousse
- **Utilisateurs :** admin, supplier1, supplier2, shop1, shop2, shop3

---

## 7. Déploiement

### Accès réseau local
Pour permettre l'accès depuis d'autres machines du même réseau :

```bash
# Frontend (port 8080)
ng serve --host 0.0.0.0 --port 8080

# Backend Gateway (port 8081)
mvn spring-boot:run -Dspring-boot.run.arguments="--server.address=0.0.0.0"
```

### Ports
| Service | Port |
|---------|------|
| Frontend Angular | 8080 |
| API Gateway | 8081 |
| Identity Service | 8082 |
| Organization Service | 8083 |
| Payment Service | 8084 |
| Notification Service | 8085 |
