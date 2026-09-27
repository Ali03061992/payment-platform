import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/notifications/notification_providers.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Tableau de bord signature : header gradient avec avatar + cloche,
/// stats visuelles, tuiles d'action, activité récente.
class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);
    final roles = auth.roles;
    final username = auth.user?['username']?.toString() ?? '';
    final unread = ref.watch(unreadCountProvider);
    final isSupplier = roles.any((r) => r.startsWith('SUPPLIER'));
    final isAdmin = roles.any((r) => r.endsWith('_ADMIN'));

    return Scaffold(
      backgroundColor: Ds.pageBg,
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(_dashKey),
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(
              child: Container(
                padding: EdgeInsets.only(
                  top: MediaQuery.of(context).padding.top + 12,
                  left: 20,
                  right: 20,
                  bottom: 26,
                ),
                decoration: const BoxDecoration(
                  gradient: Ds.headerGradient,
                  borderRadius: BorderRadius.only(
                    bottomLeft: Radius.circular(30),
                    bottomRight: Radius.circular(30),
                  ),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            gradient: Ds.goldGradient,
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Center(
                            child: Text(
                              username.isNotEmpty ? username[0].toUpperCase() : '?',
                              style: const TextStyle(
                                  color: Colors.white, fontSize: 22, fontWeight: FontWeight.w800),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Bonjour,',
                                  style: TextStyle(color: Colors.white70, fontSize: 13)),
                              Text(username,
                                  style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 19,
                                      fontWeight: FontWeight.w800)),
                              Text(roles.join(' • '),
                                  style: const TextStyle(color: Colors.white54, fontSize: 11)),
                            ],
                          ),
                        ),
                        InkWell(
                          borderRadius: BorderRadius.circular(14),
                          onTap: () => context.push('/notifications'),
                          child: Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.14),
                              borderRadius: BorderRadius.circular(14),
                            ),
                            child: Badge(
                              isLabelVisible: unread > 0,
                              label: Text('$unread'),
                              child: const Icon(Icons.notifications_outlined,
                                  color: Colors.white),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _StatsBlock(isSupplier: isSupplier),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  const SectionHeader('Actions rapides'),
                  const SizedBox(height: 10),
                  GridView.count(
                    crossAxisCount: 4,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    mainAxisSpacing: 10,
                    crossAxisSpacing: 10,
                    childAspectRatio: 0.86,
                    children: [
                      if (!isSupplier) ...[
                        QuickTile(
                            icon: Icons.add_shopping_cart,
                            label: 'Commander',
                            gradient: const [Color(0xFF1E5AA8), Color(0xFF0F3460)],
                            onTap: () => context.push('/shop/orders/create')),
                        QuickTile(
                            icon: Icons.payments_rounded,
                            label: 'Payer',
                            gradient: const [Color(0xFFF0A22E), Color(0xFFD97B1A)],
                            onTap: () => context.push('/payments/create')),
                      ],
                      if (isSupplier && isAdmin) ...[
                        QuickTile(
                            icon: Icons.add_box_rounded,
                            label: 'Commander',
                            gradient: const [Color(0xFF1E5AA8), Color(0xFF0F3460)],
                            onTap: () => context.push('/supplier/orders/create')),
                        QuickTile(
                            icon: Icons.inventory_rounded,
                            label: 'Produit',
                            gradient: const [Color(0xFF0E9F8A), Color(0xFF0B7A6A)],
                            onTap: () => context.push('/supplier/stock/create')),
                      ],
                      QuickTile(
                          icon: Icons.qr_code_scanner_rounded,
                          label: 'Scanner',
                          gradient: const [Color(0xFF7C3AED), Color(0xFF5B21B6)],
                          onTap: () => context.push('/scan')),
                      QuickTile(
                          icon: Icons.file_download_rounded,
                          label: 'Export',
                          gradient: const [Color(0xFF0E9F8A), Color(0xFF0B7A6A)],
                          onTap: () => context.push('/export')),
                    ],
                  ),
                  const SizedBox(height: 18),
                  SectionHeader('Activité récente',
                      actionLabel: 'Tout voir',
                      onAction: () => context
                          .push(isSupplier ? '/supplier/orders' : '/shop/orders')),
                  const SizedBox(height: 10),
                  _RecentOrders(isSupplier: isSupplier),
                ]),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

final _dashKey = FutureProvider((_) async => true);

