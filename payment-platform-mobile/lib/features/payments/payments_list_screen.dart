import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Liste des paiements (desktop payment-list.component).
class PaymentsListScreen extends ConsumerStatefulWidget {
  final String? initialStatus;
  const PaymentsListScreen({super.key, this.initialStatus});

  @override
  ConsumerState<PaymentsListScreen> createState() => _PaymentsListScreenState();
}

class _PaymentsListScreenState extends ConsumerState<PaymentsListScreen> {
  int _page = 0;
  String? _status;

  @override
  void initState() {
    super.initState();
    _status = widget.initialStatus;
  }

  @override
  void didUpdateWidget(PaymentsListScreen old) {
    super.didUpdateWidget(old);
    if (old.initialStatus != widget.initialStatus) {
      setState(() {
        _status = widget.initialStatus;
        _page = 0;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final canCreate = ref.watch(authProvider).roles.any((r) => r == 'SHOP_ADMIN' || r == 'SHOP_AGENT');
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Paiements')),
      floatingActionButton: canCreate
          ? FloatingActionButton(
              backgroundColor: Ds.accent,
              onPressed: () => context.push('/payments/create'),
              child: const Icon(Icons.add, color: Colors.white),
            )
          : null,
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: DropdownButtonFormField<String?>(
              key: ValueKey(_status),
              initialValue: _status,
              decoration: dsInput('Statut'),
              items: const [
                DropdownMenuItem(value: null, child: Text('Tous')),
                DropdownMenuItem(value: 'PENDING', child: Text('En attente')),
                DropdownMenuItem(value: 'CONFIRMED', child: Text('Confirmés')),
                DropdownMenuItem(value: 'REJECTED', child: Text('Rejetés')),
                DropdownMenuItem(value: 'CANCELLED', child: Text('Annulés')),
              ],
              onChanged: (v) => setState(() {
                _status = v;
                _page = 0;
              }),
            ),
          ),
          Expanded(
            child: FutureBuilder(
              future:
                  ref.read(paymentsApiProvider).list(page: _page, size: 20, status: _status),
              builder: (ctx, snap) {
                if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
                if (snap.hasError) {
                  return ErrorView(
                      message: apiErrorMessage(snap.error!), onRetry: () => setState(() {}));
                }
                final paged = snap.data!;
                if (paged.items.isEmpty) {
                  return const EmptyView(message: 'Aucun paiement');
                }                return Column(
                  children: [
                    Expanded(
                      child: RefreshIndicator(
                        onRefresh: () async => setState(() {}),
                        child: ListView.builder(
                          padding: const EdgeInsets.all(16),
                          itemCount: paged.items.length,
                          itemBuilder: (_, i) {
                            final p = paged.items[i];
                            return Padding(
                              padding: const EdgeInsets.only(bottom: 10),
                              child: AccentCard(
                                status: '${p['status'] ?? ''}',
                                onTap: () => context.push('/payments/${p['id']}'),
                                child: Row(
                                  children: [
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text('${p['reference'] ?? '—'}', style: Tx.title(size: 14)),
                                          Text(
                                              '${p['shopName'] ?? ''} → ${p['supplierName'] ?? ''}',
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                              style: Tx.small(size: 12)),
                                          Text(fmtDate(p['createdAt']?.toString()),
                                              style: Tx.caption()),
                                        ],
                                      ),
                                    ),
                                    Flexible(
                                      flex: 0,
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.end,
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Text(fmtAmount(p['amount'], '${p['currency'] ?? ''}'),
                                              maxLines: 1,
                                              overflow: TextOverflow.ellipsis,
                                              textAlign: TextAlign.end,
                                              style: Tx.amount(size: 14)),
                                          const SizedBox(height: 4),
                                          StatusBadge('${p['status'] ?? ''}'),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                    Pager(
                      page: _page,
                      totalPages: paged.totalPages,
                      onPrev: () => setState(() => _page--),
                      onNext: () => setState(() => _page++),
                    ),
                  ],
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
