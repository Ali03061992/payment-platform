import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/notifications/notification_providers.dart';
import '../../../core/auth/auth_provider.dart';
import '../../../core/router/deep_link_mapper.dart';

class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});
  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  int _page = 0;

  @override
  Widget build(BuildContext context) {
    final list = ref.watch(notificationsListProvider(_page));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Notifications'),
        actions: [
          TextButton(
            onPressed: () async {
              await ref.read(notificationApiProvider).markAllRead();
              ref.invalidate(notificationsListProvider);
              ref.read(unreadCountProvider.notifier).refresh();
            },
            child: const Text('Tout lire', style: TextStyle(color: Colors.white)),
          ),
        ],
      ),
      body: list.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('Erreur : $e')),
        data: (paged) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(notificationsListProvider);
            await ref.read(unreadCountProvider.notifier).refresh();
          },
          child: ListView.separated(
            itemCount: paged.items.length,
            separatorBuilder: (_, __) => const Divider(height: 1),
            itemBuilder: (_, i) {
              final n = paged.items[i];
              return ListTile(
                leading: Icon(n.read ? Icons.mark_email_read : Icons.mark_email_unread),
                title: Text(n.message, maxLines: 2, overflow: TextOverflow.ellipsis),
                subtitle: Text('${n.type} • ${n.createdAt.toLocal()}'),
                onTap: () async {
                  if (!n.read) {
                    await ref.read(notificationApiProvider).markRead(n.id);
                    ref.invalidate(notificationsListProvider);
                    ref.read(unreadCountProvider.notifier).refresh();
                  }
                  if (context.mounted) {
                    final route = routeForNotificationType(n.type);
                    if (route != '/notifications') context.push(route);
                  }
                },
              );
            },
          ),
        ),
      ),
      bottomNavigationBar: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(
              onPressed: _page > 0 ? () => setState(() => _page--) : null,
              icon: const Icon(Icons.chevron_left)),
          Text('Page ${_page + 1}'),
          IconButton(onPressed: () => setState(() => _page++), icon: const Icon(Icons.chevron_right)),
        ],
      ),
    );
  }
}
