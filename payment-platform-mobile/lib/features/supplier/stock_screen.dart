import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Mouvements de stock (desktop stock-management.component).
class StockScreen extends ConsumerStatefulWidget {
  const StockScreen({super.key});
  @override
  ConsumerState<StockScreen> createState() => _StockScreenState();
}

class _StockScreenState extends ConsumerState<StockScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.movements();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Stock')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Stock')),
      floatingActionButton: FloatingActionButton(
        backgroundColor: Ds.accent,
        onPressed: () => _createMovement(context),
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final items = snap.data ?? [];
          if (items.isEmpty) return const EmptyView(message: 'Aucun mouvement');
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              itemBuilder: (_, i) {
                final m = items[i];
                final type = '${m['type'] ?? ''}';
                final icon = type == 'IN'
                    ? Icons.arrow_downward
                    : type == 'OUT'
                        ? Icons.arrow_upward
                        : Icons.tune;
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DsCard(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      leading: Icon(icon, color: Ds.accent),
                      title: Text('${m['productName'] ?? ''}',
                          style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text('$type • Qté ${m['quantity']} • ${fmtDate(m['createdAt']?.toString())}'),
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

  Future<void> _createMovement(BuildContext context) async {
    try {
      final products = await ref.read(supplierApiProvider)!.products('ACTIVE');
      if (!context.mounted) return;
      String? productId = products.isNotEmpty ? '${products.first['id']}' : null;
      String type = 'IN';
      final qty = TextEditingController();
      final refCtrl = TextEditingController();
      final notes = TextEditingController();
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('Nouveau mouvement'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: productId,
                  decoration: dsInput('Produit'),
                  items: [
                    for (final p in products)
                      DropdownMenuItem(value: '${p['id']}', child: Text('${p['name']}')),
                  ],
                  onChanged: (v) => productId = v,
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: type,
                  decoration: dsInput('Type'),
                  items: const [
                    DropdownMenuItem(value: 'IN', child: Text('Entrée (IN)')),
                    DropdownMenuItem(value: 'OUT', child: Text('Sortie (OUT)')),
                    DropdownMenuItem(value: 'ADJUSTMENT', child: Text('Ajustement')),
                  ],
                  onChanged: (v) => type = v ?? 'IN',
                ),
                const SizedBox(height: 12),
                TextField(controller: qty, decoration: dsInput('Quantité'), keyboardType: TextInputType.number),
                const SizedBox(height: 12),
                TextField(controller: refCtrl, decoration: dsInput('Référence')),
                const SizedBox(height: 12),
                TextField(controller: notes, decoration: dsInput('Notes')),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Enregistrer')),
          ],
        ),
      );
      final q = int.tryParse(qty.text);
      final reference = refCtrl.text.trim();
      final noteText = notes.text.trim();
      qty.dispose();
      refCtrl.dispose();
      notes.dispose();
      if (ok != true || productId == null || q == null) return;
      await ref.read(supplierApiProvider)!.createMovement(productId!, {
        'type': type,
        'quantity': q,
        'reference': reference,
        'notes': noteText,
      });
      if (context.mounted) showSnack(context, 'Mouvement enregistré');
      _load();
    } catch (e) {
      if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }
}
