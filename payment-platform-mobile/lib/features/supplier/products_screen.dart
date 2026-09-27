import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Catalogue produits (desktop product-management.component).
class ProductsScreen extends ConsumerStatefulWidget {
  const ProductsScreen({super.key});
  @override
  ConsumerState<ProductsScreen> createState() => _ProductsScreenState();
}

class _ProductsScreenState extends ConsumerState<ProductsScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.products();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Produits')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Produits')),
      floatingActionButton: FloatingActionButton(
        backgroundColor: Ds.accent,
        onPressed: () => context.push('/supplier/stock/create').then((_) => _load()),
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
          if (items.isEmpty) return const EmptyView(message: 'Aucun produit');
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
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text('${p['name'] ?? ''}',
                                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                            ),
                            StatusBadge('${p['status'] ?? ''}'),
                          ],
                        ),
                        Text('SKU ${p['sku'] ?? '—'} • ${fmtAmount(p['unitPrice'], '${p['currency'] ?? ''}')}',
                            style: const TextStyle(color: Ds.muted, fontSize: 12)),
                        Text('Stock $avail (seuil ${p['minQuantity'] ?? 0})',
                            style: TextStyle(
                                color: avail <= ((p['minQuantity'] as num?) ?? 0)
                                    ? Ds.dangerText
                                    : Ds.textSecondary,
                                fontSize: 13)),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.end,
                          children: [
                            TextButton.icon(
                              onPressed: () => _edit(context, p),
                              icon: const Icon(Icons.edit, size: 18),
                              label: const Text('Modifier'),
                            ),
                            TextButton.icon(
                              onPressed: () => _deactivate(context, '${p['id']}', '${p['name']}'),
                              icon: const Icon(Icons.block, size: 18, color: Ds.dangerText),
                              label: const Text('Désactiver', style: TextStyle(color: Ds.dangerText)),
                            ),
                          ],
                        ),
                      ],
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

  Future<void> _edit(BuildContext context, Map<String, dynamic> p) async {
    final price = TextEditingController(text: '${p['unitPrice'] ?? ''}');
    final qty = TextEditingController(text: '${p['quantity'] ?? ''}');
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Modifier ${p['name']}'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: price, decoration: dsInput('Prix unitaire'), keyboardType: TextInputType.number),
            const SizedBox(height: 12),
            TextField(controller: qty, decoration: dsInput('Quantité'), keyboardType: TextInputType.number),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Enregistrer')),
        ],
      ),
    );
    final pv = double.tryParse(price.text.replaceAll(',', '.'));
    final qv = int.tryParse(qty.text);
    price.dispose();
    qty.dispose();
    if (ok != true) return;
    try {
      await ref.read(supplierApiProvider)!.updateProduct('${p['id']}', {
        if (pv != null) 'unitPrice': pv,
        if (qv != null) 'quantity': qv,
      });
      if (context.mounted) showSnack(context, 'Produit mis à jour');
      _load();
    } catch (e) {
      if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  Future<void> _deactivate(BuildContext context, String id, String name) async {
    if (!await confirmDialog(context, title: 'Désactiver', message: 'Désactiver « $name » ?', ok: 'Désactiver')) {
      return;
    }
    try {
      await ref.read(supplierApiProvider)!.deactivateProduct(id);
      if (context.mounted) showSnack(context, 'Produit désactivé');
      _load();
    } catch (e) {
      if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }
}
