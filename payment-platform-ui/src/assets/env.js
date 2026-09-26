/**
 * M7 : valeurs par défaut LOCALES (aucun secret — chaînes vides = push
 * désactivé proprement). Généré en déploiement depuis env.template.js ;
 * ne JAMAIS commiter de vraies clés ici.
 *
 * OAuth/passwordless (feature/oauth-passwordless) :
 *   IS_DEV vaut 'true' en local/dev et active le panneau dev du front
 *   (`window.__env.IS_DEV === 'true'`) + le endpoint backend
 *   POST /api/auth/dev-login (sans password). Format STRING 'true'/''
 *   (pas de booléen) car envsubst ne produit que des chaînes.
 *   En prod générée, IS_DEV est vide = mode prod sûr (panneau masqué).
 */
(function (window) {
  window.__env = window.__env || {};
  window.__env.FIREBASE_API_KEY = '';
  window.__env.FIREBASE_AUTH_DOMAIN = '';
  window.__env.FIREBASE_PROJECT_ID = '';
  window.__env.FIREBASE_STORAGE_BUCKET = '';
  window.__env.FIREBASE_MESSAGING_SENDER_ID = '';
  window.__env.FIREBASE_APP_ID = '';
  window.__env.FCM_VAPID_KEY = '';
  // OAuth/passwordless (dev local) : 'true' => panneau dev visible
  // (window.__env.IS_DEV === 'true'). Prod generee : '' (vide = sur).
  window.__env.IS_DEV = 'true';
})(typeof window !== 'undefined' ? window : this);
