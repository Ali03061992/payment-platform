Étude de faisabilité --- Notifications PWA : ouverture au clic et session persistante
Date : 19/10/2026
Cible : release/1.3.0
Plateforme prioritaire : PWA Android
Statut : Étude --- décision attendue
Synthèse exécutive
L'objectif est de garantir qu'un clic sur une notification rouvre la PWA
sur la page attendue et que l'utilisateur retrouve sa session après
fermeture puis relance de l'application, tout en maintenant la réception
des notifications lorsque l'application est fermée. Le Web Push VAPID
est considéré comme fonctionnel hors session ; ce volet n'est donc pas à
redévelopper.
Recommandation : avancer par étapes. Tester d'abord le changement de
comportement du clic sur notification, puis traiter la restauration de
session après avoir décidé explicitement du stockage des tokens. Ne pas
considérer openWindow comme une garantie de lancement de la WebAPK, ni
localStorage comme une décision de sécurité neutre. Le Service Worker
personnalisé et le cookie HttpOnly restent des options à instruire
selon les résultats des tests et l'architecture effective.
1. Contexte et objectifs
  Réf.                    Besoin                  État / constat
  B1                      Le clic sur une         Le push est reçu et
                          notification doit       affiché ; le clic ne
                          rouvrir l'application   rouvre rien ou ouvre un
                          installée sur la bonne  contexte incorrect.
                          page.                   
  B2                      La session doit être    La relance renvoie
                          restaurée après         actuellement vers
                          fermeture et relance de l'écran de connexion.
                          la PWA.                 
  B3                      Les notifications       Considéré comme acquis
                          doivent continuer à     : Web Push VAPID
                          arriver application     fonctionne hors
                      fermée.                 session.
Contraintes : livraison progressive, absence de régression sur
Angular NGSW, SSE, login et passkeys. La cible initiale de recette est
Android ; iOS est hors périmètre de cette phase.
2. État des lieux technique
2.1 Chaîne de notification
Le backend construit un payload de notification contenant notamment
title, body, icon, badge, tag et data.url. Le payload
comprend également une action onActionClick utilisant
navigateLastFocusedOrOpen. Le navigateur traite le push via le Service
Worker Angular ngsw-worker.js, qui possède déjà les handlers push et
notificationclick.
- Backend mentionné dans l'étude source : WebPushSender.java,
  construction du payload.
- Le canal FCM est déclaré désactivé ; seul Web Push est actif.
- L'abonnement push est indépendant de la session Angular.
2.2 Hypothèse sur le clic
L'étude source attribue le problème au comportement
navigateLastFocusedOrOpen : un onglet existant sur la même origine
peut être choisi avant l'ouverture d'une nouvelle fenêtre. Elle relève
également que la fermeture immédiate de la notification peut masquer un
échec ultérieur. Ces éléments doivent être confirmés par des tests sur
les appareils cibles.
2.3 Session et tokens
Selon l'étude source, les informations d'authentification sont
actuellement stockées dans sessionStorage et lues par plusieurs
services. Une relance de la WebAPK crée un nouveau contexte de
navigation et le stockage de session n'est plus disponible, ce qui
conduit les guards à rediriger vers /login.
  Élément                             Valeur indiquée dans l'étude source
  Access token                        30 minutes
  Refresh token                       7 jours, avec rotation à chaque
                                      usage
  Expiration                          Au-delà du TTL serveur, une
                                      nouvelle authentification est
                                  nécessaire.
3. Options étudiées et arbitrages
3.1 Ouverture de l'application au clic
  Option            Avantages         Limites / risques Recommandation
  A. Modifier     Modification      Le lancement de   À tester en
  l'opération en    limitée du        la WebAPK n'est   premier.
  openWindow    payload ; pas de  pas garanti sur
                    Service Worker    tous les
                    personnalisé.     appareils ;
                                      comportement
                                      desktop
                                      différent.        
  B. Service      Contrôle plus fin Dette de          Seulement si
  Worker            du clic, de la    maintenance et    l'option A échoue
  personnalisé    navigation et des compatibilité à   et que l'échec
                    erreurs.          gérer avec        est reproduit.
                                      Angular NGSW.     
  C. Retirer le   Aucune valeur     Comportement      Écartée en
  handler NGSW    démontrée dans    natif moins       l'état.
                    l'étude.          maîtrisé ;
                                      nécessite une
                                      autre stratégie
                                      de Service
                                  Worker.           
