# NEXTERA — Dossier fonctionnel

> Inventaire complet de l'état implémenté (web + API). La partie mobile (`payment-platform-mobile/`) est volontairement exclue de ce document.
> L'architecture, les API, la sécurité et le déploiement sont décrits dans [TECHNIQUE.md](TECHNIQUE.md).

NEXTERA est une plateforme B2B qui relie **fournisseurs** et **boutiques** : catalogue, stocks, commandes, livraisons, paiements, facturation conforme au droit tunisien, retenues à la source TEJ et notifications temps réel.

## 1. Rôles et comptes

| Rôle | Périmètre |
|---|---|
| `SYSTEM_ADMIN` | Administration globale : organisations, utilisateurs, relations, statistiques, journal d'audit |
| `SUPPLIER_ADMIN` | Gestion complète côté fournisseur (catalogue, stocks, commandes, agents, finances) |
| `SUPPLIER_AGENT` | Livreur : ses livraisons, acceptation, géolocalisation |
| `SHOP_ADMIN` | Gestion complète côté boutique (commandes, réception, paiements, litiges) |
| `SHOP_AGENT` | Exploitation boutique : livraisons assignées, réception, litiges et devis (pas de création/gestion de commandes ni de solde) |

### Authentification et gestion des accès
- Inscription, connexion, déconnexion, rafraîchissement de session (JWT).
- **Passkeys (WebAuthn)** : connexion sans mot de passe et gestion des appareils (renommage, révocation) depuis la page Sécurité ; fonctionnalité désactivée par défaut côté UI (`PASSKEYS_ENABLED`).
- Changement de mot de passe connecté ; mot de passe oublié (demande → validation → confirmation) ; première activation par lien (validation → définition du mot de passe).
- Création d'utilisateurs par l'admin, activation / désactivation de comptes.
- Agents fournisseurs : création, modification, activation / désactivation, liste par fournisseur.
- Préférences utilisateur persistées (thème, langue).
- Gardes de routes par authentification et par rôle côté web.

## 2. Administration (SYSTEM_ADMIN)

- **Fournisseurs** : création, liste, fiche, activation / désactivation, profil fiscal (MF, patente, identifiant TEJ, nature de retenue par défaut, adresse, régime, attestation de suspension).
- **Boutiques** : mêmes opérations que les fournisseurs.
- **Relations fournisseur ↔ boutique** : création, liste (par fournisseur / par boutique), suppression.
- **Utilisateurs** : création, liste, fiche, activation / désactivation.
- **Statistiques globales** : tableau de bord admin (comptes, organisations, activité).
- **Journal d'audit** : traçabilité des actions sensibles, consultation et filtrage.

## 3. Espace fournisseur

### Catalogue
- **Familles** de produits : création, modification, suppression, liste.
- **Catégories** : création, suppression, liste (rattachées aux familles).
- **Produits** : création, modification, désactivation, fiche complète :
  - prix unitaire HT ou **TTC** (conversion automatique en HT, sans TVA ajoutée à tort),
  - **TVA par produit** : 19 %, 13 %, 7 % ou 0 % motivé (art. 18 Code TVA),
  - assujettissement **FODEC 1 %** (inclus dans la base TVA),
  - SKU, unité, description, image (téléversement).

### Stocks
- Tableau de bord stock, gestion des quantités, **mouvements de stock** (entrées/sorties, historique).
- **Alertes stock bas** (seuil par produit, notification automatique).
- **Optimisation scientifique** : configuration et recommandations (quantités à recommander).

### Commandes et livraisons
- Liste, recherche, fiche détail (articles, événements, commentaires).
- **Devis** : sur demande boutique (brouillon/confirmée), modification des lignes et quantités, remises lignes (%) uniquement (aucune remise globale), envoi versionné avec notification à la boutique.
- Cycle complet : confirmation → préparation → prêt → affectation d'un livreur → livraison → réception confirmée par la boutique → **réception acceptée** (avec paiement automatique créé et certificat de retenue émis, avec rattrapage si manqué).
- Annulation, rejet, **recommande** (reorder), rejet de livraison, commentaires par commande.
- Suivi GPS : positions des livraisons, livraisons du jour, « mes livraisons » (livreur), agents boutique.
- Export CSV des commandes ; facture PDF (téléchargement + impression) ; **facture électronique TEIF v1.8.8** (XML à signer puis déposer sur TTN).
- Référence électronique externe (TTN/TEJ) saisie par commande, reprise dans le QR de la facture.

