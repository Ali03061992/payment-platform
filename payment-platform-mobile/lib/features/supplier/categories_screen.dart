import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/providers.dart';
import '../../core/api/api_helpers.dart';
import '../../shared/widgets/ui.dart';

/// Catégories (desktop category-management.component, soft-delete M3).
class CategoriesScreen extends ConsumerStatefulWidget {
  const CategoriesScreen({super.key});
  @override
  ConsumerState<CategoriesScreen> createState() => _CategoriesScreenState();
}

class _CategoriesScreenState extends ConsumerState<CategoriesScreen> {
  Future<List<Map<String, dynamic>>>? _future;

  @override
  void initState() {
    super.initState();
    _load();
  }

  void _load() => setState(() {
        _future = ref.read(supplierApiProvider)?.categories();
      });

  @override
  Widget build(BuildContext context) {
    if (ref.watch(supplierApiProvider) == null) {
      return Scaffold(appBar: AppBar(title: const Text('Catégories')), body: const EmptyView(message: 'Réservé aux fournisseurs'));
    }
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Catégories')),
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
          if (items.isEmpty) return const EmptyView(message: 'Aucune catégorie');
          return RefreshIndicator(
            onRefresh: () async => _load(),
            child: ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              itemBuilder: (_, i) {
                final c = items[i];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DsCard(
                    child: ListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text('${c['name'] ?? ''}', style: const TextStyle(fontWeight: FontWeight.w600)),
                      subtitle: Text('Code ${c['code'] ?? '—'}'),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete_outline, color: Ds.dangerText),
                        onPressed: () => _delete('${c['id']}', '${c['name']}'),
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
    final name = TextEditingController();
    final code = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nouvelle catégorie'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: name, decoration: dsInput('Nom')),
            const SizedBox(height: 12),
            TextField(controller: code, decoration: dsInput('Code')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
          FilledButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Créer')),
        ],
      ),
    );
    final n = name.text.trim();
    final cd = code.text.trim();
    name.dispose();
    code.dispose();
    if (ok != true || n.isEmpty || cd.isEmpty) return;
    try {
      await ref.read(supplierApiProvider)!.createCategory(n, cd);
      if (mounted) showSnack(context, 'Catégorie créée');
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
      await ref.read(supplierApiProvider)!.deleteCategory(id);
      if (mounted) showSnack(context, 'Catégorie supprimée');
      _load();
    } catch (e) {
      if (mounted) showSnack(context, apiErrorMessage(e), error: true);
    }
  }
}
