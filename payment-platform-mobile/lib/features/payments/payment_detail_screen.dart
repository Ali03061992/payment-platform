import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Détail + historique + actions (desktop payment-detail.component).
class PaymentDetailScreen extends ConsumerStatefulWidget {
  final String id;
  const PaymentDetailScreen({super.key, required this.id});

  @override
  ConsumerState<PaymentDetailScreen> createState() => _PaymentDetailScreenState();
}

class _PaymentDetailScreenState extends ConsumerState<PaymentDetailScreen> {
  Future<Map<String, dynamic>>? _future;

  @override
  void initState() {
    super.initState();
    _future = ref.read(paymentsApiProvider).getById(widget.id);
  }

  void _reload() => setState(() {
        _future = ref.read(paymentsApiProvider).getById(widget.id);
      });

  Future<void> _run(Future<Map<String, dynamic>> Function() call, String ok) async {
    try {
      await call();
      if (mounted) showSnack(context, ok);
      _reload();
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  @override
  Widget build(BuildContext context) {
    final roles = ref.watch(authProvider).roles;
    final isSupplier = roles.any((r) => r.startsWith('SUPPLIER'));
    final isShop = roles.any((r) => r.startsWith('SHOP'));
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Détail paiement')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) {
            return const LoadingView();
          }
          if (snap.hasError || !snap.hasData) {
            return ErrorView(
                message: apiErrorMessage(snap.error ?? 'Erreur'), onRetry: _reload);
          }
          final p = snap.data!;
          final status = '${p['status'] ?? ''}';
          final events = (p['events'] as List?) ?? [];          return RefreshIndicator(
            onRefresh: () async => _reload(),
            child: SingleChildScrollView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      gradient: Ds.headerGradient,
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: [
                        BoxShadow(
                            color: Ds.accent.withValues(alpha: 0.35),
                            blurRadius: 22,
                            offset: const Offset(0, 10)),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text('${p['reference'] ?? '—'}',
                                  style: const TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.w600,
                                      color: Colors.white70)),
                            ),
                            StatusBadge(status),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(fmtAmount(p['amount'], '${p['currency'] ?? ''}'),
                            style: const TextStyle(
                                fontSize: 34,
                                fontWeight: FontWeight.w800,
                                color: Colors.white)),
                        Text('${p['shopName'] ?? ''} → ${p['supplierName'] ?? ''}',
                            style: const TextStyle(color: Colors.white70, fontSize: 13)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  DsCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        InfoRow('Créé par', '${p['createdByName'] ?? '—'}'),
                        InfoRow('Créé le', fmtDate(p['createdAt']?.toString())),
                        if ('${p['rejectionReason'] ?? ''}'.isNotEmpty)
                          InfoRow('Motif rejet', '${p['rejectionReason']}'),
                      ],
                    ),
                  ),
                  if (status == 'PENDING') ...[
                    const SizedBox(height: 12),
                    if (isSupplier)
                      Row(
                        children: [
                          Expanded(
                            child: FilledButton.icon(
                              onPressed: () async {
                                if (await confirmDialog(context,
                                    title: 'Confirmer le paiement',
                                    message: 'Confirmer ce paiement ?')) {
                                  await _run(
                                      () => ref.read(paymentsApiProvider).confirm(widget.id),
                                      'Paiement confirmé');
                                }
                              },
                              icon: const Icon(Icons.check),
                              label: const Text('Confirmer'),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: () async {
                                final reason = await promptDialog(context,
                                    title: 'Rejeter le paiement', label: 'Motif (obligatoire)');
                                if (reason != null) {
                                  await _run(
                                      () => ref.read(paymentsApiProvider).reject(widget.id, reason),
                                      'Paiement rejeté');
                                }
                              },
                              icon: const Icon(Icons.close),
                              label: const Text('Rejeter'),
                            ),
                          ),
                        ],
                      ),
                    if (isShop)
                      FilledButton.icon(
                        onPressed: () async {
                          if (await confirmDialog(context,
                              title: 'Annuler le paiement', message: 'Annuler ce paiement ?', ok: 'Annuler')) {
                            await _run(() => ref.read(paymentsApiProvider).cancel(widget.id),
                                'Paiement annulé');
                          }
                        },
                        icon: const Icon(Icons.cancel_outlined),
                        label: const Text('Annuler le paiement'),
                      ),
                  ],
                  const SizedBox(height: 12),
                  const SectionHeader('Historique'),
                  const SizedBox(height: 8),
                  Timeline([
                    for (final e in events)
                      TimelineEntry(
                        '${(e as Map)['action'] ?? ''}',
                        '${(e as Map)['userName'] ?? ''} • ${fmtDate((e as Map)['timestamp']?.toString())}',
                        '${(e as Map)['details'] ?? ''}',
                      ),
                  ]),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
