import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Optimisation des stocks (desktop stock-optimization.component).
class OptimizationScreen extends ConsumerStatefulWidget {
  const OptimizationScreen({super.key});
  @override
  ConsumerState<OptimizationScreen> createState() => _OptimizationScreenState();
}

class _OptimizationScreenState extends ConsumerState<OptimizationScreen> {
  Future<Map<String, dynamic>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.optimization();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Optimisation')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Optimisation')),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: Ds.accent,
        onPressed: _configure,
        icon: const Icon(Icons.tune, color: Colors.white),
        label: const Text('Paramètres', style: TextStyle(color: Colors.white)),
      ),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final d = snap.data ?? {};
          final recs = (d['recommendations'] as List?) ?? (d['items'] as List?) ?? [];
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                DsCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Synthèse',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                      const SizedBox(height: 8),
                      for (final k in ['totalProducts', 'criticalCount', 'overstockCount', 'lastRun'])
                        if (d[k] != null) InfoRow(_label(k), '${d[k]}'),
                      if (d.isEmpty) const Text('Aucune donnée', style: TextStyle(color: Ds.muted)),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text('Recommandations',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                const SizedBox(height: 8),
                if (recs.isEmpty)
                  const DsCard(child: Text('Aucune recommandation', style: TextStyle(color: Ds.muted)))
                else
                  for (final r in recs)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: DsCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${(r as Map)['productName'] ?? (r as Map)['productId'] ?? ''}',
                                style: const TextStyle(fontWeight: FontWeight.w600)),
                            Text(
                                'Action ${(r as Map)['action'] ?? '—'} • Qté ${(r as Map)['quantity'] ?? (r as Map)['recommendedQuantity'] ?? '—'}',
                                style: const TextStyle(color: Ds.muted, fontSize: 13)),
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

  String _label(String k) {
    switch (k) {
      case 'totalProducts':
        return 'Produits analysés';
      case 'criticalCount':
        return 'Critiques';
      case 'overstockCount':
        return 'Surstocks';
      case 'lastRun':
        return 'Dernière analyse';
      default:
        return k;
    }
  }

  Future<void> _configure() async {
    final lead = TextEditingController(text: '7');
    final order = TextEditingController(text: '50');
    final hold = TextEditingController(text: '20');
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text("Paramètres d'optimisation"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: lead, decoration: dsInput('Délai (jours)'), keyboardType: TextInputType.number),
            const SizedBox(height: 12),
            TextField(controller: order, decoration: dsInput('Coût de commande'), keyboardType: TextInputType.number),
            const SizedBox(height: 12),
            TextField(controller: hold, decoration: dsInput('Coût de possession (%)'), keyboardType: TextInputType.number),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Lancer')),
        ],
      ),
    );
    final l = double.tryParse(lead.text.replaceAll(',', '.'));
    final o = double.tryParse(order.text.replaceAll(',', '.'));
    final h = double.tryParse(hold.text.replaceAll(',', '.'));
    lead.dispose();
    order.dispose();
    hold.dispose();
    if (ok != true || l == null || o == null || h == null) return;
    try {
      await ref.read(supplierApiProvider)!.configureOptimization(l, o, h);
      if (mounted) showSnack(context, 'Optimisation relancée');
      _load();
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }
}
