import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Balance fournisseur (desktop supplier-balance.component).
class SupplierBalanceScreen extends ConsumerStatefulWidget {
  const SupplierBalanceScreen({super.key});
  @override
  ConsumerState<SupplierBalanceScreen> createState() => _SupplierBalanceScreenState();
}

class _SupplierBalanceScreenState extends ConsumerState<SupplierBalanceScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.supplierBalance();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Balance')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Balance')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final items = snap.data ?? [];
          if (items.isEmpty) return const EmptyView(message: 'Aucune balance');
          num totalDue = 0;
          for (final b in items) {
            totalDue += (b['remainingDue'] as num?) ?? 0;
          }
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              children: [
                StatCard(
                    label: 'Total à recevoir',
                    value: fmtAmount(totalDue),
                    icon: Icons.account_balance_wallet,
                    color: Ds.successText),
                const SizedBox(height: 12),
                for (final b in items)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: DsCard(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('${b['shopName'] ?? '—'}',
                              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                          const SizedBox(height: 4),
                          InfoRow('Commandes', '${b['totalOrders'] ?? 0}'),
                          InfoRow('Paiements', fmtAmount(b['totalPayments'])),
                          InfoRow('Reste dû', fmtAmount(b['remainingDue'])),
                          InfoRow('Dernière transaction', fmtDate(b['lastTransaction']?.toString())),
                        ],
                      ),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }
}
