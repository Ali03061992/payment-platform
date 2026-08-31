# Script de Présentation - Payment Platform B2B
**Durée estimée : 4-5 minutes**  
**Format : Démonstration écran + voix-off**  
**Langue : Français**

---

## 🎬 Structure de la Vidéo

| Segment | Durée | Contenu |
|---------|-------|---------|
| 1. Intro | 0:00 - 0:30 | Contexte, problème résolu, stack technique |
| 2. Architecture | 0:30 - 1:15 | Microservices, DDD, Clean Architecture |
| 3. Auth & RBAC | 1:15 - 2:00 | JWT, rôles, permissions, organisations |
| 4. Données de démo (Covale/Pointteck) | 2:00 - 2:45 | Structure réelle : 2 fournisseurs, 4 boutiques |
| 5. Flux Paiement + Notifications | 2:45 - 3:45 | Création → Confirmation → Notifications temps réel |
| 6. PWA & Mobile | 3:45 - 4:15 | Installation, responsive, offline, QR Code |
| 7. Tests & Qualité | 4:15 - 4:45 | 100+ tests Cypress, Lighthouse 100% |
| 8. Conclusion | 4:45 - 5:00 | Repo, déploiement Docker, prochaines étapes |

---

## 📝 Script Détaillé (Voix-off)

### 1. INTRODUCTION (0:00 - 0:30)

> **[Écran : Logo Payment Platform + titre "Plateforme B2B de Gestion Paiements Fournisseurs-Boutiques"]**
>
> "Bonjour. Payment Platform est une solution complète B2B pour digitaliser la relation commerciale entre **fournisseurs** et **boutiques** : catalogue, stock, commandes, livraisons, soldes, paiements et notifications temps réel."
>
> **[Écran : Diagramme architecture microservices]**
>
> "L'application suit une architecture **microservices** avec **Domain-Driven Design** et **Clean Architecture** : 6 services indépendants (Identity, Organization, Payment, Notification, API Gateway, Frontend Angular), communiquant via **RabbitMQ** (event-driven) et une base MySQL par service."

---

### 2. ARCHITECTURE TECHNIQUE (0:30 - 1:15)

> **[Écran : Code - structure dossiers backend/modules]**
>
> "Côté backend : **Java 26**, **Spring Boot 4.1**, **Maven multi-module**. Chaque service a son propre domaine : Identity (users, auth), Organization (suppliers, shops, relations, orders, stock, catalog), Payment (paiements, soldes), Notification (temps réel via SSE)."
>
> **[Écran : docker-compose.yml]**
>
> "Le déploiement est 100% **Docker** : 10 conteneurs (MySQL 8.4, RabbitMQ 4, Mailpit, Adminer, 6 services Java, Nginx + Angular). Un seul `docker compose up` lance toute la plateforme."
>
> **[Écran : shared-lib - Outbox Pattern]**
>
> "La cohérence événementielle est garantie par le **pattern Outbox** : chaque mutation écrit dans la table `outbox_events` dans la même transaction, puis un `OutboxRelay` publie vers RabbitMQ. Idempotence côté consommateur via `EventDeduplicator`."

---

### 3. AUTHENTIFICATION & RBAC (1:15 - 2:00)

> **[Écran : Page login → Dashboard system.admin]**
>
> "L'authentification utilise **JWT HS256** (30 min expiration). Le Gateway ne valide pas les tokens — il transmet simplement l'en-tête `Authorization`. Chaque service backend valide via un `JwtAuthenticationFilter` partagé qui peuple le `SecurityContext` avec **rôles** ET **permissions fines**."
>
> **[Écran : PermissionCatalog.java]**
>
> "5 rôles métier : `SYSTEM_ADMIN`, `SUPPLIER_ADMIN`, `SUPPLIER_AGENT`, `SHOP_ADMIN`, `SHOP_AGENT`. Chaque rôle porte des permissions explicites : ex. `SUPPLIER_ADMIN` a `SUPPLIER_MANAGE_PRODUCTS`, `SUPPLIER_MANAGE_STOCK`, `SUPPLIER_MANAGE_PAYMENTS`, `VIEW_NOTIFICATIONS`..."
>
> **[Écran : Navigation selon rôle]**
>
> "La navigation Angular s'adapte dynamiquement : un `SUPPLIER_ADMIN` voit Catalogue/Stock/Commandes/Livraisons ; un `SHOP_ADMIN` voit Mes Commandes/Nouvelle Commande/Balance. Le `SYSTEM_ADMIN` accède à l'admin complet (users, fournisseurs, boutiques, relations, stats)."