Le deep-link doit être validé côté client : seuls les chemins internes
autorisés doivent être acceptés. La présence d'une URL dans le payload
ne garantit pas, à elle seule, que la navigation finale aboutira à la
page souhaitée.
3.2 Persistance de la session
  Option             Avantages         Limites / risques Recommandation
  A.               Changement        Les tokens        Option
  localStorage   potentiellement   restent           transitoire
                     limité ;          accessibles au    possible après
                     persistance après JavaScript ; un   acceptation
                     relance ; aucun   XSS peut exposer  explicite du
                     changement        le refresh token. risque et
                     backend                             protections XSS.
                     obligatoire dans
                     le scénario
                     actuel.                             
  B. Refresh token Le JavaScript ne  Modifications     À étudier dès la
  en cookie          peut pas lire     backend, proxy et phase de
  HttpOnly       directement le    flux frontend ;   conception ;
                     refresh token.    traiter CSRF/CORS choix privilégié
                                       et les attributs  si l'architecture
                                   du cookie.        le permet.
L'access token peut être conservé en mémoire lorsque l'architecture le
permet. L'état de connexion doit être établi à partir d'une session
validée, et non de la seule présence d'une valeur dans le stockage
local. Les données utilisateur persistées doivent être minimales et
revalidées côté serveur.
4. Architecture cible
  Composant                           Responsabilité
  Web Push / Service Worker           Réception des notifications même
                                      lorsque l'application Angular n'est
                                      pas ouverte.
  Gestion du clic                     Valider le deep-link et ouvrir ou
                                      focaliser le contexte adapté selon
                                      le comportement réel du navigateur.
  Initialisation Angular              Restaurer et valider la session
                                      avant de laisser les guards
                                      autoriser l'accès.
  Gestion d'authentification          Centraliser le refresh, les erreurs
                                      401, la révocation et le nettoyage
                                      au logout.
  Backend                             Rester l'autorité pour la validité
                                      des tokens, la session et les
                                  permissions.
Parcours cible : notification reçue → clic → ouverture de
l'application et conservation du deep-link → restauration/validation de
session → affichage de la page demandée. Si la session a expiré ou a été
révoquée, redirection vers /login puis retour à la destination
initiale après authentification.
5. Plan de réalisation proposé
  Phase                   Travaux                 Livrable / sortie
  1. Diagnostic et      Vérifier le payload,    Résultats
  clic                  tester openWindow sur reproductibles ;
                          les appareils Android   décision sur le besoin
                          retenus, avec et sans   éventuel d'un Service
                          onglet Chrome existant. Worker personnalisé.
  2. Restauration de    Décider du stockage des Session restaurée de
  session               tokens ; centraliser    façon cohérente et
                          lecture et refresh ;    sécurisée.
                          gérer migration,
                          expiration, logout et
                          deep-link.              
  3. Configuration et   Vérifier le TTL         Checklist de recette
  recette               effectif dans           validée et risques
                          Spring/Docker ; tester  résiduels documentés.
                          refresh concurrent,
                      SSE, passkeys et rôles. 
6. Sécurité et risques
  Risque                  Impact                  Mesure proposée
  Tokens accessibles au   Exposition possible en  Privilégier HttpOnly
  JavaScript              cas de XSS.             pour le refresh token ;
                                                  si localStorage est
                                                  retenu, documenter le
                                                  risque, renforcer les
                                                  protections XSS et
                                                  limiter les privilèges
                                                  des tokens.
  Ouverture dans un       Parcours utilisateur    Tester sur les
  onglet au lieu de la    interrompu.             appareils cibles ; ne
  PWA                                             développer un Service
                                                  Worker personnalisé
                                                  qu'après preuve
                                                  d'échec.
  Refresh concurrents     Rotation incohérente ou Centraliser le refresh
                          session invalidée.      et empêcher les
                                                  renouvellements
                                                  simultanés pour une
                                                  même session.
  Session persistante sur Accès non souhaité si   Logout explicite,
  appareil partagé        le téléphone est        révocation serveur
                          partagé.                lorsque disponible et
                                                  durée de session
                                                  documentée.
  TTL mal interprété      Session plus longue ou  Définir expiration
                          plus courte que prévu.  absolue, expiration
                                                  glissante et
                                                  comportement
                                                  d'inactivité ; vérifier
                                                  la configuration
                                                  réellement injectée.
  Deep-link non validé    Navigation externe ou   Autoriser uniquement
                          inattendue.             les routes internes
                                                  attendues et préserver
                                                  la destination durant
                                              la reconnexion.
