import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Finance fournisseur (desktop supplier-financial.component).
class SupplierFinancialScreen extends ConsumerStatefulWidget {
  const SupplierFinancialScreen({super.key});
  @override
  ConsumerState<SupplierFinancialScreen> createState() => _SupplierFinancialScreenState();
}

class _SupplierFinancialScreenState extends ConsumerState<SupplierFinancialScreen> {
  Future<Map<String, dynamic>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.financialReport();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Finance')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Finance')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final d = snap.data ?? {};
          final byStatus = (d['byStatus'] as List?) ?? [];
          final top = (d['topShops'] as List?) ?? (d['topCustomers'] as List?) ?? [];
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              children: [
                Row(
                  children: [
                    Expanded(child: StatCard(label: 'Chiffre encaissé', value: fmtAmount(d['totalRevenue'] ?? d['confirmedTotal']), icon: Icons.payments, color: Ds.successText)),
                    const SizedBox(width: 12),
                    Expanded(child: StatCard(label: 'En attente', value: fmtAmount(d['pendingTotal']), icon: Icons.hourglass_empty, color: Ds.warningText)),
                  ],
                ),
                const SizedBox(height: 12),
                DsCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Montants par statut',
                          style: TextStyle(
                              fontSize: 15, fontWeight: FontWeight.w700, color: Ds.deepText)),
                      const SizedBox(height: 10),
                      DsBarChart([
                        for (final s in byStatus)
                          DonutSlice(
                            '${(s as Map)['status'] ?? ''}'.replaceAll('_', '\n'),
                            (((s as Map)['total'] as num?) ?? 0).toDouble(),
                            StatusBadge.colorsFor('${(s as Map)['status'] ?? ''}').$2,
                          ),
                      ]),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text('Détail par statut',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                const SizedBox(height: 8),
                if (byStatus.isEmpty)
                  const DsCard(child: Text('Aucune donnée', style: TextStyle(color: Ds.muted)))
                else
                  for (final s in byStatus)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: DsCard(
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            StatusBadge('${(s as Map)['status'] ?? ''}'),
                            Text(fmtAmount((s as Map)['total']),
                                style: const TextStyle(fontWeight: FontWeight.bold)),
                          ],
                        ),
                      ),
                    ),
                const SizedBox(height: 12),
                const Text('Top boutiques',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                const SizedBox(height: 8),
                if (top.isEmpty)
                  const DsCard(child: Text('Aucune donnée', style: TextStyle(color: Ds.muted)))
                else
                  for (final t in top)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: DsCard(
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text('${(t as Map)['shopName'] ?? (t as Map)['name'] ?? ''}',
                                  style: const TextStyle(fontWeight: FontWeight.w500)),
                            ),
                            Text(fmtAmount((t as Map)['total'] ?? (t as Map)['revenue']),
                                style: const TextStyle(fontWeight: FontWeight.bold)),
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
