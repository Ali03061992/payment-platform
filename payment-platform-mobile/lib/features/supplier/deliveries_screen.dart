import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Livraisons (desktop delivery-management.component).
class DeliveriesScreen extends ConsumerStatefulWidget {
  const DeliveriesScreen({super.key});
  @override
  ConsumerState<DeliveriesScreen> createState() => _DeliveriesScreenState();
}

class _DeliveriesScreenState extends ConsumerState<DeliveriesScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.myDeliveries();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Livraisons')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Livraisons')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final items = snap.data ?? [];
          if (items.isEmpty) return const EmptyView(message: 'Aucune livraison');
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              itemBuilder: (_, i) {
                final o = items[i];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DsCard(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${o['reference'] ?? '—'}',
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text(
                          '${o['shopName'] ?? ''}\nPrévue ${fmtDate(o['plannedDeliveryDate']?.toString())}'),
                      isThreeLine: true,
                      trailing: StatusBadge('${o['status'] ?? ''}'),
                      onTap: () => context.push('/supplier/orders/${o['id']}'),
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
