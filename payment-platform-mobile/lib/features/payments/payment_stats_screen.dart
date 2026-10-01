import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Statistiques paiements avec donut interactif (desktop payment-stats.component).
class PaymentStatsScreen extends ConsumerWidget {
  const PaymentStatsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final roles = ref.watch(authProvider).roles;
    final isSupplier = roles.any((r) => r.startsWith('SUPPLIER'));
    final orgId = orgIdOf(ref.watch(authProvider).user);
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Statistiques paiements')),
      body: RefreshIndicator(
        onRefresh: () async => ref.invalidate(_statsKey),
        child: SingleChildScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          child: FutureBuilder(
            future: _load(ref, isSupplier, orgId),
            builder: (ctx, snap) {
              if (snap.connectionState == ConnectionState.waiting) {
                return const SizedBox(
                    height: 300, child: Center(child: CircularProgressIndicator(color: Ds.accent)));
              }
              if (snap.hasError) {
                return ErrorView(
                    message: apiErrorMessage(snap.error!),
                    onRetry: () => ref.invalidate(_statsKey));
              }
              final d = snap.data!;
              final stats = asMap(d['stats']);
              final summary = asMap(d['summary']);
              final total = ((stats['total'] as num?) ?? 0).toDouble();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  DsCard(
                    child: DonutChart(
                      centerValue: '${stats['total'] ?? 0}',
                      centerLabel: 'paiements',
                      slices: [
                        DonutSlice('En attente',
                            ((stats['pending'] as num?) ?? 0).toDouble(), Ds.warningText),
                        DonutSlice('Confirmés',
                            ((stats['confirmed'] as num?) ?? 0).toDouble(), Ds.teal),
                        DonutSlice('Rejetés',
                            ((stats['rejected'] as num?) ?? 0).toDouble(), Ds.dangerText),
                        DonutSlice('Annulés',
                            ((stats['cancelled'] as num?) ?? 0).toDouble(), Ds.muted),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  DsCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Montants', style: Tx.title()),
                        const SizedBox(height: 10),
                        DsBarChart([
                          DonutSlice('Att.',
                              ((summary['pendingTotal'] as num?) ?? (total > 0 ? (stats['pending'] as num?) ?? 0 : 0)).toDouble(),
                              Ds.warningText),
                          DonutSlice('Conf.',
                              ((summary['confirmedTotal'] as num?) ?? (total > 0 ? (stats['confirmed'] as num?) ?? 0 : 0)).toDouble(),
                              Ds.teal),
                        ]),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () => context.push('/payments?status=PENDING'),
                    icon: const Icon(Icons.hourglass_empty),
                    label: const Text('Voir les paiements en attente'),
                  ),
                  if (isSupplier) ...[
                    const SizedBox(height: 8),
                    FilledButton.icon(
                      onPressed: () => context.push('/supplier/agent-payments'),
                      icon: const Icon(Icons.group),
                      label: const Text('Paiements par agent'),
                    ),
                  ],
                ],
              );
            },
          ),
        ),
      ),
    );
  }

  Future<Map<String, dynamic>> _load(WidgetRef ref, bool isSupplier, String? orgId) async {
    ref.watch(_statsKey);
    final stats = await ref.read(paymentsApiProvider).stats();
    Map<String, dynamic> summary = {};
    if (isSupplier && orgId != null) {
      try {
        summary = await ref.read(supplierApiProvider)?.supplierSummary() ?? {};
      } catch (_) {}
    }
    return {'stats': stats, 'summary': summary};
  }
}

final _statsKey = FutureProvider((_) async => true);
