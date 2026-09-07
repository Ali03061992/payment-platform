# Specification Fonctionnelle & Axes d'Amelioration

## Payment Platform — B2B Payment Management System

---

## PARTIE 1: SPECIFICATION FONCTIONELLE

### 1. Contexte et Objectifs

**Payment Platform** est une plateforme B2B de gestion des paiements unitaires entre fournisseurs et boutiques. Elle couvre le cycle complet: catalogage, gestion des stocks, commandes, livraisons, paiements et notifications.

**Objectifs metier:**
- Centraliser la gestion des relations fournisseur-boutique
- Automatiser le cycle de commande et de livraison
- Suivre les paiements avec validation complete
- Optimiser les stocks via des algorithmes scientifiques
- Offrir une visibilite en temps reel sur les operations

---

### 2. Architecture Fonctionnelle

```
┌─────────────────────────────────────────────────────────┐
│                    SYSTEM_ADMIN                          │
│  Gestion organizations / utilisateurs / roles / stats   │
└───────────────────────┬─────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  FOURNISSEUR │ │  COMMANDE    │ │  PAIEMENT    │
│  - Catalogue │ │  - Creation  │ │  - Creation  │
│  - Stocks    │ │  - Lifecycle │ │  - Validation│
│  - Optimisation│ │  - Livraison│ │  - Recherche │
│  - Agents    │ │  - Suivi     │ │  - Stats     │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       └────────────────┼────────────────┘
                        ▼
              ┌──────────────────┐
              │    BOUTIQUE      │
              │  - Commandes     │
              │  - Paiements     │
              │  - Suivi         │
              │  - Balance       │
              └──────────────────┘
```

---

### 3. Roles et Permissions

#### 3.1 Matrice RBAC

| Action | SYSTEM_ADMIN | SUPPLIER_ADMIN | SUPPLIER_AGENT | SHOP_ADMIN | SHOP_AGENT |
|--------|:---:|:---:|:---:|:---:|:---:|
| Gerer organisations | X | | | | |
| Gerer utilisateurs | X | | | | |
| Voir audit/stats | X | | | | |
| Ajuster balances | X | | | | |
| Reindexer Elasticsearch | X | | | | |
| Gerer agents fournisseur | | X | | | |
| Gerer catalogue | | X | X | | |
| Gerer stock | | X | X | | |
| Optimiser stock | | X | X | | |
| Creer commandes | | X | | | |
| Confirmer/Preparer commandes | | X | | | |
| Assigner livreur | | X | | | |
| Confirmer/Rejeter paiements | | X | X | | |
| Chercher paiements | | X | X | | |
| Gerer agents boutique | | | | X | |
| Creer paiements | | | | X | X |
| Creer commandes | | | | X | X |
| Annuler/Rejeter commandes | | | | X | X |
| Voir balance | | | | X | X |
| Voir notifications | X | X | X | X | X |

#### 3.2 Roles Detailles

**SYSTEM_ADMIN:**
- Gerer les fournisseurs et boutiques (CRUD, activation/desactivation)
- Gerer les utilisateurs et les roles
- Voir les statistiques globales et les logs d'audit
- Ajuster manuellement les balances
- Reindexer Elasticsearch
- Voir tous les paiements de toutes les organisations

**SUPPLIER_ADMIN:**
- Gerer les agents de son organisation
- Gerer le catalogue (categories, familles, produits)
- Gerer les stocks (mouvements, ajustements)
- Lancer l'optimisation des stocks
- Gerer les commandes: confirmer, preparer, pret pour livraison, assigner livreur
- Confirmer/Rejeter/Annuler les paiements
- Chercher les paiements via Elasticsearch

**SUPPLIER_AGENT:**
- Voir et gerer les stocks (mouvements)
- Confirmer/Rejeter les paiements
- Voir les livraisons assignees
- Lancer l'optimisation des stocks
- NE PEUT PAS: gerer les agents, creer des commandes, acceder a l'admin

