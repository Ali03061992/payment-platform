import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Création de commande (desktop create-order + supplier/create-order).
/// [supplierView] : le fournisseur choisit la boutique destinataire.
class OrderCreateScreen extends ConsumerStatefulWidget {
  final bool supplierView;
  const OrderCreateScreen({super.key, this.supplierView = false});

  @override
  ConsumerState<OrderCreateScreen> createState() => _OrderCreateScreenState();
}

class _OrderCreateScreenState extends ConsumerState<OrderCreateScreen> {
  bool _loading = true;
  String? _error;
  List<Map<String, dynamic>> _partners = [];
  String? _partnerId;
  List<Map<String, dynamic>> _products = [];
  final Map<String, int> _qty = {};
  bool _asap = false;
  final _notes = TextEditingController();
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _loadPartners();
  }

  @override
  void dispose() {
    _notes.dispose();
    super.dispose();
  }

  Future<void> _loadPartners() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final orgId = orgIdOf(ref.read(authProvider).user);
      if (orgId == null) throw Exception('Organisation introuvable');
      if (widget.supplierView) {
        _partners = await ref.read(supplierApiProvider)!.relationsBySupplier();
      } else {
        _partners = await ref.read(shopApiProvider).relationsByShop(orgId);
      }
      _partners = _partners.where((r) => '${r['status'] ?? 'ACTIVE'}' == 'ACTIVE').toList();
      if (_partners.isNotEmpty) {
        _partnerId = widget.supplierView
            ? _partners.first['shopId']?.toString()
            : _partners.first['supplierId']?.toString();
        await _loadProducts();
      }
    } catch (e) {
      setState(() => _error = apiErrorMessage(e));
    } finally {
      setState(() => _loading = false);
    }
  }

  Future<void> _loadProducts() async {
    if (_partnerId == null) return;
    try {
      final supplierId = widget.supplierView
          ? orgIdOf(ref.read(authProvider).user)!
          : _partnerId!;
      final list = widget.supplierView
          ? await ref.read(supplierApiProvider)!.products('ACTIVE')
          : await ref.read(shopApiProvider).productsOf(supplierId);
      setState(() {
        _products = list;
        _qty.clear();
      });
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  double get _total {
    double t = 0;
    for (final p in _products) {
      final q = _qty['${p['id']}'] ?? 0;
      t += ((p['unitPrice'] as num?) ?? 0).toDouble() * q;
    }
    return t;
  }

  Future<void> _submit() async {
    final items = [
      for (final p in _products)
        if ((_qty['${p['id']}'] ?? 0) > 0)
          {'productId': '${p['id']}', 'quantity': _qty['${p['id']}'], 'discount': 0},
    ];
    if (items.isEmpty) {
      showSnack(context, 'Ajoutez au moins un article', error: true);
      return;
    }
    setState(() => _sending = true);
    try {
      final orgId = orgIdOf(ref.read(authProvider).user)!;
      final data = {
        'supplierId': widget.supplierView ? orgId : _partnerId,
        'shopId': widget.supplierView ? _partnerId : orgId,
        'asapPayment': _asap,
        'paymentTerms': 'NET_30',
        'currency': 'TND',
        'globalDiscount': 0,
        'notes': _notes.text.trim(),
        'items': items,
      };
      final created = widget.supplierView
          ? await ref.read(supplierApiProvider)!.createOrder(data)
          : await ref.read(shopApiProvider).createOrder(data);
      if (mounted) {
        showSnack(context, 'Commande ${created['reference'] ?? ''} créée');
        context.go(widget.supplierView ? '/supplier/orders' : '/shop/orders/${created['id']}');
      }
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Créer une commande')),
      body: _loading
          ? const LoadingView()
          : _error != null
              ? ErrorView(message: _error!, onRetry: _loadPartners)
              : Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                      child: DsCard(
                        child: Column(
                          children: [
                            DropdownButtonFormField<String>(
                              initialValue: _partnerId,
                              decoration:
                                  dsInput(widget.supplierView ? 'Boutique' : 'Fournisseur'),
                              items: [
                                for (final r in _partners)
                                  DropdownMenuItem(
                                    value: widget.supplierView
                                        ? r['shopId']?.toString()
                                        : r['supplierId']?.toString(),
                                    child: Text(widget.supplierView
                                        ? '${r['shopName'] ?? r['shopId']}'
                                        : '${r['supplierName'] ?? r['supplierId']}'),
                                  ),
                              ],
                              onChanged: (v) async {
                                setState(() => _partnerId = v);
                                await _loadProducts();
                              },
                            ),
                            SwitchListTile(
                              contentPadding: EdgeInsets.zero,
                              title: const Text('Paiement ASAP'),
                              subtitle: const Text('Paiement automatique à la réception'),
                              value: _asap,
                              activeThumbColor: Ds.accent,
                              onChanged: (v) => setState(() => _asap = v),
                            ),
                            TextField(controller: _notes, decoration: dsInput('Notes')),
                          ],
                        ),
                      ),
                    ),
                    Expanded(
                      child: _products.isEmpty
                          ? const EmptyView(message: 'Aucun produit disponible')
                          : ListView.builder(
                              padding: const EdgeInsets.all(16),
                              itemCount: _products.length,
                              itemBuilder: (_, i) {
                                final p = _products[i];
                                final id = '${p['id']}';
                                final q = _qty[id] ?? 0;
                                final avail =
                                    ((p['quantity'] as num?) ?? 0).toInt() - ((p['reservedQty'] as num?) ?? 0).toInt();
                                return Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: DsCard(
                                    child: Row(
                                      children: [
                                        Expanded(
                                          child: Column(
                                            crossAxisAlignment: CrossAxisAlignment.start,
                                            children: [
                                              Text('${p['name'] ?? ''}',
                                                  style: const TextStyle(fontWeight: FontWeight.w600)),
                                              Text(
                                                  '${fmtAmount(p['unitPrice'], '${p['currency'] ?? ''}')} • dispo $avail',
                                                  style: const TextStyle(color: Ds.muted, fontSize: 12)),
                                            ],
                                          ),
                                        ),
                                        IconButton(
                                          icon: const Icon(Icons.remove_circle_outline, color: Ds.accent),
                                          onPressed: q > 0 ? () => setState(() => _qty[id] = q - 1) : null,
                                        ),
                                        Text('$q', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                                        IconButton(
                                          icon: const Icon(Icons.add_circle, color: Ds.accent),
                                          onPressed: () => setState(() => _qty[id] = q + 1),
                                        ),
                                      ],
                                    ),
                                  ),
                                );
                              },
                            ),
                    ),
                    Container(
                      padding: const EdgeInsets.all(16),
                      color: Colors.white,
                      child: Row(
                        children: [
                          Expanded(
                            child: Text('Total : ${fmtAmount(_total, 'TND')}',
                                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                          ),
                          FilledButton(
                            onPressed: _sending ? null : _submit,
                            child: Text(_sending ? 'Envoi...' : 'Commander'),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
    );
  }
}