7. Critères d'acceptation
- [ ] Application fermée : la notification est reçue et le clic ouvre
  le contexte attendu sur le deep-link.
- [ ] Un onglet Chrome et la PWA sont présents : le clic ne se perd
  pas dans un contexte inadapté.
- [ ] Relance depuis l'icône : la session valide est restaurée sans
  ressaisie des identifiants.
- [ ] Session expirée ou révoquée : aucun accès protégé n'est accordé
  ; retour propre vers /login.
- [ ] Après reconnexion, la destination initialement demandée est
  restaurée.
- [ ] Logout : arrêt de SSE, nettoyage du stockage et révocation
  serveur lorsque l'API le permet.
- [ ] Plusieurs demandes de refresh simultanées ne provoquent pas
  d'invalidation accidentelle.
- [ ] Aucune régression sur les passkeys, badges, SSE, pages admin,
  boutique et fournisseur.
- [ ] Le comportement reste fonctionnel après mise à jour et
  activation du Service Worker.
8. TTL et durée de session
Le TTL de 30 jours est une proposition métier à confirmer, pas une
garantie automatique de 30 jours d'inactivité. La décision doit préciser
si le TTL est absolu ou glissant, si la rotation renouvelle l'échéance,
comment le serveur révoque une session et ce qui se passe après une
longue période sans utilisation.
- Vérifier que la propriété Spring est bien alimentée par la variable
  de déploiement attendue.
- Tester l'expiration côté serveur ; la présence de données locales ne
  doit pas prolonger la validité d'une session expirée.
- Ne pas exposer un secret ou une valeur sensible dans les journaux de
  diagnostic.
9. Décisions attendues
  Réf.                    Décision proposée       Statut
  D1                      Autoriser               À valider
                          l'expérimentation
                          openWindow comme
                          premier test, sans
                          garantie préalable sur
                          le lancement de la
                          WebAPK.                 
  D2                      Valider le besoin de    À valider
                          persistance ; arrêter
                          le choix localStorage
                          vs cookie après analyse
                          sécurité et
                          architecture.           
  D3                      Étudier le refresh      À valider
                          token en cookie
                          HttpOnly dès la
                          conception de la
                          solution.               
  D4                      Confirmer 30 jours      À valider
                          après clarification des
                          règles d'expiration, de
                          rotation et de
                          révocation.             
  D5                      Maintenir le Service    À valider
                          Worker personnalisé
                          hors périmètre initial
                          sauf échec prouvé de
                      l'option A.             
10. Estimation
L'estimation initiale de 2,5 jours peut être conservée comme
hypothèse pour le scénario minimal : modification du payload, adaptation
de la persistance et tests limités. Elle doit être revalidée après
inspection du code. Une implémentation cookie HttpOnly, une refonte du
flux de refresh ou un Service Worker personnalisé nécessiteraient une
estimation complémentaire.
  Lot                            Estimation initiale Réserve
  Payload et test                              0,5 j Selon les tests et le
  backend                                            comportement du
                                                     navigateur.
  Frontend session                               1 j Dépend du stockage
                                                     retenu et de la
                                                     gestion du refresh.
  Configuration et                             0,5 j À confirmer selon les
  tests                                              environnements.
  Recette manuelle                             0,5 j Prévoir les appareils
                                                     et versions Android
                                                     ciblés.
  Total indicatif                      2,5 j Ne couvre pas une
                                                     refonte sécurité
                                                 importante.
Conclusion
L'objectif fonctionnel est faisable, sous réserve de confirmer le
comportement du clic sur les appareils cibles et de choisir un mécanisme
de restauration de session cohérent avec les exigences de sécurité. La
démarche recommandée est progressive : tester d'abord l'ouverture de
notification, décider ensuite du modèle de session, puis valider la
recette complète. Aucune garantie de lancement de la WebAPK ou de
persistance exacte ne doit être annoncée avant les tests.