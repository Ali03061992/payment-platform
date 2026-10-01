import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/theme/app_theme.dart';
import 'core/router/app_router.dart';
import 'core/notifications/pop_banner.dart';

class PaymentApp extends ConsumerWidget {
  const PaymentApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = AppRouter.build(ref);
    return MaterialApp.router(
      title: 'Payment Platform',
      theme: appTheme,
      routerConfig: router,
      debugShowCheckedModeBanner: false,
      builder: (ctx, child) => Stack(
        children: [
          child ?? const SizedBox.shrink(),
          const PopBanner(),
        ],
      ),
    );
  }
}
