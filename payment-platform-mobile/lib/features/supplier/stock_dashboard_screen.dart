import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Dashboard stock (desktop stock-dashboard.component).
class StockDashboardScreen extends ConsumerWidget {
  const StockDashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final api = ref.watch(supplierApiProvider);
    if (api == null) return Scaffold(appBar: AppBar(title: const Text('Stock')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Dashboard stock')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(_k),
        child: FutureBuilder(
          future: _load(ref),
          builder: (ctx, snap) {
            if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
            if (snap.hasError) {
              return ErrorView(message: apiErrorMessage(snap.error!), onRetry: () => ref.invalidate(_k));
            }
            final d = snap.data!;
            return ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              children: [
                Row(
                  children: [
                    Expanded(child: StatCard(label: 'Produits', value: '${d['products']}', icon: Icons.inventory, color: Ds.infoText, onTap: () => context.push('/supplier/products'))),
                    const SizedBox(width: 12),
                    Expanded(child: StatCard(label: 'Stock bas', value: '${d['low']}', icon: Icons.warning_amber, color: Ds.dangerText, onTap: () => context.push('/supplier/low-stock-alerts'))),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(child: StatCard(label: 'Ruptures', value: '${d['oos']}', icon: Icons.remove_shopping_cart, color: Ds.warningText)),
                    const SizedBox(width: 12),
                    Expanded(child: StatCard(label: 'Mouvements (30j)', value: '${d['moves']}', icon: Icons.swap_vert, color: Ds.accent, onTap: () => context.push('/supplier/stock'))),
                  ],
                ),
                const SizedBox(height: 12),
                FilledButton.icon(
                  onPressed: () => context.push('/supplier/optimization'),
                  icon: const Icon(Icons.auto_graph),
                  label: const Text('Optimisation des stocks'),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  Future<Map<String, int>> _load(WidgetRef ref) async {
    ref.watch(_k);
    final api = ref.read(supplierApiProvider)!;
    final results = await Future.wait([api.products(), api.lowStockAlerts(), api.movements()]);
    final products = results[0] as List;
    final low = results[1] as List;
    final moves = results[2] as List;
    final oos = products.where((p) => '${(p as Map)['status']}' == 'OUT_OF_STOCK').length;
    return {'products': products.length, 'low': low.length, 'oos': oos, 'moves': moves.length};
  }
}

final _k = FutureProvider((_) async => true);
