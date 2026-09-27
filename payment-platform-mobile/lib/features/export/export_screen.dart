import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:share_plus/share_plus.dart';
import '../../core/auth/auth_provider.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Export CSV (desktop export.component) : paiements et commandes.
class ExportScreen extends ConsumerStatefulWidget {
  const ExportScreen({super.key});
  @override
  ConsumerState<ExportScreen> createState() => _ExportScreenState();
}

class _ExportScreenState extends ConsumerState<ExportScreen> {
  String? _status;
  bool _busy = false;

  Future<void> _export(String kind) async {
    setState(() => _busy = true);
    try {
      final dio = ref.read(dioProvider);
      final path = kind == 'payments' ? '/api/payments/export/csv' : '/api/orders/export/csv';
      final res = await dio.get<List<int>>(path,
          queryParameters: {if (_status != null && _status!.isNotEmpty) 'status': _status},
          options: Options(responseType: ResponseType.bytes));
      final bytes = res.data ?? [];
      final name = kind == 'payments' ? 'paiements.csv' : 'commandes.csv';
      await Share.shareXFiles(
          [XFile.fromData(Uint8List.fromList(bytes), name: name, mimeType: 'text/csv')],
          text: 'Export $name');
      if (mounted) showSnack(context, 'Export $name généré');
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Export')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: DsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              DropdownButtonFormField<String?>(
                initialValue: _status,
                decoration: dsInput('Statut (optionnel)'),
                items: const [
                  DropdownMenuItem(value: null, child: Text('Tous')),
                  DropdownMenuItem(value: 'PENDING', child: Text('En attente')),
                  DropdownMenuItem(value: 'CONFIRMED', child: Text('Confirmés')),
                  DropdownMenuItem(value: 'ACCEPTED', child: Text('Acceptés')),
                  DropdownMenuItem(value: 'CANCELLED', child: Text('Annulés')),
                ],
                onChanged: (v) => setState(() => _status = v),
              ),
              const SizedBox(height: 16),
              FilledButton.icon(
                onPressed: _busy ? null : () => _export('payments'),
                icon: const Icon(Icons.payments),
                label: const Text('Exporter les paiements (CSV)'),
              ),
              const SizedBox(height: 8),
              FilledButton.icon(
                onPressed: _busy ? null : () => _export('orders'),
                icon: const Icon(Icons.receipt_long),
                label: const Text('Exporter les commandes (CSV)'),
              ),
              if (_busy) ...[
                const SizedBox(height: 16),
                const Center(child: CircularProgressIndicator(color: Ds.accent)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
