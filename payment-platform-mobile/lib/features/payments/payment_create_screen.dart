import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Création de paiement (desktop create-payment.component).
/// Clé d'idempotence générée automatiquement (B1).
class PaymentCreateScreen extends ConsumerStatefulWidget {
  const PaymentCreateScreen({super.key});
  @override
  ConsumerState<PaymentCreateScreen> createState() => _PaymentCreateScreenState();
}

class _PaymentCreateScreenState extends ConsumerState<PaymentCreateScreen> {
  final _amount = TextEditingController();
  String _currency = 'TND';
  String? _supplierId;
  List<Map<String, dynamic>> _relations = [];
  bool _loadingRelations = true;
  String? _loadError;
  bool _sending = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _amount.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loadingRelations = true;
      _loadError = null;
    });
    try {
      final orgId = orgIdOf(ref.read(authProvider).user);
      if (orgId == null) throw Exception('Organisation introuvable');
      final rels = await ref.read(shopApiProvider).relationsByShop(orgId);
      setState(() {
        _relations = rels.where((r) => '${r['status'] ?? 'ACTIVE'}' == 'ACTIVE').toList();
        if (_relations.isNotEmpty) {
          _supplierId = _relations.first['supplierId']?.toString();
        }
      });
    } catch (e) {
      setState(() => _loadError = apiErrorMessage(e));
    } finally {
      setState(() => _loadingRelations = false);
    }
  }

  Future<void> _submit() async {
    final amount = double.tryParse(_amount.text.replaceAll(',', '.'));
    if (amount == null || amount <= 0) {
      showSnack(context, 'Montant invalide', error: true);
      return;
    }
    if (_supplierId == null) {
      showSnack(context, 'Choisissez un fournisseur', error: true);
      return;
    }
    setState(() => _sending = true);
    try {
      final orgId = orgIdOf(ref.read(authProvider).user)!;
      final created = await ref.read(paymentsApiProvider).create({
        'shopId': orgId,
        'supplierId': _supplierId,
        'amount': amount,
        'currency': _currency,
      });
      if (mounted) {
        showSnack(context, 'Paiement ${created['reference'] ?? ''} créé');
        context.go('/payments/${created['id']}');
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
      appBar: AppBar(title: const Text('Créer un paiement')),
      body: _loadingRelations
          ? const LoadingView()
          : _loadError != null
              ? ErrorView(message: _loadError!, onRetry: _load)
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: DsCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        DropdownButtonFormField<String>(
                          initialValue: _supplierId,
                          decoration: dsInput('Fournisseur'),
                          items: [
                            for (final r in _relations)
                              DropdownMenuItem(
                                value: r['supplierId']?.toString(),
                                child: Text('${r['supplierName'] ?? r['supplierId']}'),
                              ),
                          ],
                          onChanged: (v) => setState(() => _supplierId = v),
                        ),
                        const SizedBox(height: 12),
                        TextField(
                          controller: _amount,
                          decoration: dsInput('Montant'),
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          initialValue: _currency,
                          decoration: dsInput('Devise'),
                          items: const [
                            DropdownMenuItem(value: 'TND', child: Text('TND')),
                            DropdownMenuItem(value: 'EUR', child: Text('EUR')),
                            DropdownMenuItem(value: 'USD', child: Text('USD')),
                          ],
                          onChanged: (v) => setState(() => _currency = v ?? 'TND'),
                        ),
                        const SizedBox(height: 16),
                        SizedBox(
                          height: 50,
                          child: FilledButton(
                            onPressed: _sending ? null : _submit,
                            child: Text(_sending ? 'Envoi...' : 'Créer le paiement'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
    );
  }
}
