import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../core/auth/auth_provider.dart';
import '../../shared/widgets/ui.dart';

/// Détail commande + tunnel de statuts + commentaires (desktop
/// shop/order-detail.component + supplier/order-management.component).
/// [supplierView] = actions tunnel fournisseur, sinon actions boutique.
class OrderDetailScreen extends ConsumerStatefulWidget {
  final String id;
  final bool supplierView;
  const OrderDetailScreen({super.key, required this.id, this.supplierView = false});

  @override
  ConsumerState<OrderDetailScreen> createState() => _OrderDetailScreenState();
}

class _OrderDetailScreenState extends ConsumerState<OrderDetailScreen> {
  Future<Map<String, dynamic>>? _future;
  final _comment = TextEditingController();

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _comment.dispose();
    super.dispose();
  }

  void _load() => setState(() {
        _future = ref.read(shopApiProvider).order(widget.id);
      });

  Future<void> _act(String action, [Map<String, dynamic>? body]) async {
    try {
      if (widget.supplierView) {
        await ref.read(supplierApiProvider)!.transition(widget.id, action, body);
      } else {
        await ref.read(shopApiProvider).transition(widget.id, action, body);
      }
      if (mounted) showSnack(context, 'Commande mise à jour');
      _load();
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  Future<void> _confirmAct(String title, String message, String action,
      [Map<String, dynamic>? body, String ok = 'Confirmer']) async {
    if (await confirmDialog(context, title: title, message: message, ok: ok)) {
      await _act(action, body);
    }
  }

  @override
  Widget build(BuildContext context) {
    final roles = ref.watch(authProvider).roles;
    final isSupplierAdmin = roles.contains('SUPPLIER_ADMIN');
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Détail commande')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError || !snap.hasData) {
            return ErrorView(message: apiErrorMessage(snap.error ?? 'Erreur'), onRetry: _load);
          }
          final o = snap.data!;
          final status = '${o['status'] ?? ''}';
          final items = (o['items'] as List?) ?? [];
          final events = (o['events'] as List?) ?? [];
          return RefreshIndicator(
            onRefresh: () async => _load(),
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
                              child: Text('${o['reference'] ?? '—'}',
                                  style: Tx.body(color: Colors.white70, size: 15)),
                            ),
                            StatusBadge(status),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(fmtAmount(o['total'], '${o['currency'] ?? ''}'),
                            style: Tx.heroAmount()),
                        Text('${o['supplierName'] ?? ''} → ${o['shopName'] ?? ''}',
                            style: Tx.body(color: Colors.white70, size: 13)),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  DsCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        InfoRow('Créée par', '${o['createdByName'] ?? '—'}'),
                        InfoRow('Créée le', fmtDate(o['createdAt']?.toString())),
                        if ('${o['deliveryAgentName'] ?? ''}'.isNotEmpty)
                          InfoRow('Livreur', '${o['deliveryAgentName']}'),
                        if ('${o['plannedDeliveryDate'] ?? ''}'.isNotEmpty)
                          InfoRow('Livraison prévue', fmtDate(o['plannedDeliveryDate']?.toString())),
                        if ('${o['receivedByName'] ?? ''}'.isNotEmpty)
                          InfoRow('Reçu par', '${o['receivedByName']}'),
                        if ('${o['deliveryRejectionReason'] ?? ''}'.isNotEmpty)
                          InfoRow('Motif rejet livraison', '${o['deliveryRejectionReason']}'),
                        if ('${o['notes'] ?? ''}'.isNotEmpty) InfoRow('Notes', '${o['notes']}'),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  const SectionHeader('Articles'),
                  const SizedBox(height: 8),
                  DsCard(
                    padding: EdgeInsets.zero,
                    child: Column(
                      children: [
                        for (final it in items)
                          ListTile(
                            title: Text('${(it as Map)['productName'] ?? ''}',
                                style: const TextStyle(fontWeight: FontWeight.w500)),
                            subtitle: Text(
                                'Qté ${(it as Map)['quantity']} × ${fmtAmount((it as Map)['unitPrice'])}'),
                            trailing: Text(fmtAmount((it as Map)['lineTotal']),
                                style: const TextStyle(fontWeight: FontWeight.bold)),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  ..._actions(context, status, o, isSupplierAdmin),
                  const SizedBox(height: 12),
                  const SectionHeader('Historique'),
                  const SizedBox(height: 8),
                  Timeline([
                    for (final e in events)
                      TimelineEntry(
                        '${(e as Map)['action'] ?? ''}',
                        fmtDate((e as Map)['timestamp']?.toString()),
                        '${(e as Map)['details'] ?? ''}',
                      ),
                  ]),
                  const SizedBox(height: 12),
                  const SectionHeader('Commentaires'),
                  const SizedBox(height: 8),
                  _Comments(orderId: widget.id, draft: _comment, onSent: () {}),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  List<Widget> _actions(
      BuildContext context, String status, Map<String, dynamic> o, bool isSupplierAdmin) {
    final w = <Widget>[];
    void btn(String label, IconData icon, VoidCallback onTap, {bool primary = true}) {
      w.add(Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: SizedBox(
          width: double.infinity,
          height: 48,
          child: primary
              ? FilledButton.icon(onPressed: onTap, icon: Icon(icon), label: Text(label))
              : OutlinedButton.icon(onPressed: onTap, icon: Icon(icon), label: Text(label)),
        ),
      ));
    }

    if (widget.supplierView) {
      switch (status) {
        case 'DRAFT':
          if (isSupplierAdmin) {
            btn('Confirmer', Icons.check, () => _confirmAct('Confirmer', 'Confirmer la commande ?', 'confirm'));
            btn('Annuler', Icons.cancel_outlined, () => _confirmAct('Annuler', 'Annuler la commande ?', 'cancel', null, 'Annuler'), primary: false);
          }
        case 'CONFIRMED':
          if (isSupplierAdmin) {
            btn('Préparer', Icons.kitchen, () => _act('prepare'));
            btn('Annuler', Icons.cancel_outlined, () => _confirmAct('Annuler', 'Annuler la commande ?', 'cancel', null, 'Annuler'), primary: false);
          }
        case 'PREPARING':
          if (isSupplierAdmin) {
            btn('Prêt pour livraison', Icons.local_shipping, () => _act('ready'));
            btn('Annuler', Icons.cancel_outlined, () => _confirmAct('Annuler', 'Annuler la commande ?', 'cancel', null, 'Annuler'), primary: false);
          }
        case 'READY_FOR_DELIVERY':
          if (isSupplierAdmin) {
            btn('Assigner un livreur', Icons.person_add, () => _assignDelivery(context, o));
          }
        case 'IN_DELIVERY':
          if (isSupplierAdmin) {
            btn('Confirmer la livraison', Icons.verified, () => _act('confirm-delivery', {'confirmedDate': DateTime.now().toIso8601String()}));
            btn('Marquer livrée', Icons.done_all, () => _deliver(context));
            btn('Rejeter la livraison', Icons.block,
                () => _promptReason(context, 'Rejeter la livraison').then((r) {
                      if (r != null) _act('delivery-reject', {'reason': r});
                    }),
                primary: false);
          }
          btn('Accepter la livraison', Icons.thumb_up, () => _act('accept-delivery', {'accepted': true}));
          btn('Refuser la livraison', Icons.thumb_down,
              () => _promptReason(context, 'Refuser la livraison').then((r) {
                    if (r != null) _act('accept-delivery', {'accepted': false, 'reason': r});
                  }),
              primary: false);
        case 'DELIVERED':
          btn('Voir le litige', Icons.gavel, () => _openDispute(context, '${o['id']}'), primary: false);
        case 'DELIVERY_REJECTED':
          if (isSupplierAdmin) {
            btn('Re-confirmer', Icons.refresh, () => _act('confirm'));
            btn('Annuler', Icons.cancel_outlined, () => _confirmAct('Annuler', 'Annuler la commande ?', 'cancel', null, 'Annuler'), primary: false);
          }
      }
      if ('${o['asapPayment'] ?? false}' == 'true' && status != 'ACCEPTED' && status != 'CANCELLED') {
        btn('Accepter ASAP (paiement auto)', Icons.bolt, () => _act('accept-asap'));
      }
    } else {
      switch (status) {
        case 'DRAFT':
          btn('Annuler', Icons.cancel_outlined, () => _confirmAct('Annuler', 'Annuler la commande ?', 'cancel', null, 'Annuler'), primary: false);
        case 'DELIVERED':
          btn('Accepter', Icons.check_circle, () => _act('accept'));
          btn('Rejeter', Icons.cancel, () => _confirmAct('Rejeter', 'Rejeter la commande livrée ?', 'reject', null, 'Rejeter'), primary: false);
          btn('Ouvrir un litige', Icons.gavel, () => _createDispute(context, '${o['id']}'), primary: false);
        case 'ACCEPTED':
        case 'REJECTED':
        case 'CANCELLED':
          btn('Commander à nouveau', Icons.refresh, () async {
            try {
              final n = await ref.read(shopApiProvider).reorder('${o['id']}');
              if (context.mounted) {
                showSnack(context, 'Commande ${n['reference'] ?? ''} recréée');
                context.push('/shop/orders/${n['id']}');
              }
            } catch (e) {
              if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
            }
          });
      }
    }
    if (w.isEmpty) return const [];
    return [
      const SectionHeader('Actions'),
      const SizedBox(height: 8),
      ...w,
    ];
  }

  Future<String?> _promptReason(BuildContext context, String title) =>
      promptDialog(context, title: title, label: 'Motif');

  Future<void> _deliver(BuildContext context) async {
    final name = await promptDialog(context, title: 'Marquer livrée', label: 'Reçu par', ok: 'Livrer');
    if (name != null) await _act('deliver', {'receivedBy': name});
  }

  Future<void> _assignDelivery(BuildContext context, Map<String, dynamic> o) async {
    try {
      final agents = await ref.read(supplierApiProvider)!.agents();
      if (!context.mounted) return;
      String? agentId = agents.isNotEmpty ? '${agents.first['id']}' : null;
      final dateCtrl = TextEditingController(
          text: DateTime.now().add(const Duration(days: 1)).toIso8601String().substring(0, 10));
      final ok = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('Assigner un livreur'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String>(
                initialValue: agentId,
                decoration: dsInput('Livreur'),
                items: [
                  for (final a in agents)
                    DropdownMenuItem(
                        value: '${a['id']}',
                        child: Text('${a['firstName'] ?? ''} ${a['lastName'] ?? ''} (${a['username'] ?? ''})')),
                ],
                onChanged: (v) => agentId = v,
              ),
              const SizedBox(height: 12),
              TextField(controller: dateCtrl, decoration: dsInput('Date prévue (AAAA-MM-JJ)')),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Assigner')),
          ],
        ),
      );
      final planned = dateCtrl.text.trim();
      dateCtrl.dispose();
      if (ok == true && agentId != null) {
        await _act('assign-delivery', {
          'agentId': agentId,
          if (planned.isNotEmpty) 'plannedDeliveryDate': planned,
        });
      }
    } catch (e) {
      if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  Future<void> _createDispute(BuildContext context, String orderId) async {
    final reason = await promptDialog(context, title: 'Ouvrir un litige', label: 'Motif du litige');
    if (reason == null) return;
    try {
      final d = await ref.read(shopApiProvider).createDispute({'orderId': orderId, 'reason': reason});
      if (context.mounted) {
        showSnack(context, 'Litige créé');
        context.push('/shop/disputes/${d['id']}');
      }
    } catch (e) {
      if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  Future<void> _openDispute(BuildContext context, String orderId) async {
    try {
      final list = await ref.read(shopApiProvider).disputesByOrder(orderId);
      if (context.mounted) {
        if (list.isEmpty) {
          showSnack(context, 'Aucun litige pour cette commande');
        } else {
          context.push('/shop/disputes/${list.first['id']}');
        }
      }
    } catch (e) {
      if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }
}

class _Comments extends ConsumerStatefulWidget {
  final String orderId;
  final TextEditingController draft;
  final VoidCallback onSent;
  const _Comments({required this.orderId, required this.draft, required this.onSent});

  @override
  ConsumerState<_Comments> createState() => _CommentsState();
}

class _CommentsState extends ConsumerState<_Comments> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _future = ref.read(shopApiProvider).comments(widget.orderId);
  }

  @override
  Widget build(BuildContext context) {
    return DsCard(
      child: Column(
        children: [
          FutureBuilder(
            future: _future,
            builder: (ctx, snap) {
              if (snap.connectionState == ConnectionState.waiting) {
                return const Padding(
                    padding: EdgeInsets.all(8), child: CircularProgressIndicator());
              }
              final items = snap.data ?? [];
              if (items.isEmpty) {
                return const Text('Aucun commentaire', style: TextStyle(color: Ds.muted));
              }
              return Column(
                children: [
                  for (final c in items)
                    ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${c['content'] ?? ''}'),
                      subtitle: Text('${c['authorName'] ?? ''} • ${fmtDate(c['createdAt']?.toString())}'),
                    ),
                ],
              );
            },
          ),
          Row(
            children: [
              Expanded(child: TextField(controller: widget.draft, decoration: dsInput('Commentaire'))),
              IconButton(
                icon: const Icon(Icons.send, color: Ds.accent),
                onPressed: () async {
                  if (widget.draft.text.trim().isEmpty) return;
                  try {
                    await ref.read(shopApiProvider).addComment(widget.orderId, widget.draft.text.trim());
                    widget.draft.clear();
                    setState(() {
                      _future = ref.read(shopApiProvider).comments(widget.orderId);
                    });
                  } catch (e) {
                    if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
                  }
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}
