import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Statistiques paiements (desktop payment-stats.component).
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
              if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
              if (snap.hasError) {
                return ErrorView(
                    message: apiErrorMessage(snap.error!),
                    onRetry: () => ref.invalidate(_statsKey));
              }
              final d = snap.data!;
              final stats = asMap(d['stats']);
              final summary = asMap(d['summary']);
              return Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    children: [
                      Expanded(child: StatCard(label: 'Total', value: '${stats['total'] ?? 0}', icon: Icons.payments, color: Ds.accent)),
                      const SizedBox(width: 12),
                      Expanded(child: StatCard(label: 'En attente', value: '${stats['pending'] ?? 0}', icon: Icons.hourglass_empty, color: Ds.warningText, onTap: () => context.push('/payments?status=PENDING'))),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(child: StatCard(label: 'Confirmés', value: '${stats['confirmed'] ?? 0}', icon: Icons.check_circle, color: Ds.successText)),
                      const SizedBox(width: 12),
                      Expanded(child: StatCard(label: 'Rejetés', value: '${stats['rejected'] ?? 0}', icon: Icons.cancel, color: Ds.dangerText)),
                    ],
                  ),
                  if (isSupplier && summary.isNotEmpty) ...[
                    const SizedBox(height: 16),
                    const Text('Synthèse fournisseur',
                        style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                    const SizedBox(height: 8),
                    DsCard(
                      child: Column(
                        children: [
                          InfoRow('En attente (montant)', fmtAmount(summary['pendingTotal'])),
                          InfoRow('En attente (nombre)', '${summary['pendingCount'] ?? 0}'),
                          InfoRow('Confirmé (montant)', fmtAmount(summary['confirmedTotal'])),
                          InfoRow('Confirmé (nombre)', '${summary['confirmedCount'] ?? 0}'),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
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
