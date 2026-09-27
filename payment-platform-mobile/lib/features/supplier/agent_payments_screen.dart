import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Paiements des agents (desktop agent-payments.component).
class AgentPaymentsScreen extends ConsumerStatefulWidget {
  const AgentPaymentsScreen({super.key});
  @override
  ConsumerState<AgentPaymentsScreen> createState() => _AgentPaymentsScreenState();
}

class _AgentPaymentsScreenState extends ConsumerState<AgentPaymentsScreen> {
  Future<List<Map<String, dynamic>>>? _future;
  DateTime _from = DateTime.now().subtract(const Duration(days: 30));
  DateTime _to = DateTime.now();

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() {
    final api = ref.read(supplierApiProvider);
    if (api == null) return;
    setState(() {
      _future = ref.read(paymentsApiProvider).agentSummary(
            api.supplierId,
            _from.toIso8601String().substring(0, 10),
            _to.toIso8601String().substring(0, 10),
          );
    });
  }

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Paiements agents')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Paiements agents')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: DsCard(
              child: Row(
                children: [
                  Expanded(child: Text('Du ${_from.toIso8601String().substring(0, 10)} au ${_to.toIso8601String().substring(0, 10)}')),
                  IconButton(
                    icon: const Icon(Icons.calendar_month, color: Ds.accent),
                    onPressed: () async {
                      final picked = await showDateRangePicker(
                        context: context,
                        firstDate: DateTime(2020),
                        lastDate: DateTime.now(),
                        initialDateRange: DateTimeRange(start: _from, end: _to),
                      );
                      if (picked != null) {
                        setState(() {
                          _from = picked.start;
                          _to = picked.end;
                        });
                        _load();
                      }
                    },
                  ),
                ],
              ),
            ),
          ),
          Expanded(
            child: FutureBuilder(
              future: _future,
              builder: (ctx, snap) {
                if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
                if (snap.hasError) {
                  return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
                }
                final items = snap.data ?? [];
                if (items.isEmpty) return const EmptyView(message: 'Aucune donnée sur la période');
                return RefreshIndicator(
                  onRefresh: () async => _load(),
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: items.length,
                    itemBuilder: (_, i) {
                      final a = items[i];
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 8),
                        child: DsCard(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text('${a['agentName'] ?? a['username'] ?? '—'}',
                                  style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                              const SizedBox(height: 4),
                              InfoRow('Confirmés', '${a['confirmedCount'] ?? 0} (${fmtAmount(a['confirmedTotal'])})'),
                              InfoRow('En attente', '${a['pendingCount'] ?? 0} (${fmtAmount(a['pendingTotal'])})'),
                            ],
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
