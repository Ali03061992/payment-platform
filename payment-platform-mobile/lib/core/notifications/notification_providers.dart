import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/auth_provider.dart';

final unreadCountProvider = StateNotifierProvider<UnreadCountNotifier, int>((ref) => UnreadCountNotifier(ref));

class UnreadCountNotifier extends StateNotifier<int> {
  final Ref ref;
  Timer? _poll;
  UnreadCountNotifier(this.ref) : super(0);

  void startPolling() {
    stopPolling();
    refresh();
    _poll = Timer.periodic(const Duration(seconds: 30), (_) => refresh());
  }

  void stopPolling() {
    _poll?.cancel();
    _poll = null;
  }

  Future<void> refresh() async {
    if (!ref.read(authProvider).isLoggedIn) return;
    try {
      state = await ref.read(notificationApiProvider).unreadCount();
    } catch (_) {}
  }
}

final notificationsListProvider =
    FutureProvider.family((Ref ref, int page) => ref.watch(notificationApiProvider).list(page: page));
