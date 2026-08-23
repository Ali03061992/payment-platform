# Frontend Routes & API Paths - Payment Platform

## Angular Routes

| Route | Component | Auth Required | Roles Allowed | Description |
|-------|-----------|---------------|---------------|-------------|
| `/login` | LoginComponent | No | All | Page de connexion |
| `/register` | RegisterComponent | No | All | Inscription publique (SUPPLIER/SHOP roles) |
| `/dashboard` | LayoutComponent > DashboardComponent | Yes | All authenticated | Tableau de bord principal |
| `/dashboard/admin/users` | LayoutComponent > UserManagementComponent | Yes | SYSTEM_ADMIN | Liste et gestion des utilisateurs |
| `/dashboard/admin/users/create` | LayoutComponent > CreateUserComponent | Yes | SYSTEM_ADMIN | Créer un nouveau compte utilisateur |
| `/dashboard/sales/accounts` | LayoutComponent > AccountActivationComponent | Yes | SALES, SYSTEM_ADMIN | Activer/désactiver des comptes |
| `/dashboard/supplier/stock` | LayoutComponent > StockManagementComponent | Yes | SUPPLIER_ADMIN, SUPPLIER_AGENT | Gestion du stock |
| `/dashboard/supplier/stock/create` | LayoutComponent > AddProductComponent | Yes | SUPPLIER_ADMIN | Ajouter un produit |

## Roles

| Role | Description | Permissions |
|------|-------------|-------------|
| SYSTEM_ADMIN | Administrateur global | ADMIN_MANAGE_ORGANIZATIONS, ADMIN_MANAGE_USERS, ADMIN_VIEW_AUDIT, ADMIN_VIEW_STATS, SALES_MANAGE_ACCOUNTS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SUPPLIER_ADMIN | Administrateur fournisseur | SUPPLIER_MANAGE_AGENTS, SUPPLIER_MANAGE_PAYMENTS, SUPPLIER_MANAGE_PRODUCTS, SUPPLIER_MANAGE_STOCK, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SUPPLIER_AGENT | Agent fournisseur | SUPPLIER_MANAGE_PAYMENTS, SUPPLIER_MANAGE_STOCK, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SHOP_ADMIN | Administrateur boutique | SHOP_MANAGE_AGENTS, SHOP_CREATE_PAYMENTS, SHOP_CANCEL_PAYMENTS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SHOP_AGENT | Agent boutique | SHOP_CREATE_PAYMENTS, SHOP_CANCEL_PAYMENTS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |
| SALES | Commercial | SALES_MANAGE_ACCOUNTS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS |

## Backend API Endpoints

### Auth (Identity Service via Gateway)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | Public | Connexion, retourne JWT |
| POST | `/api/auth/register` | Public | Inscription (SUPPLIER/SHOP roles) |
| GET | `/api/auth/me` | Authenticated | Profil utilisateur courant |

### Users (Identity Service via Gateway)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/users` | ADMIN_MANAGE_USERS, SALES_MANAGE_ACCOUNTS | Liste des utilisateurs |
| GET | `/api/users/{id}` | Authenticated | Détails utilisateur |
| PATCH | `/api/users/{id}/activate` | ADMIN_MANAGE_USERS, SALES_MANAGE_ACCOUNTS | Activer un compte |
| PATCH | `/api/users/{id}/disable` | ADMIN_MANAGE_USERS, SALES_MANAGE_ACCOUNTS | Désactiver un compte |

### Stock Management (Organization Service via Gateway)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/suppliers/{supplierId}/products` | SUPPLIER_MANAGE_PRODUCTS, SUPPLIER_MANAGE_STOCK | Liste des produits |
| GET | `/api/suppliers/{supplierId}/products/{productId}` | SUPPLIER_MANAGE_PRODUCTS, SUPPLIER_MANAGE_STOCK | Détails produit |
| POST | `/api/suppliers/{supplierId}/products` | SUPPLIER_MANAGE_PRODUCTS | Créer un produit |
| PATCH | `/api/suppliers/{supplierId}/products/{productId}` | SUPPLIER_MANAGE_PRODUCTS | Modifier un produit |
| DELETE | `/api/suppliers/{supplierId}/products/{productId}` | SUPPLIER_MANAGE_PRODUCTS | Supprimer un produit |
| GET | `/api/suppliers/{supplierId}/movements` | SUPPLIER_MANAGE_STOCK | Liste des mouvements de stock |
| POST | `/api/suppliers/{supplierId}/movements` | SUPPLIER_MANAGE_STOCK | Créer un mouvement de stock |

## Default Accounts

| Username | Password | Role | Description |
|----------|----------|------|-------------|
| system.admin | Admin@123 | SYSTEM_ADMIN | Administrateur global |
| sales.admin | Sales@123 | SALES | Commercial |

## Services

| Service | Port | Description |
|---------|------|-------------|
| Angular UI | 8080 | Frontend Angular |
| API Gateway | 8081 | Reverse proxy |
| Identity Service | 8082 | Authentification, utilisateurs |
| Organization Service | 8083 | Fournisseurs, boutiques, stock |
| Payment Service | 8084 | Paiements |
| Notification Service | 8085 | Notifications |
| Adminer | 8086 | MySQL Web UI |
| MySQL | 3307 | Base de données |
| RabbitMQ | 5673/15673 | Message broker |

## File Structure (Frontend)

```
src/app/
├── core/
│   ├── auth.guard.ts          # Guard: vérifie l'authentification
│   ├── jwt.interceptor.ts     # Intercepteur: ajoute le token JWT
│   └── role.guard.ts          # Guard: vérifie les rôles
├── models/
│   ├── user.model.ts          # Modèles User, Login, Register
│   └── stock.model.ts         # Modèles Product, StockMovement
├── services/
│   ├── auth.service.ts        # Service d'inscription
│   ├── login.service.ts       # Service de connexion
│   ├── user.service.ts        # Service de gestion des utilisateurs
│   └── stock.service.ts       # Service de gestion du stock
├── layout/
│   ├── layout.component.ts    # Layout principal (sidebar + header)
│   ├── layout.component.html
│   └── layout.component.css
├── dashboard/
│   ├── dashboard.component.ts
│   ├── dashboard.component.html
│   └── dashboard.component.css
├── login/
│   ├── login.component.ts
│   ├── login.component.html
│   └── login.component.css
├── register/
│   ├── register.component.ts
│   ├── register.component.html
│   └── register.component.css
├── admin/
│   ├── user-management/
│   │   ├── user-management.component.ts
│   │   ├── user-management.component.html
│   │   └── user-management.component.css
│   └── create-user/
│       ├── create-user.component.ts
│       ├── create-user.component.html
│       └── create-user.component.css
├── sales/
│   └── account-activation/
│       ├── account-activation.component.ts
│       ├── account-activation.component.html
│       └── account-activation.component.css
├── supplier/
│   ├── stock-management/
│   │   ├── stock-management.component.ts
│   │   ├── stock-management.component.html
│   │   └── stock-management.component.css
│   └── add-product/
│       ├── add-product.component.ts
│       ├── add-product.component.html
│       └── add-product.component.css
├── app.module.ts
├── app.component.ts
├── app.component.html
└── app.component.css
```
