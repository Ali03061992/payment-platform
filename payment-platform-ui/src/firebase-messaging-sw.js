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
}

var messaging = __messaging;

if (messaging) {
  messaging.onBackgroundMessage(function(payload) {
    const notificationTitle = payload.notification?.title || 'NEXTERA';
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

function resolveTargetUrl(data) {
  try {
    return new URL((data && data.url) || '/dashboard', self.registration.scope).href;
  } catch (e) {
    return new URL('/dashboard', self.registration.scope).href;
  }
}

self.addEventListener('notificationclick', function(event) {
  event.notification.close();
  const targetUrl = resolveTargetUrl(event.notification.data || {});

  event.waitUntil((async function() {
    const clientList = await clients.matchAll({ type: 'window', includeUncontrolled: true });
    for (const client of clientList) {
      if (new URL(client.url).origin !== self.location.origin) continue;
      // PWA déjà ouverte : naviguer vers le deep-link puis focus (sinon le clic
      // ne faisait que ramener l'app sur l'écran courant).
      if ('navigate' in client) {
        try { await client.navigate(targetUrl); } catch (e) {}
      }
      if ('focus' in client) return client.focus();
    }
    return clients.openWindow(targetUrl);
  })());
});