**SHOP_ADMIN:**
- Gerer les agents de sa boutique
- Creer des commandes (selection fournisseur + produits)
- Annuler/Rejeter des commandes
- Creer des paiements
- Voir l'historique des balances

**SHOP_AGENT:**
- Creer des paiements
- Voir les commandes
- NE PEUT PAS: gerer les agents, creer des commandes

---

### 4. Workflow des Commandes

#### 4.1 Machine a Etats

```
                    ┌──────────────────────────────────────────────────────┐
                    │                                                      │
DRAFT ──┬──> CONFIRMED ──┬──> PREPARING ──┬──> READY_FOR_DELIVERY ──> IN_DELIVERY ──┬──> DELIVERED ──┬──> ACCEPTED
        │                │                │                                          │                │
        │                │                │                                          │                │
        └──> CANCELLED   └──> CANCELLED   └──> CANCELLED                             └──> REJECTED     └──> (terminal)
                                                                                      (terminal)
                                                                                         │
                                                                                         └──> DELIVERY_REJECTED ──┬──> CONFIRMED (re-cyclage)
                                                                                                                 └──> CANCELLED
```

#### 4.2 Regles de Transition

| Etat Actuel | Transitions Autorisees | Acteur |
|---|---|---|
| DRAFT | CONFIRMED, CANCELLED | SUPPLIER_ADMIN, SHOP_ADMIN |
| CONFIRMED | PREPARING, CANCELLED | SUPPLIER_ADMIN |
| PREPARING | READY_FOR_DELIVERY, CANCELLED | SUPPLIER_ADMIN |
| READY_FOR_DELIVERY | IN_DELIVERY | (transition automatique ou manuelle) |
| IN_DELIVERY | DELIVERED, DELIVERY_REJECTED | DELIVERY_AGENT, SUPPLIER_ADMIN |
| DELIVERED | ACCEPTED, REJECTED | SHOP_MANAGER |
| DELIVERY_REJECTED | CONFIRMED, CANCELLED | SUPPLIER_ADMIN |
| CANCELLED | (terminal) | — |
| REJECTED | (terminal) | — |
| ACCEPTED | (terminal) | — |

#### 4.3 Flux Detaille

**Scenario 1: Commande Boutique → Fournisseur**
```
1. Boutique cree la commande (DRAFT)
2. Fournisseur confirme (CONFIRMED) — verifie stock disponible
3. Fournisseur prepare (PREPARING) — reserve le stock
4. Fournisseur marque pret (READY_FOR_DELIVERY)
5. Fournisseur assigne un livreur
6. Livreur livre (IN_DELIVERY → DELIVERED)
7. Boutique accepte (DELIVERED → ACCEPTED) — stock debite definitivement
```

**Scenario 2: Commande Fournisseur → Boutique (auto-confirme)**
```
1. Fournisseur cree la commande → DRAFT auto-confirme → CONFIRMED → PREPARING
2. Meme flux de livraison que ci-dessus
```

**Scenario 3: Rejet de livraison**
```
1. Commande IN_DELIVERY
2. Livreur/Fournisseur rejette (DELIVERY_REJECTED)
3. Fournisseur peut re-confirmer (→ CONFIRMED) ou annuler (→ CANCELLED)
```

#### 4.4 Gestion du Stock dans les Commandes

| Evenement | Impact Stock |
|---|---|
| Creation commande | `reservedQty += quantiteCommandee` |
| Annulation commande | `reservedQty -= quantiteCommandee` |
| Acceptation commande | `quantity -= quantiteCommandee`, `reservedQty = 0` |
| Stock insuffisant | Erreur 409 "Stock insuffisant" |

---

### 5. Workflow des Paiements

#### 5.1 Cycle de Vie

```
PENDING ──┬──> CONFIRMED (fournisseur confirme)
          │
          ├──> REJECTED (fournisseur rejette avec motif)
          │
          └──> CANCELLED (boutique annule)
```

#### 5.2 Regles