---

### 4. DONNÉES DE DÉMO : COVALE & POINTTECK (2:00 - 2:45)

> **[Écran : Admin → Fournisseurs / Boutiques]**
>
> "Pour la démo, nous avons créé une structure réaliste : **2 fournisseurs** — **Covale** (id=1) et **Pointteck** (id=2) — chacun avec **2 boutiques** : Covale gère *Tunis Sousse* (id=3) et *Sfax Mahdiya* (id=4) ; Pointteck gère *Tunis Centre* (id=5) et *Sfax Ville* (id=6)."
>
> **[Écran : Admin → Relations F-B]**
>
> "4 relations **actives** lient chaque fournisseur à ses boutiques. C'est la condition *sine qua non* pour créer un paiement ou une commande."
>
> **[Écran : Users table]**
>
> "11 utilisateurs pré-configurés : `system.admin` (global), `covale.admin` + 2 agents (org=1), `pointteck.admin` + 2 agents (org=2), `abdelslam` admin boutique Tunis (org=3), `ali` admin boutique Sfax (org=4), et 2 admins pour les boutiques Pointteck. Tous avec le mot de passe `Admin@123`."

---

### 5. FLUX PAIEMENT + NOTIFICATIONS TEMPS RÉEL (2:45 - 3:45)

> **[Écran : Connexion abdelslam → Paiements → Créer]**
>
> "Connecté en tant qu'**abdelslam** (admin boutique Tunis Sousse, org=3), je crée un paiement de 500€ vers le fournisseur Covale. La relation existe, le paiement passe en statut `PENDING`."
>
> **[Écran : Notification cloche 🔔 appear]**
>
> "Immédiatement : l'événement `payment.created` est publié via RabbitMQ. Le **Notification Service** consomme, déduplique, et crée **2 notifications** : une pour l'organisation fournisseur (Covale), une pour l'utilisateur créateur (abdelslam)."
>
> **[Écran : Connexion covale.admin → 🔔 1 notification]**
>
> "Côté fournisseur, `covale.admin` voit la notification : *'Nouveau paiement PAY-xxx de 500 EUR'*. Il ouvre le détail, scanne le QR code si besoin, et **confirme**."
>
> **[Écran : Confirmation → Notification shop]**
>
> "La confirmation publie `payment.confirmed` → notification pour la boutique : *'Paiement PAY-xxx confirmé par le fournisseur'*. Le badge 🔔 se met à jour en temps réel (polling 30s + SSE prévu)."
>
> **[Écran : Rejet / Annulation]**
>
> "Même flux pour le **rejet** (notification boutique avec motif) et l'**annulation** (notification fournisseur). Tout est tracé en base `notifications` avec `read_status`, `related_entity_type=PAYMENT`."

---

### 6. PWA, MOBILE & QR CODE (3:45 - 4:15)

