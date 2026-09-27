# 04 — Setup mobile

## Prérequis

Flutter 3.35+ (`flutter --version`), Dart 3.9+, Android Studio (SDK 34) + Xcode 16 (iOS), Firebase CLI, compte Firebase (projet `payment-platform`).

## Install

```bash
cd payment-platform-mobile
flutter pub get
cp .env.example .env   # API_BASE_URL=http://10.0.2.2:8081 (émulateur Android) ou http://localhost:8081
flutter analyze
flutter test
flutter run -d emulator   # ou chrome pour itération UI
```

`.env` : `API_BASE_URL`, `FCM_VAPID_KEY` (web/debug), `ENV=dev`. Jamais de secret Firebase commité.

## Firebase

1. `flutterfire configure` → génère `lib/firebase_options.dart`.
2. Android : `google-services.json` dans `android/app/`, canal `payment_high` créé au boot (`notification_channel.dart`).
3. iOS : `GoogleService-Info.plist` via Xcode, push + background modes (remote notifications), APNS key uploadée côté Firebase console.
4. Backend : `app.push.enabled=true` + service-account JSON (voir doc 03). Vérifier `POST /api/fcm-tokens` retourne 200 après login mobile.

## Build

```bash
flutter build apk --release                 # test terrain Android
flutter build appbundle --release           # Play Store
flutter build ipa --release                 # App Store (Xcode signing)
```

Versioning : `pubspec.yaml` (`version: 1.0.0+N`), changelog `CHANGELOG-MOBILE.md`.

## Vérification parité

- [ ] Login 4 rôles OK, SYSTEM_ADMIN → `/403`.
- [ ] Payments CRUD + Idempotency-Key, stats, scan QR, export.
- [ ] Orders tunnel complet (confirm→prepare→ready→assign→accept→deliver→accepted) + `asapPayment`.
- [ ] Stock/catalogue (ADMIN), deliveries, disputes, balances.
- [ ] Notifs : push < 5 s, tap → deep-link (doc 03), badge, mark-read, fallback polling sans Firebase.