| Action | Acteur | Condition |
|---|---|---|
| Creer | SHOP_ADMIN, SHOP_AGENT | Relation fournisseur-boutique ACTIVE |
| Confirmer | SUPPLIER_ADMIN, SUPPLIER_AGENT | Statut = PENDING |
| Rejeter | SUPPLIER_ADMIN, SUPPLIER_AGENT | Statut = PENDING, motif obligatoire |
| Annuler | SHOP_ADMIN, SHOP_AGENT | Statut = PENDING |

#### 5.3 Consecquences

- **Confirmation**: Evenement `payment.confirmed` → notification aux deux parties
- **Rejet**: Evenement `payment.rejected` → notification + motif stocke
- **Annulation**: Evenement `payment.cancelled` → notification
- **Optimistic Locking**: `@Version` evite les conflits de double confirmation

---

### 6. Gestion du Catalogue

#### 6.1 Hierarchie

```
Fournisseur
  ├── Categories (N)  ←—— ManyToMany ——→  Familles (N)
  │                                         │
  └── Produits (N) ──┬── categoryId ────────┘
                     └── familyId ──────────┘
```

#### 6.2 Entites

| Entite | Champs Cles | Relations |
|---|---|---|
| ProductCategory | name, code, supplierId | Appartient a un fournisseur |
| ProductFamily | name, code, supplierId, categories[] | ManyToMany avec Categories |
| Product | name, sku, unitPrice, quantity, reservedQty, categoryId, familyId, unit, minQuantity | Appartient a un fournisseur |

---

### 7. Gestion des Stocks

#### 7.1 Types de Mouvements

| Type | Description | Impact |
|---|---|---|
| IN | Entree de stock | `quantity += qty` |
| OUT | Sortie de stock | `quantity -= qty` |
| ADJUSTMENT | Ajustement (inventaire) | `quantity = nouvelleValeur` |

#### 7.2 Statuts de Produit

| Statut | Condition |
|---|---|
| ACTIVE | Stock > 0, produit actif |
| INACTIVE | Desactive manuellement |
| OUT_OF_STOCK | Stock = 0 |

#### 7.3 Mecanisme de Reservation

```
Stock Total = quantity (physique)
Stock Disponible = quantity - reservedQty (physique - reserve)
Stock Securite = calcule par l'optimiseur
```

---

### 8. Moteur d'Optimisation des Stocks

#### 8.1 Modules

| Module | Algorithme | Sortie |
|---|---|---|
| **ABCClassifier** | Pareto (valeur annuelle) | A (20% CA), B (30%), C (50%) |
| **XYZClassifier** | Coefficient de variation | X (stable), Y (variable), Z (sporadique) |
| **DemandForecaster** | SMA, WMA, SES, Holt, Holt-Winters, Croston | Prevision + methode optimale |
| **SafetyStockCalculator** | SS = Z * sqrt(L*σd² + d²*σL²) | Stock de securite calcule |
| **ReorderPointCalculator** | ROP = d*L + SS | Point de commande |
| **EOQCalculator** | EOQ = sqrt(2DS/H) | Quantite economique optimale |
| **RiskAnalyzer** | Distribution normale | Risque rupture + surstock |
| **AnomalyDetector** | Regles metier | Anomalies (stocks negatifs, pics, etc.) |
| **RecommendationEngine** | Combinaison de tous | ACTION: ORDER_NOW, MONITOR, REDUCE, etc. |

#### 8.2 Matrice ABC x XYZ

| | X (Stable) | Y (Variable) | Z (Sporadique) |
|---|---|---|---|
| **A** | Commande reguliere, stock faible | Commande adjustable, stock moyen | Commande au cas par cas |
| **B** | Review periodique | Review + stock securite | Commande par lot |
| **C** | Stock minimal | Commande en lot | Stock de securite eleve |

---

### 9. Systeme de Notifications

#### 9.1 Declencheurs

| Evenement | Notification Generee |
|---|---|
| payment.created | "Paiement {ref} cree par {shop}" → fournisseur |
| payment.confirmed | "Paiement {ref} confirme" → boutique |
| payment.rejected | "Paiement {ref} rejete: {motif}" → boutique |
| payment.cancelled | "Paiement {ref} annule" → fournisseur |

