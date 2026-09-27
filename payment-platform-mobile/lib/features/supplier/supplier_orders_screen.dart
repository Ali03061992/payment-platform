import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Commandes fournisseur (desktop order-management.component).
class SupplierOrdersScreen extends ConsumerStatefulWidget {
  const SupplierOrdersScreen({super.key});
  @override
  ConsumerState<SupplierOrdersScreen> createState() => _SupplierOrdersScreenState();
}

class _SupplierOrdersScreenState extends ConsumerState<SupplierOrdersScreen> {
  int _page = 0;

  @override
  Widget build(BuildContext context) {
    final api = ref.watch(supplierApiProvider);
    final isAdmin = ref.watch(authProvider).roles.contains('SUPPLIER_ADMIN');
    if (api == null) {
      return Scaffold(appBar: AppBar(title: const Text('Commandes')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Commandes')),
      floatingActionButton: isAdmin
          ? FloatingActionButton(
              backgroundColor: Ds.accent,
              onPressed: () => context.push('/supplier/orders/create'),
              child: const Icon(Icons.add, color: Colors.white),
            )
          : null,
      body: FutureBuilder(
        future: api.orders(page: _page, size: 20),
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: () => setState(() {}));
          }
          final paged = snap.data!;
          if (paged.items.isEmpty) return const EmptyView(message: 'Aucune commande');
          return Column(
            children: [
              Expanded(
                child: RefreshIndicator(
                  onRefresh: () async => setState(() {}),
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: paged.items.length,
                    itemBuilder: (_, i) {
                      final o = paged.items[i];
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 10),
                        child: AccentCard(
                          status: '${o['status'] ?? ''}',
                          onTap: () => context.push('/supplier/orders/${o['id']}'),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text('${o['reference'] ?? '—'}',
                                        style: const TextStyle(
                                            fontWeight: FontWeight.w700, fontSize: 14)),
                                    Text('${o['shopName'] ?? ''}',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: const TextStyle(color: Ds.muted, fontSize: 12)),
                                    Text(fmtDate(o['createdAt']?.toString()),
                                        style: const TextStyle(color: Ds.muted, fontSize: 11)),
                                  ],
                                ),
                              ),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(fmtAmount(o['total'], '${o['currency'] ?? ''}'),
                                      style: const TextStyle(
                                          fontWeight: FontWeight.w800, fontSize: 14)),
                                  const SizedBox(height: 4),
                                  StatusBadge('${o['status'] ?? ''}'),
                                ],
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
    );
  }
}