> **[Écran : Mobile view - hamburger menu]**
>
> "L'application est une **PWA installable** : `manifest.json`, Service Worker (`ngsw-worker.js`), cache statique 1 an, stratégie `NetworkFirst` pour l'API. Fonctionne offline pour la navigation."
>
> **[Écran : Responsive < 768px]**
>
> "Sur mobile : sidebar coulissante avec overlay, bouton hamburger, police 16px (pas de zoom iOS), zones de toucher 44px, `safe-area-inset` pour les encoche."
>
> **[Écran : Payment Detail → QR Code]**
>
> "Chaque paiement génère un **QR Code** (lib `angularx-qrcode`) contenant `paymentId`, `reference`, `amount`, `currency`, `shopId`, `supplierId`. Le fournisseur scanne avec la page **Scanner QR** (caméra native via `BarcodeDetector` API + fallback input manuel)."
>
> **[Écran : Agent Payments]**
>
> "Écran **Paiements Agents** pour `SUPPLIER_ADMIN` : filtre par date, total du jour en vert, détail par agent (créateur du paiement) avec expansion."

---

### 7. TESTS & QUALITÉ (4:15 - 4:45)

> **[Écran : Cypress run - 100 tests passing]**
>
> "**100+ tests Cypress** passent : 14 admin, 25 API integration, 10 auth, 12 navigation, **11 notifications**, 9 payments, 6 shop, 13 supplier. Nouveaux tests `data-structure.cy.ts` valident toute la structure Covale/Pointteck : auth par rôle, relations, RBAC endpoints, flux paiement complet, UI navigation."
>
> **[Écran : Lighthouse scores]**
>
> "**Lighthouse 100%** sur les 4 catégories : Performance, Accessibilité, SEO, Best Practices. Headers sécurité Nginx (`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, CSP), gzip, cache assets, pas de `console.error` en prod."
>
> **[Écran : Budget bundles Angular]**
>
> "Budgets respectés : initial < 600kB (actuel ~545kB), composants < 6kB CSS."

---

### 8. CONCLUSION (4:45 - 5:00)

> **[Écran : GitHub repo + docker-compose]**
>
> "Tout le code est sur **GitHub** : `github.com/Ali03061992/payment-platform`. Un `docker compose up -d` lance l'ensemble. Les données de démo (Covale/Pointteck) sont créées via scripts SQL + API."
>
> **[Écran : Prochaines étapes]**
>
> "Prochaines itérations : SSE temps réel pour notifications (remplacer polling), exports PDF/Excel, multi-devises, gestion des retours produits, dashboard analytique fournisseur/boutique."
>
> "Merci d'avoir regardé ! Questions ?"

---

## 🎥 Notes de Tournage

| Scène | Action écran | Outil suggéré |
|-------|-------------|---------------|
| Architecture | Diagramme draw.io / Mermaid | OBS + navigateur |
| Code | VS Code (thème sombre) | Zoom sur fichiers clés |
| API | Postman / curl + jq | Terminal + navigateur |
| UI | Chrome 1280x720 (Cypress viewport) | Device toolbar mobile |
| Tests | `npx cypress run --headed` | Enregistrement terminal |
| Lighthouse | Chrome DevTools > Lighthouse | Capture d'écran scores |

---

## 🔗 Liens Utiles pour la Vidéo

- **Repo** : `https://github.com/Ali03061992/payment-platform`
- **Démo locale** : `http://localhost:8080` (après `docker compose up -d`)
- **Comptes démo** :
  - `system.admin / Admin@123` (admin global)
  - `covale.admin / Admin@123` (fournisseur 1)
  - `abdelslam / Admin@123` (boutique Tunis)
  - `ali / Admin@123` (boutique Sfax)
  - `pointteck.admin / Admin@123` (fournisseur 2)

---

## 💡 Variantes Possibles

| Version | Durée | Focus |
|---------|-------|-------|
| **Courte (2 min)** | 2:00 | Demo rapide : login → paiement → notification → QR |
| **Technique (8 min)** | 8:00 | Deep dive : Outbox, DDD, Clean Arch, Event-driven |
| **Business (3 min)** | 3:00 | Valeur métier : gain de temps, traçabilité, mobile |

---

*Généré automatiquement pour Payment Platform v1.0.0*  
*Date : Août 2026*