import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'notification_api.dart';
import '../router/deep_link_mapper.dart';

@pragma('vm:entry-point')
Future<void> firebaseMessagingBackgroundHandler(RemoteMessage message) async {}

class FcmService {
  final NotificationApi api;
  final FlutterLocalNotificationsPlugin local = FlutterLocalNotificationsPlugin();
  final void Function(String route)? onDeepLink;
  final void Function(String title, String body, String route)? onPush;
  String? _token;

  FcmService({required this.api, this.onDeepLink, this.onPush});

  String? get token => _token;

  Future<void> init() async {
    try {
      await local.initialize(
        const InitializationSettings(
          android: AndroidInitializationSettings('@mipmap/ic_launcher'),
          iOS: DarwinInitializationSettings(),
        ),
        onDidReceiveNotificationResponse: (resp) {
          final route = resp.payload ?? '/notifications';
          onDeepLink?.call(route);
        },
      );
      FirebaseMessaging.onBackgroundMessage(firebaseMessagingBackgroundHandler);
      FirebaseMessaging.onMessage.listen(_onForeground);
      FirebaseMessaging.onMessageOpenedApp.listen(_onOpened);
      FirebaseMessaging.instance.onTokenRefresh.listen((t) async {
        _token = t;
        await api.registerToken(t);
      });
    } catch (_) {}
  }

  Future<void> requestPermissionAndRegister() async {
    try {
      final settings = await FirebaseMessaging.instance.requestPermission(alert: true, badge: true, sound: true);
      if (settings.authorizationStatus != AuthorizationStatus.authorized &&
          settings.authorizationStatus != AuthorizationStatus.provisional) {
        return;
      }
      _token = await FirebaseMessaging.instance.getToken();
      await api.registerToken(_token);
      final initial = await FirebaseMessaging.instance.getInitialMessage();
      if (initial != null) _onOpened(initial);
    } catch (_) {}
  }

  Future<void> _onForeground(RemoteMessage msg) async {
    final data = msg.data;
    final route = mapBackendUrlToMobileRoute(data['url'], tag: data['tag']);
    final title = msg.notification?.title ?? 'Payment Platform';
    final body = msg.notification?.body ?? '';
    await local.show(
      msg.hashCode,
      title,
      body,
      const NotificationDetails(
        android: AndroidNotificationDetails(
          'payment_high',
          'Paiements',
          importance: Importance.max,
          priority: Priority.high,
          tag: 'payment-notification',
          playSound: true,
          enableVibration: true,
          enableLights: true,
        ),
        iOS: DarwinNotificationDetails(
            presentAlert: true, presentBadge: true, presentSound: true),
      ),
      payload: route,
    );
    onPush?.call(title, body, route);
    onDeepLink?.call('__refresh__');
  }

  void _onOpened(RemoteMessage msg) {
    final route = mapBackendUrlToMobileRoute(msg.data['url'], tag: msg.data['tag']);
    onDeepLink?.call(route);
  }
}
