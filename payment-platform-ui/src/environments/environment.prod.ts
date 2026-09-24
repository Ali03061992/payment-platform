function runtimeEnv(name: string): string {
  try {
    const w = window as unknown as { __env?: Record<string, string> };
    return w.__env?.[name] ?? '';
  } catch {
    return '';
  }
}

export const environment = {
  production: true,
  appUrl: window.location.origin,
  firebase: {
    apiKey: runtimeEnv('FIREBASE_API_KEY'),
    authDomain: runtimeEnv('FIREBASE_AUTH_DOMAIN'),
    projectId: runtimeEnv('FIREBASE_PROJECT_ID'),
    storageBucket: runtimeEnv('FIREBASE_STORAGE_BUCKET'),
    messagingSenderId: runtimeEnv('FIREBASE_MESSAGING_SENDER_ID'),
    appId: runtimeEnv('FIREBASE_APP_ID')
  },
  fcmVapidKey: runtimeEnv('FCM_VAPID_KEY')
};
