import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Ajout de produit (desktop add-product.component).
class AddProductScreen extends ConsumerStatefulWidget {
  const AddProductScreen({super.key});
  @override
  ConsumerState<AddProductScreen> createState() => _AddProductScreenState();
}

class _AddProductScreenState extends ConsumerState<AddProductScreen> {
  final _name = TextEditingController();
  final _sku = TextEditingController();
  final _desc = TextEditingController();
  final _price = TextEditingController();
  final _qty = TextEditingController();
  final _min = TextEditingController(text: '5');
  String _currency = 'TND';
  String? _categoryId;
  String? _familyId;
  List<Map<String, dynamic>> _cats = [];
  List<Map<String, dynamic>> _fams = [];
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _loadRefs();
  }

  @override
  void dispose() {
    _name.dispose();
    _sku.dispose();
    _desc.dispose();
    _price.dispose();
    _qty.dispose();
    _min.dispose();
    super.dispose();
  }

  Future<void> _loadRefs() async {
    try {
      final api = ref.read(supplierApiProvider);
      if (api == null) return;
      final results = await Future.wait([api.categories(), api.families()]);
      if (mounted) {
        setState(() {
          _cats = results[0] as List<Map<String, dynamic>>;
          _fams = results[1] as List<Map<String, dynamic>>;
        });
      }
    } catch (_) {}
  }

  Future<void> _submit() async {
    if (_name.text.trim().isEmpty || _price.text.isEmpty || _qty.text.isEmpty) {
      showSnack(context, 'Nom, prix et quantité sont obligatoires', error: true);
      return;
    }
    setState(() => _sending = true);
    try {
      await ref.read(supplierApiProvider)!.createProduct({
        'name': _name.text.trim(),
        'sku': _sku.text.trim().isEmpty ? 'SKU-${DateTime.now().millisecondsSinceEpoch}' : _sku.text.trim(),
        'description': _desc.text.trim(),
        'unitPrice': double.parse(_price.text.replaceAll(',', '.')),
        'currency': _currency,
        'quantity': int.parse(_qty.text),
        'minQuantity': int.tryParse(_min.text) ?? 5,
        if (_categoryId != null) 'categoryId': _categoryId,
        if (_familyId != null) 'familyId': _familyId,
      });
      if (mounted) {
        showSnack(context, 'Produit créé');
        context.pop();
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
      appBar: AppBar(title: const Text('Ajouter un produit')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: DsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(controller: _name, decoration: dsInput('Nom *')),
              const SizedBox(height: 12),
              TextField(controller: _sku, decoration: dsInput('SKU (auto si vide)')),
              const SizedBox(height: 12),
              TextField(controller: _desc, decoration: dsInput('Description'), maxLines: 2),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                        controller: _price,
                        decoration: dsInput('Prix *'),
                        keyboardType: const TextInputType.numberWithOptions(decimal: true)),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: DropdownButtonFormField<String>(
                      initialValue: _currency,
                      decoration: dsInput('Devise'),
                      items: const [
                        DropdownMenuItem(value: 'TND', child: Text('TND')),
                        DropdownMenuItem(value: 'EUR', child: Text('EUR')),
                        DropdownMenuItem(value: 'USD', child: Text('USD')),
                      ],
                      onChanged: (v) => setState(() => _currency = v ?? 'TND'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                        controller: _qty,
                        decoration: dsInput('Quantité *'),
                        keyboardType: TextInputType.number),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                        controller: _min,
                        decoration: dsInput('Seuil alerte'),
                        keyboardType: TextInputType.number),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _categoryId,
                decoration: dsInput('Catégorie'),
                items: [
                  const DropdownMenuItem(value: null, child: Text('—')),
                  for (final c in _cats)
                    DropdownMenuItem(value: '${c['id']}', child: Text('${c['name']}')),
                ],
                onChanged: (v) => setState(() => _categoryId = v),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                initialValue: _familyId,
                decoration: dsInput('Famille'),
                items: [
                  const DropdownMenuItem(value: null, child: Text('—')),
                  for (final f in _fams)
                    DropdownMenuItem(value: '${f['id']}', child: Text('${f['name']}')),
                ],
                onChanged: (v) => setState(() => _familyId = v),
              ),
              const SizedBox(height: 16),
              SizedBox(
                height: 50,
                child: FilledButton(
                  onPressed: _sending ? null : _submit,
                  child: Text(_sending ? 'Envoi...' : 'Créer le produit'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
