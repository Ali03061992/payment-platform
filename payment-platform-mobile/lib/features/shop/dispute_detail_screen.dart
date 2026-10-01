import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Détail litige + messages + résolution (desktop dispute-detail.component).
class DisputeDetailScreen extends ConsumerStatefulWidget {
  final String id;
  const DisputeDetailScreen({super.key, required this.id});

  @override
  ConsumerState<DisputeDetailScreen> createState() => _DisputeDetailScreenState();
}

class _DisputeDetailScreenState extends ConsumerState<DisputeDetailScreen> {
  Future<Map<String, dynamic>>? _future;
  final _msg = TextEditingController();
  String _orderRef = '';

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _msg.dispose();
    super.dispose();
  }

  void _load() => setState(() {
        _future = _loadFull();
      });

  /// Charge le litige puis résout la référence commande (jamais d'ID brut).
  Future<Map<String, dynamic>> _loadFull() async {
    final d = await ref.read(shopApiProvider).dispute(widget.id);
    final orderId = d['orderId']?.toString() ?? '';
    if ((d['orderReference'] == null || '${d['orderReference']}'.isEmpty) && orderId.isNotEmpty) {
      try {
        final o = await ref.read(shopApiProvider).order(orderId);
        _orderRef = '${o['reference'] ?? ''}';
      } catch (_) {}
    } else {
      _orderRef = '${d['orderReference'] ?? ''}';
    }
    return d;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Litige')),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError || !snap.hasData) {
            return ErrorView(message: apiErrorMessage(snap.error ?? 'Erreur'), onRetry: _load);
          }
          final d = snap.data!;
          final messages = (d['messages'] as List?) ?? [];
          final status = '${d['status'] ?? 'OPEN'}';
          return SingleChildScrollView(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                DsCard(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          const Text('Litige', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Ds.ink)),
                          StatusBadge(status),
                        ],
                      ),
                      const SizedBox(height: 8),
                      InfoRow('Commande', _orderRef.isEmpty ? '—' : _orderRef),
                      InfoRow('Motif', '${d['reason'] ?? '—'}'),
                      InfoRow('Créé le', fmtDate(d['createdAt']?.toString())),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text('Messages',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Ds.ink)),
                const SizedBox(height: 8),
                if (messages.isEmpty)
                  const DsCard(child: Text('Aucun message', style: TextStyle(color: Ds.muted)))
                else
                  for (final m in messages)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 8),
                      child: DsCard(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('${(m as Map)['content'] ?? ''}'),
                            Text(
                                '${(m as Map)['authorName'] ?? ''} • ${fmtDate((m as Map)['createdAt']?.toString())}',
                                style: const TextStyle(color: Ds.muted, fontSize: 12)),
                          ],
                        ),
                      ),
                    ),
                const SizedBox(height: 8),
                DsCard(
                  child: Row(
                    children: [
                      Expanded(child: TextField(controller: _msg, decoration: dsInput('Message'))),
                      IconButton(
                        icon: const Icon(Icons.send, color: Ds.accent),
                        onPressed: () async {
                          if (_msg.text.trim().isEmpty) return;
                          try {
                            await ref.read(shopApiProvider).disputeMessage(widget.id, _msg.text.trim());
                            _msg.clear();
                            _load();
                          } catch (e) {
                            if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
                          }
                        },
                      ),
                    ],
                  ),
                ),
                if (status != 'RESOLVED' && status != 'CLOSED') ...[
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 48,
                    child: FilledButton.icon(
                      onPressed: () async {
                        if (await confirmDialog(context,
                            title: 'Résoudre le litige', message: 'Marquer ce litige comme résolu ?')) {
                          try {
                            await ref.read(shopApiProvider).resolveDispute(widget.id);
                            if (context.mounted) showSnack(context, 'Litige résolu');
                            _load();
                          } catch (e) {
                            if (context.mounted) showSnack(context, apiErrorMessage(e), error: true);
                          }
                        }
                      },
                      icon: const Icon(Icons.verified),
                      label: const Text('Résoudre'),
                    ),
                  ),
                ],
              ],
            ),
          );
        },
      ),
    );
  }
}