class _StatsBlock extends ConsumerWidget {
  final bool isSupplier;
  const _StatsBlock({required this.isSupplier});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(_dashKey);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(color: Colors.black.withValues(alpha: 0.18), blurRadius: 24, offset: const Offset(0, 10)),
        ],
      ),
      child: FutureBuilder(
        future: isSupplier ? _supplierStats(ref) : _shopStats(ref),
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator(color: Ds.accent)),
            );
          }
          if (snap.hasError) {
            return InkWell(
              onTap: () => ref.invalidate(_dashKey),
              child: Padding(
                padding: const EdgeInsets.all(8),
                child: Text('Toucher pour recharger',
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: Ds.muted)),
              ),
            );
          }
          final s = snap.data!;
          final bars = s['bars'] as List<double>;
          return Column(
            children: [
              Row(
                children: [
                  Expanded(
                    child: _MiniStat(
                        label: s['l1'] as String,
                        value: s['v1'] as String,
                        color: s['c1'] as Color,
                        onTap: () => context.push(s['r1'] as String)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _MiniStat(
                        label: s['l2'] as String,
                        value: s['v2'] as String,
                        color: s['c2'] as Color,
                        onTap: () => context.push(s['r2'] as String)),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(child: MiniBars(bars)),
                  const SizedBox(width: 12),
                  _MiniStat(
                      label: s['l3'] as String,
                      value: s['v3'] as String,
                      color: s['c3'] as Color,
                      onTap: () => context.push(s['r3'] as String)),
                ],
              ),
            ],
          );
        },
      ),
    );
  }

  Future<Map<String, Object>> _supplierStats(WidgetRef ref) async {
    final api = ref.read(supplierApiProvider);
    if (api == null) {
      return {
        'l1': 'En attente', 'v1': '0', 'c1': Ds.warningText, 'r1': '/payments',
        'l2': 'Commandes', 'v2': '0', 'c2': Ds.infoText, 'r2': '/supplier/orders',
        'l3': 'Stock bas', 'v3': '0', 'c3': Ds.dangerText, 'r3': '/supplier/low-stock-alerts',
        'bars': [0.0],
      };
    }
    final results = await Future.wait([
      api.supplierSummary(),
      api.orders(page: 0, size: 1),
      api.lowStockAlerts(),
    ]);
    final summary = asMap(results[0]);
    final pending = ((summary['pendingTotal'] as num?) ?? 0).toDouble();
    final confirmed = ((summary['confirmedTotal'] as num?) ?? 0).toDouble();
    return {
      'l1': 'Paiements en attente',
      'v1': '${summary['pendingCount'] ?? 0}',
      'c1': Ds.warningText,
      'r1': '/payments?status=PENDING',
      'l2': 'Commandes',
      'v2': '${(results[1] as dynamic).totalElements ?? 0}',
      'c2': Ds.infoText,
      'r2': '/supplier/orders',
      'l3': 'Stock bas',
      'v3': '${(results[2] as List).length}',
      'c3': Ds.dangerText,
      'r3': '/supplier/low-stock-alerts',
      'bars': [pending, confirmed],
    };
  }

  Future<Map<String, Object>> _shopStats(WidgetRef ref) async {
    final payments = ref.read(paymentsApiProvider);
    final shop = ref.read(shopApiProvider);
    final orgId = orgIdOf(ref.read(authProvider).user);
    final stats = await payments.stats();
    final orders = await shop.orders(page: 0, size: 1);
    double due = 0;
    if (orgId != null) {
      try {
        final balances = await shop.shopBalance(orgId);
        for (final b in balances) {
          due += ((b['remainingDue'] as num?) ?? 0).toDouble();
        }
      } catch (_) {}
    }
    final pending = ((stats['pending'] as num?) ?? 0).toDouble();
    final confirmed = ((stats['confirmed'] as num?) ?? 0).toDouble();
    final rejected = ((stats['rejected'] as num?) ?? 0).toDouble();
    final cancelled = ((stats['cancelled'] as num?) ?? 0).toDouble();
    return {
      'l1': 'En attente',
      'v1': '${stats['pending'] ?? 0}',
      'c1': Ds.warningText,
      'r1': '/payments?status=PENDING',
      'l2': 'Commandes',
      'v2': '${orders.totalElements}',
      'c2': Ds.infoText,
      'r2': '/shop/orders',
      'l3': 'Reste dû',
      'v3': fmtAmount(due),
      'c3': Ds.dangerText,
      'r3': '/shop/balance',
      'bars': [pending, confirmed, rejected, cancelled],
    };
  }
}

class _MiniStat extends StatelessWidget {
  final String label;
  final String value;
  final Color color;
  final VoidCallback onTap;
  const _MiniStat({required this.label, required this.value, required this.color, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(14),
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(width: 8, height: 8,
                  decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
              const SizedBox(width: 6),
              Expanded(
                child: Text(label,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(fontSize: 11.5, color: Ds.muted, fontWeight: FontWeight.w600)),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(value,
              style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w800, color: Ds.deepText)),
        ],
      ),
    );
  }
}

class _RecentOrders extends ConsumerWidget {
  final bool isSupplier;
  const _RecentOrders({required this.isSupplier});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(_dashKey);
    return FutureBuilder(
      future: ref.read(shopApiProvider).recentOrders(4),
      builder: (ctx, snap) {
        if (snap.connectionState == ConnectionState.waiting) {
          return const SizedBox(height: 120, child: Center(child: CircularProgressIndicator(color: Ds.accent)));
        }
        if (snap.hasError) {
          return DsCard(
            child: Row(
              children: [
                const Expanded(child: Text('Activité indisponible', style: TextStyle(color: Ds.muted))),
                TextButton(onPressed: () => ref.invalidate(_dashKey), child: const Text('Réessayer')),
              ],
            ),
          );
        }
        final items = snap.data ?? [];
        if (items.isEmpty) {
          return const EmptyView(message: 'Aucune activité pour le moment');
        }
        return Column(
          children: [
            for (final o in items)
              Padding(
                padding: const EdgeInsets.only(bottom: 10),
                child: AccentCard(
                  status: '${o['status'] ?? ''}',
                  onTap: () => context.push(isSupplier
                      ? '/supplier/orders/${o['id']}'
                      : '/shop/orders/${o['id']}'),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${o['reference'] ?? '—'}',
                                style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
                            Text(
                                '${isSupplier ? (o['shopName'] ?? '') : (o['supplierName'] ?? '')} • ${fmtDate(o['createdAt']?.toString())}',
                                style: const TextStyle(color: Ds.muted, fontSize: 12)),
                          ],
                        ),
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(fmtAmount(o['total'], '${o['currency'] ?? ''}'),
                              style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14)),
                          const SizedBox(height: 4),
                          StatusBadge('${o['status'] ?? ''}'),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}
