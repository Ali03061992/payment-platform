import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Alertes stock bas (desktop low-stock-alerts.component).
class LowStockAlertsScreen extends ConsumerStatefulWidget {
  const LowStockAlertsScreen({super.key});
  @override
  ConsumerState<LowStockAlertsScreen> createState() => _LowStockAlertsScreenState();
}

class _LowStockAlertsScreenState extends ConsumerState<LowStockAlertsScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.lowStockAlerts();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Alertes')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Alertes stock bas')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final items = snap.data ?? [];
          if (items.isEmpty) {
            return const EmptyView(message: 'Aucune alerte, stocks sains', icon: Icons.check_circle_outline);
          }
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              itemBuilder: (_, i) {
                final p = items[i];
                final avail = ((p['quantity'] as num?) ?? 0).toInt() - ((p['reservedQty'] as num?) ?? 0).toInt();
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DsCard(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: const Icon(Icons.warning_amber, color: Ds.dangerText),
                      title: Text('${p['name'] ?? ''}', style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text('Disponible $avail • seuil ${p['minQuantity'] ?? 0}'),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}