### Agents et finances
- Livreurs : liste, affectation aux livraisons, **paiements des agents** (synthèse par agent).
- **Soldes** : solde fournisseur global, solde par boutique, ajustements manuels.
- **Rapport financier fournisseur** (chiffre d'affaires, encaissements, retenues).

## 4. Espace boutique

- **Commandes** : création (manuelle ou depuis le catalogue), liste, recherche, fiche détail, modification (brouillon), annulation, réclamation (tout statut), **recommande**, paiement ASAP (acceptation + paiement immédiat).
- **Demande sans remise** : la boutique chiffre sans aucune remise (remises = devis fournisseur).
- **Devis reçus** : historique versionné par commande (totaux, date), téléchargement de chaque devis en PDF, notification à chaque envoi.
- **Réception** : acceptation / rejet de livraison, confirmation de date, personne ayant réceptionné.
- **Factures** : téléchargement PDF, impression, facture électronique TEIF, référence externe + QR.
- **Retenue à la source** : consultation du certificat de la facture, téléchargement PDF (QR + référence) et XML (dépôt TEJ), enregistrement de la référence TEJ après dépôt.
- **Soldes** : solde boutique, détail par fournisseur.
- **Réclamations** : page unifiée `/dashboard/reclamations` (boutique, fournisseur, admin), création sur n'importe quelle commande, détail et messagerie avec le fournisseur.

## 5. Paiements

- Création (manuelle ou **automatique** à la réception acceptée), liste, recherche par référence, fiche détail.
- Validation : confirmation, rejet, annulation ; facture PDF du paiement.
- **Paiements ASAP**, détection des **paiements en retard**, statistiques (par statut, par période, par fournisseur, synthèse agent).
- Exports (données + CSV).

## 6. Fiscalité tunisienne (facturation + TEJ)

- **Moteur fiscal** : ordre remise → FODEC 1 % dans la base → TVA par taux → TTC → **timbre fiscal 1,000 DT** hors base ; prix TTC avec TVA déduite ; 3 décimales ; ventilation par taux sur chaque facture.
- **Obligations** : MF + adresse obligatoires (fournisseur toujours ; boutique sauf non-assujetti/export), patente, mentions art. 18 (MF des 2 parties, HT, taux + montants, remises, série), mentions art. 11 pour suspension/export + attestation.
- **Facture PDF** : pleine page, totaux insécables, signatures, QR + référence externe, impression.
- **TEIF v1.8.8** : génération XML (MF 13 caractères, dates jjMMAA, codes I-*, timbre séparé, pas de RAS), bouton de téléchargement boutique + fournisseur.
- **Certificats de retenue (TEJ, art. 55)** : émission automatique à la livraison sur base TTC, natures (marchandises, services, loyers, honoraires, sans MF 15 %, …), priorité nature explicite > défaut fournisseur > auto ; échéance fin du mois suivant ; PDF + XML + lot mensuel XML ; marquage du dépôt (référence TEJ) ; certificats à échéance (rappel J-7 planifié) ; **synthèse mensuelle** par nature pour réconcilier la déclaration.
- **Guides intégrés** : page d'aide TEJ (dépôt pas à pas, lien officiel) et écran de **préparation du dépôt** (par référence, saisie libre, import du PDF du certificat avec extraction auto).

## 7. Litiges (réclamations)

- Page unique `/dashboard/reclamations` pour tous les rôles métier (boutique, fournisseur, admin) : liste paginée des litiges de l'organisation avec recherche, filtres statut/priorité et accès à la fiche avec fil de messages.
- Ouverture sur **n'importe quelle commande accessible, quel que soit son statut** (en cours, livrée, rejetée, annulée, etc.) ; un seul litige actif (`OPEN` / `IN_PROGRESS`) par commande (sinon `409`). La boutique peut ouvrir la réclamation depuis la fiche commande, le fournisseur depuis le détail de ses commandes.
- Priorités et SLA : `LOW` (7 j), `NORMAL` (72 h), `HIGH` (24 h), `URGENT` (4 h) ; échéance recalculée à chaque changement de priorité, retard signalé dans la liste et la fiche.
- Prise en charge et affectation : passage `OPEN` → `IN_PROGRESS`, assignation à un admin (ou à soi-même), affichage de l'assigné et de l'échéance.
- Cycle de vie : `OPEN` → `IN_PROGRESS` → `RESOLVED` / `CLOSED` ; résolution et clôture réservées aux admins ; chaque transition est historisée (statut, auteur, date, commentaire).
- Pièces jointes : upload PDF/JPEG/PNG (5 Mo max) par les participants, téléchargement authentifié ; notes internes visibles uniquement par les admins (2 000 caractères max).
- Notifications in-app et push aux deux parties, liens profonds vers `/dashboard/reclamations/{id}` ; les anciennes routes `shop/disputes` et `admin/disputes` redirigent vers la page unifiée.

## 8. Notifications

- Centre de notifications in-app : liste, compteur de non-lus, marquage lu / tout marquer lu.
- Temps réel : flux **SSE** + **push FCM** + **Web Push PWA** (abonnements, préférences, purge des endpoints expirés).
- Événements couverts : commandes, livraisons, paiements, stocks bas, litiges, rappel J-7 des retenues ; messages i18n stockés (clé + paramètres) et rendus dans la langue de l'utilisateur.

## 9. Expérience utilisateur (web)

- **3 langues** : français, anglais, arabe avec **RTL** ; sélecteur premium ; formats tunisiens (dates, montants TND).
- **Thèmes** : sélecteur d'accent persistant (base + saisie hexadécimale personnalisée), mode clair/sombre.
- **Visite guidée** (tour) des écrans, bannière + toasts, dialogues de confirmation, pagination, scanner **QR**, PWA (mise à jour).
- Tableaux de bord par rôle (admin, fournisseur, boutique, agent) avec notifications.

## 10. Socle technique (transverse)

- API Gateway : validation JWT, **liste blanche IP** (CIDR), **limitation de débit** (quotas renforcés sur l'auth).
- Microservices : identité, organisations/commandes/stocks, paiements, notifications ; événements outbox (RabbitMQ) ; idempotence.
- Journal d'audit, healthchecks, déploiement Docker multi-conteneurs, CI (build, tests, e2e).
