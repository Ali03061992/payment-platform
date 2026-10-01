import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/notifications/notification_providers.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Tableau de bord épuré : grand titre, chiffres clés,
/// donut de répartition, actions douces, activité récente.
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
    final now = DateTime.now();
    const months = [
      'janvier', 'février', 'mars', 'avril', 'mai', 'juin',
      'juillet', 'août', 'septembre', 'octobre', 'novembre', 'décembre'
    ];

    return Scaffold(
      backgroundColor: Ds.pageBg,
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(_dashKey),
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
            SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.only(
                  top: MediaQuery.of(context).padding.top + 8,
                  left: 20,
                  right: 20,
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${now.day} ${months[now.month - 1]} ${now.year}',
                              style: Tx.small(size: 12.5)),
                          const SizedBox(height: 2),
                          Text('Bonjour, $username', style: Tx.h1(size: 24)),
                        ],
                      ),
                    ),
                    InkWell(
                      borderRadius: BorderRadius.circular(16),
                      onTap: () => context.push('/notifications'),
                      child: Container(
                        padding: const EdgeInsets.all(11),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(16),
                          boxShadow: const [
                            BoxShadow(
                                color: Color.fromRGBO(16, 24, 40, 0.06),
                                blurRadius: 12,
                                offset: Offset(0, 3)),
                          ],
                        ),
                        child: Badge(
                          isLabelVisible: unread > 0,
                          label: Text('$unread'),
                          backgroundColor: Ds.dangerText,
                          child: const Icon(Icons.notifications_outlined,
                              color: Ds.deepText),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 14, 16, 110),
              sliver: SliverList(
                delegate: SliverChildListDelegate([
                  _OverviewCard(isSupplier: isSupplier),
                  const SizedBox(height: 18),
                  const SectionHeader('Actions rapides'),
                  const SizedBox(height: 10),
                  GridView.count(
                    crossAxisCount: 4,
                    shrinkWrap: true,
                    physics: const NeverScrollableScrollPhysics(),
                    mainAxisSpacing: 10,
                    crossAxisSpacing: 10,
                    childAspectRatio: 0.88,
                    children: [
                      if (!isSupplier) ...[
                        QuickTile(
                            icon: Icons.add_shopping_cart_rounded,
                            label: 'Commander',
                            tint: Ds.accent,
                            onTap: () => context.push('/shop/orders/create')),
                        QuickTile(
                            icon: Icons.payments_rounded,
                            label: 'Payer',
                            tint: Ds.teal,
                            onTap: () => context.push('/payments/create')),
                      ],
                      if (isSupplier && isAdmin) ...[
                        QuickTile(
                            icon: Icons.add_box_rounded,
                            label: 'Commander',
                            tint: Ds.accent,
                            onTap: () => context.push('/supplier/orders/create')),
                        QuickTile(
                            icon: Icons.inventory_2_rounded,
                            label: 'Produit',
                            tint: Ds.teal,
                            onTap: () => context.push('/supplier/stock/create')),
                      ],
                      QuickTile(
                          icon: Icons.qr_code_scanner_rounded,
                          label: 'Scanner',
                          tint: const Color(0xFF7C3AED),
                          onTap: () => context.push('/scan')),
                      QuickTile(
                          icon: Icons.file_download_rounded,
                          label: 'Export',
                          tint: Ds.infoText,
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

/// Carte chiffres clés + donut de répartition.
class _OverviewCard extends ConsumerWidget {
  final bool isSupplier;
  const _OverviewCard({required this.isSupplier});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(_dashKey);
    return DsCard(
      child: FutureBuilder(
        future: isSupplier ? _supplierStats(ref) : _shopStats(ref),
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const Padding(
              padding: EdgeInsets.symmetric(vertical: 40),
              child: Center(child: CircularProgressIndicator(color: Ds.accent)),
            );
          }
          if (snap.hasError) {
            return InkWell(
              onTap: () => ref.invalidate(_dashKey),
              child: const Padding(
                padding: EdgeInsets.all(12),
                child: Text('Toucher pour recharger',
                    textAlign: TextAlign.center, style: TextStyle(color: Ds.muted)),
              ),
            );
          }
          final d = snap.data!;
          final slices = d['slices'] as List<DonutSlice>;
          return Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: _Headline(
                        label: d['l1'] as String,
                        value: d['v1'] as String,
                        route: d['r1'] as String),
                  ),
                  Container(width: 1, height: 44, color: const Color(0xFFEAECF0)),
                  Expanded(
                    child: _Headline(
                        label: d['l2'] as String,
                        value: d['v2'] as String,
                        route: d['r2'] as String),
                  ),
                  Container(width: 1, height: 44, color: const Color(0xFFEAECF0)),
                  Expanded(
                    child: _Headline(
                        label: d['l3'] as String,
                        value: d['v3'] as String,
                        route: d['r3'] as String),
                  ),
                ],
              ),
              const Divider(height: 28),
              DonutChart(
                slices: slices,
                centerValue: d['total'] as String,
                centerLabel: d['totalLabel'] as String,
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
        'l1': 'En attente', 'v1': '0', 'r1': '/payments',
        'l2': 'Commandes', 'v2': '0', 'r2': '/supplier/orders',
        'l3': 'Stock bas', 'v3': '0', 'r3': '/supplier/low-stock-alerts',
        'total': '0', 'totalLabel': 'commandes',
        'slices': const [DonutSlice('Vide', 1, Color(0xFFEAECF0))],
      };
    }
    final results = await Future.wait([
      api.supplierSummary(),
      api.orders(page: 0, size: 1),
      api.lowStockAlerts(),
    ]);
    final summary = asMap(results[0]);
    final pending = ((summary['pendingCount'] as num?) ?? 0).toDouble();
    final confirmed = ((summary['confirmedCount'] as num?) ?? 0).toDouble();
    final orders = (results[1] as dynamic).totalElements ?? 0;
    return {
      'l1': 'En attente',
      'v1': '$pending'.replaceAll('.0', ''),
      'r1': '/payments?status=PENDING',
      'l2': 'Commandes',
      'v2': '$orders',
      'r2': '/supplier/orders',
      'l3': 'Stock bas',
      'v3': '${(results[2] as List).length}',
      'r3': '/supplier/low-stock-alerts',
      'total': '$orders',
      'totalLabel': 'commandes',
      'slices': [
        DonutSlice('En attente', pending, Ds.warningText),
        DonutSlice('Confirmés', confirmed, Ds.teal),
      ],
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
      'v1': '$pending'.replaceAll('.0', ''),
      'r1': '/payments?status=PENDING',
      'l2': 'Commandes',
      'v2': '${orders.totalElements}',
      'r2': '/shop/orders',
      'l3': 'Reste dû',
      'v3': fmtAmount(due),
      'r3': '/shop/balance',
      'total': '${orders.totalElements}',
      'totalLabel': 'commandes',
      'slices': [
        DonutSlice('En attente', pending, Ds.warningText),
        DonutSlice('Confirmés', confirmed, Ds.teal),
        DonutSlice('Rejetés', rejected, Ds.dangerText),
        DonutSlice('Annulés', cancelled, Ds.muted),
      ],
    };
  }
}

class _Headline extends StatelessWidget {
  final String label;
  final String value;
  final String route;
  const _Headline({required this.label, required this.value, required this.route});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () => context.push(route),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 6),
        child: Column(
          children: [
            Text(value,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Tx.amount()),
            Text(label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: Tx.caption()),
          ],
        ),
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
          return const SizedBox(
              height: 100,
              child: Center(child: CircularProgressIndicator(color: Ds.accent)));
        }
        if (snap.hasError) {
          return DsCard(
            child: Row(
              children: [
                const Expanded(
                    child: Text('Activité indisponible', style: TextStyle(color: Ds.muted))),
                TextButton(
                    onPressed: () => ref.invalidate(_dashKey), child: const Text('Réessayer')),
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
                            Text('${o['reference'] ?? '—'}', style: Tx.title(size: 14)),
                            Text(
                                '${isSupplier ? (o['shopName'] ?? '') : (o['supplierName'] ?? '')} • ${fmtDate(o['createdAt']?.toString())}',
                                style: Tx.small(size: 12)),
                          ],
                        ),
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(fmtAmount(o['total'], '${o['currency'] ?? ''}'),
                              style: Tx.amount(size: 14)),
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
