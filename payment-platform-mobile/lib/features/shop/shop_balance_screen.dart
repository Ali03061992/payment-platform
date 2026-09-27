import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Balance boutique (desktop balance-view.component).
class ShopBalanceScreen extends ConsumerWidget {
  const ShopBalanceScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final orgId = orgIdOf(ref.watch(authProvider).user);
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Ma balance')),
      body: orgId == null
          ? const EmptyView(message: 'Organisation introuvable')
          : RefreshIndicator(
              onRefresh: () async => ref.invalidate(_balanceKey),
              child: FutureBuilder(
                future: _load(ref, orgId),
                builder: (ctx, snap) {
                  if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
                  if (snap.hasError) {
                    return ErrorView(
                        message: apiErrorMessage(snap.error!),
                        onRetry: () => ref.invalidate(_balanceKey));
                  }
                  final items = snap.data ?? [];
                  if (items.isEmpty) {
                    return const EmptyView(message: 'Aucune balance');
                  }
                  num totalDue = 0;
                  for (final b in items) {
                    totalDue += (b['remainingDue'] as num?) ?? 0;
                  }
                  return ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(16),
                    children: [
                      StatCard(
                          label: 'Reste dû total',
                          value: fmtAmount(totalDue),
                          icon: Icons.account_balance_wallet,
                          color: Ds.dangerText),
                      const SizedBox(height: 12),
                      for (final b in items)
                        Padding(
                          padding: const EdgeInsets.only(bottom: 8),
                          child: DsCard(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text('${b['supplierName'] ?? '—'}',
                                    style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                                const SizedBox(height: 4),
                                InfoRow('Commandes', '${b['totalOrders'] ?? 0}'),
                                InfoRow('Paiements', fmtAmount(b['totalPayments'])),
                                InfoRow('Reste dû', fmtAmount(b['remainingDue'])),
                                InfoRow('Dernière transaction',
                                    fmtDate(b['lastTransaction']?.toString())),
                              ],
                            ),
                          ),
                        ),
                    ],
                  );
                },
              ),
            ),
    );
  }

  Future<List<Map<String, dynamic>>> _load(WidgetRef ref, String orgId) async {
    ref.watch(_balanceKey);
    return ref.read(shopApiProvider).shopBalance(orgId);
  }
}

final _balanceKey = FutureProvider((_) async => true);
