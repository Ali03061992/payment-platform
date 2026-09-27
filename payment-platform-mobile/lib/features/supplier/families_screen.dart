import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Familles de produits (desktop family-management.component, soft-delete M3).
class FamiliesScreen extends ConsumerStatefulWidget {
  const FamiliesScreen({super.key});
  @override
  ConsumerState<FamiliesScreen> createState() => _FamiliesScreenState();
}

class _FamiliesScreenState extends ConsumerState<FamiliesScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.families();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Familles')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Familles')),
      floatingActionButton: FloatingActionButton(
        backgroundColor: Ds.accent,
        onPressed: _create,
        child: const Icon(Icons.add, color: Colors.white),
      ),
      body: FutureBuilder(
        future: _future,
        builder: (ctx, snap) {
          if (snap.connectionState == ConnectionState.waiting) return const LoadingView();
          if (snap.hasError) {
            return ErrorView(message: apiErrorMessage(snap.error!), onRetry: _load);
          }
          final items = snap.data ?? [];
          if (items.isEmpty) return const EmptyView(message: 'Aucune famille');
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              itemBuilder: (_, i) {
                final f = items[i];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DsCard(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${f['name'] ?? ''}', style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text('Code ${f['code'] ?? '—'}'),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete_outline, color: Ds.dangerText),
                        onPressed: () => _delete('${f['id']}', '${f['name']}'),
                      ),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }

  Future<void> _create() async {
    List<Map<String, dynamic>> cats = [];
    try {
      cats = await ref.read(supplierApiProvider)!.categories();
    } catch (_) {}
    if (!mounted) return;
    final name = TextEditingController();
    final code = TextEditingController();
    final selected = <String>{};
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx2, setS) => AlertDialog(
          title: const Text('Nouvelle famille'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(controller: name, decoration: dsInput('Nom')),
                const SizedBox(height: 12),
                TextField(controller: code, decoration: dsInput('Code')),
                const SizedBox(height: 12),
                const Align(
                  alignment: Alignment.centerLeft,
                  child: Text('Catégories', style: TextStyle(fontWeight: FontWeight.w600)),
                ),
                for (final c in cats)
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text('${c['name']}'),
                    value: selected.contains('${c['id']}'),
                    activeColor: Ds.accent,
                    onChanged: (v) => setS(() {
                      if (v == true) {
                        selected.add('${c['id']}');
                      } else {
                        selected.remove('${c['id']}');
                      }
                    }),
                  ),
              ],
            ),
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
            FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Créer')),
          ],
        ),
      ),
    );
    final n = name.text.trim();
    final cd = code.text.trim();
    name.dispose();
    code.dispose();
    if (ok != true || n.isEmpty || cd.isEmpty) return;
    try {
      await ref.read(supplierApiProvider)!.createFamily(n, cd, selected.toList());
      if (mounted) showSnack(context, 'Famille créée');
      _load();
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }

  Future<void> _delete(String id, String name) async {
    if (!await confirmDialog(context, title: 'Supprimer', message: 'Supprimer « $name » ?', ok: 'Supprimer')) {
      return;
    }
    try {
      await ref.read(supplierApiProvider)!.deleteFamily(id);
      if (mounted) showSnack(context, 'Famille supprimée');
      _load();
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }
}
