import 'package:flutter_local_notifications/flutter_local_notifications.dart';

Future<void> ensurePaymentChannel(FlutterLocalNotificationsPlugin plugin) async {
  const channel = AndroidNotificationChannel(
    'payment_high',
    'Paiements',
    description: 'Notifications paiements, commandes et stock',
    importance: Importance.max,
  );
  await plugin
      .resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>()
      ?.createNotificationChannel(channel);
}
