/**
 * M7 : gabarit de configuration push (Firebase/FCM) — JAMAIS de secret ici.
 *
 * Déploiement : générer `assets/env.js` depuis les variables d'environnement,
 * par ex. avec envsubst :
 *
 *   envsubst < src/assets/env.template.js > dist/.../browser/assets/env.js
 *
 * Variables requises (vides = push désactivé proprement, sans erreur) :
 *   FIREBASE_API_KEY, FIREBASE_AUTH_DOMAIN, FIREBASE_PROJECT_ID,
 *   FIREBASE_STORAGE_BUCKET, FIREBASE_MESSAGING_SENDER_ID, FIREBASE_APP_ID,
 *   FCM_VAPID_KEY
 *
 * Voir deploy/.env.example.
 */
(function (window) {
  window.__env = window.__env || {};
  window.__env.FIREBASE_API_KEY = '${FIREBASE_API_KEY}';
  window.__env.FIREBASE_AUTH_DOMAIN = '${FIREBASE_AUTH_DOMAIN}';
  window.__env.FIREBASE_PROJECT_ID = '${FIREBASE_PROJECT_ID}';
  window.__env.FIREBASE_STORAGE_BUCKET = '${FIREBASE_STORAGE_BUCKET}';
  window.__env.FIREBASE_MESSAGING_SENDER_ID = '${FIREBASE_MESSAGING_SENDER_ID}';
  window.__env.FIREBASE_APP_ID = '${FIREBASE_APP_ID}';
  window.__env.FCM_VAPID_KEY = '${FCM_VAPID_KEY}';
})(typeof window !== 'undefined' ? window : this);