#### 9.2 Fonctionnalites

- Stockage persistant en base
- Polling frontend toutes les 30 secondes
- Badge compteur non-lus
- Marquer lu individuellement ou en masse
- Dedoublonnage des evenements (table `processed_events`)

---

### 10. Architecture Technique

#### 10.1 Stack

| Couche | Technologie |
|---|---|
| Frontend | Angular 16, TypeScript, PWA |
| API Gateway | Java 26, Spring Boot 4.1.0 (proxy personnalise) |
| Backend | Java 26, Spring Boot 4.1.0, DDD/Clean Architecture |
| Base de donnees | MySQL 8.4 (une base par service) |
| Search Engine | Elasticsearch 9.0.0 |
| Message Queue | RabbitMQ 4.x (Outbox Pattern) |
| Auth | JWT HS256 (30 min), BCrypt (cost 12) |
| Container | Docker multi-stage, Docker Compose |

#### 10.2 Flux d'Une Requete API

```
Angular → POST /api/orders
  → API Gateway (port 8081)
    → Routing: /orders/** → Organization Service (port 8083)
      → JwtAuthenticationFilter → Valide JWT
      → @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', ...)")
      → CreateOrderUseCase.execute()
        → Verifie organizations actives
        → Verifie relation active
        → Reserve stock
        → Sauvegarde order + items
        → Log ORDER_CREATED event
      → Retourne OrderResponse
```

#### 10.3 Communication Inter-Services

```
Payment Service ──[Outbox]──→ RabbitMQ ──→ Notification Service
Organization Service ──[Events]──→ Identity Service (desactivation cascade)
Organization Service ──[HTTP]──→ Identity Service (verification utilisateur)
```

#### 10.4 Patterns Utilises

| Pattern | Utilisation |
|---|---|
| DDD | Aggregats (Order, Payment, User), Value Objects |
| Clean Architecture | Use Cases separes du Domain |
| CQRS-lite | Commandes distinctes des requetes |
| Outbox | Evenements fiables via base de donnees |
| Optimistic Locking | `@Version` sur les entites |
| State Machine | `OrderStatus` avec transitions validees |
| Event Sourcing (leger) | `OrderEvent`, `PaymentEvent` pour audit |

---

## PARTIE 2: AXES D'AMELIORATION

### A. Securite et Robustesse

#### A.1 Authentification et Autorisation

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| S1 | **Rate limiting par role** — Limiter differemment par role (ex: 10 req/min pour SHOP_AGENT vs 120 pour SYSTEM_ADMIN) | Haute | Eviter abus |
| S2 | **Refresh Token** — Implementer un refresh token pour eviter la deconnexion toutes les 30 min | Haute | UX |
| S3 | **RBAC granulaire par organisation** — Verifier que l'organizationId du JWT correspond a la ressource demandee (pas juste le role) | Haute | Securite |
| S4 | **Audit trail complet** — Enregistrer TOUS les acctions (pas seulement les paiements) | Moyenne | Conformite |
| S5 | **Rotations des cles JWT** — Permettre la rotation des secrets JWT sans deconnexion | Moyenne | Securite |
| S6 | **Password policy** — enforce complexite mot de passe (majuscule, chiffre, special, min 8 chars) | Moyenne | Securite |
| S7 | **Account lockout** — Verrouillage apres 5 tentatives echouees | Basse | Securite |

#### A.2 Donnees et Confidentialite

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| D1 | **Chiffrement des données sensibles** — Chiffrer les mots de passe, tokens en BDD | Haute | RGPD |
| D2 | **Anonymisation des logs** — Ne jamais logger de donnees personnelles | Haute | RGPD |
| D3 | **Soft delete** — Plutot que suppression physique, marquer comme supprime | Moyenne | Integrite |
| D4 | **Backup automatique** — Scripts de backup BDD avec retention | Moyenne | Disponibilite |

---

### B. Fonctionnel et Metier

