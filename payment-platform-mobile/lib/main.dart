import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'app.dart';
import 'core/auth/auth_provider.dart';
import 'core/notifications/fcm_service.dart';
import 'core/notifications/notification_providers.dart';
import 'core/notifications/notif_pop.dart';
import 'core/notifications/notify_signal.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env', isOptional: true);
  try {
    await Firebase.initializeApp();
  } catch (_) {}
  runApp(const ProviderScope(child: _Bootstrap()));
}

class _Bootstrap extends ConsumerStatefulWidget {
  const _Bootstrap();
  @override
  ConsumerState<_Bootstrap> createState() => _BootstrapState();
}

class _BootstrapState extends ConsumerState<_Bootstrap> {
  FcmService? _fcm;

  @override
  void initState() {
    super.initState();
    Future.microtask(() async {
      await ref.read(authProvider.notifier).bootstrap();
      final api = ref.read(notificationApiProvider);
      _fcm = FcmService(
        api: api,
        onDeepLink: (route) {
          if (route == '__refresh__') {
            ref.read(unreadCountProvider.notifier).refresh();
          }
        },
        onPush: (title, body, route) {
          // Signal instantané : son + vibration + bannière + compteur.
          NotifySignal.bam();
          notifPopBus.pop(NotifPop(title: title, body: body, route: route));
          ref.read(unreadCountProvider.notifier).refresh();
        },
      );
      await _fcm!.init();
      if (ref.read(authProvider).isLoggedIn) {
        await _fcm!.requestPermissionAndRegister();
        ref.read(unreadCountProvider.notifier).startPolling();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(authProvider, (_, next) {
      if (next.isLoggedIn) {
        _fcm?.requestPermissionAndRegister();
        ref.read(unreadCountProvider.notifier).startPolling();
      } else {
        ref.read(unreadCountProvider.notifier).stopPolling();
      }
    });
    return const PaymentApp();
  }
}
