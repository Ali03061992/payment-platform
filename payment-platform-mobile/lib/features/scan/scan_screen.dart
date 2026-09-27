import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../../core/api/providers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Scanner QR (desktop qr-scanner.component) : une référence scannée
/// ouvre le paiement ou la commande correspondante.
class ScanScreen extends ConsumerStatefulWidget {
  const ScanScreen({super.key});
  @override
  ConsumerState<ScanScreen> createState() => _ScanScreenState();
}

class _ScanScreenState extends ConsumerState<ScanScreen> {
  final _ctrl = MobileScannerController();
  final _manual = TextEditingController();
  bool _busy = false;

  @override
  void dispose() {
    _ctrl.dispose();
    _manual.dispose();
    super.dispose();
  }

  Future<void> _resolve(String code) async {
    if (_busy || code.isEmpty) return;
    setState(() => _busy = true);
    try {
      try {
        final p = await ref.read(paymentsApiProvider).getByReference(code);
        if (mounted) {
          context.push('/payments/${p['id']}');
          return;
        }
      } catch (_) {}
      final o = await ref.read(shopApiProvider).orderByReference(code);
      if (mounted) {
        final roles = ref.read(authProvider).roles;
        context.push(roles.any((r) => r.startsWith('SUPPLIER'))
            ? '/supplier/orders/${o['id']}'
            : '/shop/orders/${o['id']}');
      }
    } catch (e) {
      if (mounted) showSnack(context, 'Référence introuvable : $code', error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Scanner QR')),
      body: Column(
        children: [
          Expanded(
            flex: 3,
            child: MobileScanner(
              controller: _ctrl,
              onDetect: (capture) {
                final code = capture.barcodes.isNotEmpty
                    ? (capture.barcodes.first.rawValue ?? '')
                    : '';
                _resolve(code);
              },
            ),
          ),
          Expanded(
            flex: 2,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: DsCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text('Saisie manuelle',
                        style: TextStyle(fontWeight: FontWeight.bold, color: Ds.ink)),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: TextField(
                              controller: _manual, decoration: dsInput('Référence (PAY-…, CMD-…)')),
                        ),
                        const SizedBox(width: 8),
                        FilledButton(
                          onPressed: _busy ? null : () => _resolve(_manual.text.trim()),
                          child: _busy ? const Text('...') : const Text('Ouvrir'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