#### B.1 Commandes et Livraisons

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| C1 | **Notifications de commande** — Envoyer des notifications pour chaque etat de commande (confirme, prepare, livre, etc.) | Haute | Suivi |
| C2 | **Prix a la creation** — Stocker le prix unitaire au moment de la commande (pas au moment de l'affichage) | Haute | Integrite |
| C3 | **Historique de prix** — Garder un historique des prix des produits | Moyenne | Traçabilité |
| C4 | **Commandes recurring/periodiques** — Permettre des commandes automatiques periodiques | Basse | Productivite |
| C5 | **Transporteur** — Ajouter un champ transporteur avec suivi | Basse | Logistique |
| C6 | **Retour de marchandise** — Workflow de retour (RETURNED status) | Moyenne | Metier |

#### B.2 Paiements

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| P1 | **Paiement partiel** — Accepter des paiements partiels (solde restant) | Haute | Metier |
| P2 | **Multi-devises** — Gestion correcte des taux de change | Moyenne | International |
| P3 | **Factures/Recus** — Generer des factures PDF | Moyenne | Comptabilité |
| P4 | **Rapprochement bancaire** — Importer les releves bancaires | Basse | Comptabilité |
| P5 | **Paiement recurrent** — Abonnements pour commandes regulieres | Basse | Metier |
| P6 | **Intégration Stripe/ PayPal** — Paiement en ligne | Basse | Paiement |

#### B.3 Catalogue et Stock

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| K1 | **Images de produits** — Upload et gestion d'images | Haute | UX |
| K2 | **Import/Export CSV** — Import en masse de produits et stocks | Haute | Productivite |
| K3 | **Alertes de stock bas** — Notifications automatiques quand stock < seuil | Haute | Gestion |
| K4 | **Code-barres/QR** — Generer et scanner des codes-barres | Moyenne | Logistique |
| K5 | **Variantes de produits** — Tailles, couleurs (produits composites) | Basse | Metier |
| K6 | **Historique des prix d'achat** — Tracker les variations de cout | Moyenne | Analyse |

---

### C. Architecture et Performance

#### C.1 Scalabilite

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| A1 | **Cache Redis** — Cache pour les donnees frequentes (catalogue, stats) | Haute | Performance |
| A2 | **API Gateway stateless** — Deployer plusieurs instances avec load balancer | Moyenne | Disponibilite |
| A3 | **Read replicas MySQL** — Replicas en lecture pour les requetes analytiques | Moyenne | Performance |
| A4 | **Async processing** — Deplacer les operations lourdes en background (RabbitMQ) | Haute | Performance |
| A5 | **Pagination reelle** — Implementer la pagination côté backend (pas juste front-end) | Haute | Performance |
| A6 | **Indexation optimisee** — Verifier et optimiser les index MySQL | Moyenne | Performance |

#### C.2 Observabilite

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| O1 | **Centralized logging (ELK)** — Kibana pour les logs | Haute | Debug |
| O2 | **Distributed tracing (Jaeger/Zipkin)** — Suivi des requetes a travers les services | Haute | Debug |
| O3 | **Metrics (Prometheus/Grafana)** — Metriques metier et techniques | Haute | Monitoring |
| O4 | **Health checks detailles** — Checks DB, RabbitMQ, ES dans chaque service | Moyenne | Disponibilite |
| O5 | **Alerting** — Alerts automatiques sur erreurs, latence, stock | Moyenne | Ops |

#### C.3 Fiabilite

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| R1 | **Circuit Breaker (Resilience4j)** — Eviter les cascades d'erreurs | Haute | Fiabilite |
| R2 | **Retry with backoff** — Reessayer automatiquement les appels echoues | Haute | Fiabilite |
| R3 | **Idempotency keys** — Eviter les doublons de creation | Haute | Fiabilite |
| R4 | **Dead Letter Queue** — Files DLQ pour les messages non traites | Moyenne | Fiabilite |
| R5 | **Database migrations** — Procedures de migration sans downtime | Moyenne | Disponibilite |

---

### D. Frontend et UX

#### D.1 Interface Utilisateur

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| U1 | **Dark mode** — Mode sombre pour les interfaces longue duree | Basse | UX |
| U2 | **Export PDF/Excel** — Exporter les listes (commandes, paiements, stocks) | Haute | Productivite |
| U3 | **Dashboard personnalise** — Widgets configurables par role | Moyenne | UX |
| U4 | **Recherche globale** — Barre de recherche unifiee (commandes, paiements, produits) | Moyenne | UX |
| U5 | **Notifications push reelles** — Web Push au lieu du polling | Haute | UX |
| U6 | **Mode hors-ligne** — PWA amelioree avec sync automatique | Basse | UX |
| U7 | **Accessibilité (a11y)** — WCAG 2.1 AA compliance | Moyenne | Conformite |
| U8 | **Internationalisation (i18n)** — Support multilingue (FR, EN, AR) | Basse | International |

#### D.2 Mobile

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| M1 | **Application native** — React Native ou Flutter pour les livreurs | Basse | Mobile |
| M2 | **Scanner QR ameliore** — Support multi-scan, historique des scans | Basse | Mobile |
| M3 | **Gestures avances** — Swipe pour actions rapides (confirmer, rejeter) | Basse | UX |

---

### E. DevOps et Deploiement

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| E1 | **CI/CD Pipeline** — GitHub Actions pour build, test, deploy automatique | Haute | Productivite |
| E2 | **Environment staging** — Copie de production pour les tests | Haute | Qualite |
| E3 | **Kubernetes** — Migration de Docker Compose vers K8s | Basse | Production |
| E4 | **Terraform/IaC** — Infrastructure as Code | Basse | Reproductibilite |
| E5 | **Secrets management** — Vault ou AWS Secrets Manager | Haute | Securite |
| E6 | **Blue/Green deployment** — Deploiement sans downtime | Moyenne | Disponibilite |
| E7 | **Feature flags** — Activer/desactiver des fonctionnalites sans redeploy | Basse | Agile |

---

### F. Tests et Qualite

| # | Amelioration | Priorite | Impact |
|---|---|---|---|
| T1 | **Tests unitaires backend** — Couverture > 80% sur les Use Cases | Haute | Qualite |
| T2 | **Tests d'integration** — Tests end-to-end avec Testcontainers | Haute | Qualite |
| T3 | **Tests de performance** — JMeter/Gatling pour les flux critiques | Haute | Performance |
| T4 | **Mutation testing** — PIT pour verifier la qualite des tests | Basse | Qualite |
| T5 | **Contract testing** — Pact pour les interfaces entre services | Basse | Fiabilite |
| T6 | **Code review obligatoire** — PR review avant merge | Haute | Qualite |

---

## PARTIE 3: FEUILLE DE ROUTE

### Phase 1 (Court terme — 1-2 mois)
- [ ] Refresh Token (S2)
- [ ] Notifications de commande (C1)
- [ ] Alertes de stock bas (K3)
- [ ] Import/Export CSV (K2)
- [ ] Images de produits (K1)
- [ ] Pagination reelle (A5)
- [ ] Circuit Breaker (R1)
- [ ] Tests unitaires Use Cases (T1)

### Phase 2 (Moyen terme — 3-6 mois)
- [ ] Cache Redis (A1)
- [ ] Distributed tracing (O2)
- [ ] Metrics Prometheus/Grafana (O3)
- [ ] Factures PDF (P3)
- [ ] Paiement partiel (P1)
- [ ] Export PDF/Excel (U2)
- [ ] Notifications push reelles (U5)
- [ ] CI/CD Pipeline (E1)
- [ ] Environment staging (E2)
- [ ] Tests d'integration (T2)

### Phase 3 (Long terme — 6-12 mois)
- [ ] Kubernetes (E3)
- [ ] Application mobile livreur (M1)
- [ ] Dark mode (U1)
- [ ] Internationalisation (U8)
- [ ] Commandes recurring (C4)
- [ ] Integration paiement en ligne (P6)
- [ ] Feature flags (E7)
- [ ] Contract testing (T5)
