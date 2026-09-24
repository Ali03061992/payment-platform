/**
 * M7 : valeurs par défaut LOCALES (aucun secret — chaînes vides = push
 * désactivé proprement). Généré en déploiement depuis env.template.js ;
 * ne JAMAIS commiter de vraies clés ici.
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
})(typeof window !== 'undefined' ? window : this);
