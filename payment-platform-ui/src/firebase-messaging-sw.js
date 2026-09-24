importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');
// M7 : clés via env runtime (généré en déploiement depuis assets/env.template.js).
importScripts('/assets/env.js');

var __cfg = (self.__env || {});
var __messaging = null;
if (__cfg.FIREBASE_API_KEY && __cfg.FIREBASE_PROJECT_ID
    && __cfg.FIREBASE_MESSAGING_SENDER_ID && __cfg.FIREBASE_APP_ID) {
  firebase.initializeApp({
    apiKey: __cfg.FIREBASE_API_KEY,
    authDomain: __cfg.FIREBASE_AUTH_DOMAIN,
    projectId: __cfg.FIREBASE_PROJECT_ID,
    storageBucket: __cfg.FIREBASE_STORAGE_BUCKET,
    messagingSenderId: __cfg.FIREBASE_MESSAGING_SENDER_ID,
    appId: __cfg.FIREBASE_APP_ID
  });
  __messaging = firebase.messaging();
} else {
  console.info('[firebase-messaging-sw.js] Push non configuré (clés absentes).');
}

var messaging = __messaging;

if (messaging) {
  messaging.onBackgroundMessage(function(payload) {
    console.log('[firebase-messaging-sw.js] Received background message ', payload);
    const notificationTitle = payload.notification?.title || 'Payment Platform';
    const notificationOptions = {
      body: payload.notification?.body || '',
      icon: '/assets/icons/icon-192x192.png',
      badge: '/assets/icons/icon-72x72.png',
      tag: payload.data?.tag || 'payment-notification',
      data: payload.data || {}
    };

    self.registration.showNotification(notificationTitle, notificationOptions);
  });
}

self.addEventListener('notificationclick', function(event) {
  event.notification.close();
  const urlToOpen = event.notification.data?.url || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function(clientList) {
      for (const client of clientList) {
        if (client.url.includes(self.location.origin) && 'focus' in client) {
          return client.focus();
        }
      }
      return clients.openWindow(urlToOpen);
    })
  );
});
