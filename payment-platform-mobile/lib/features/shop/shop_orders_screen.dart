import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Liste des commandes boutique (desktop order-list.component).
class ShopOrdersScreen extends ConsumerStatefulWidget {
  const ShopOrdersScreen({super.key});
  @override
  ConsumerState<ShopOrdersScreen> createState() => _ShopOrdersScreenState();
}

class _ShopOrdersScreenState extends ConsumerState<ShopOrdersScreen> {
  int _page = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Commandes')),
      floatingActionButton: FloatingActionButton(
        backgroundColor: Ds.accent,
        onPressed: () => context.push('/shop/orders/create'),
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: FutureBuilder(
        future: ref.read(shopApiProvider).orders(page: _page, size: 20),
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
                          onTap: () => context.push('/shop/orders/${o['id']}'),
                          child: Row(
                            children: [
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text('${o['reference'] ?? '—'}', style: Tx.title(size: 14)),
                                    Text('${o['supplierName'] ?? ''}',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: Tx.small(size: 12)),
                                    Text(fmtDate(o['createdAt']?.toString()), style: Tx.caption()),
                                  ],
                                ),
                              ),
                              Column(
                                crossAxisAlignment: CrossAxisAlignment.end,
                                children: [
                                  Text(fmtAmount(o['total'], '${o['currency'] ?? ''}'),
                                      style: Tx.amount(size: 14)),
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
