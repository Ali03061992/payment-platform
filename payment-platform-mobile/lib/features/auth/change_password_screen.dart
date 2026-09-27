import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/auth/auth_provider.dart';
import '../../../shared/widgets/ui.dart';

/// Changement de mot de passe (desktop change-password.component).
class ChangePasswordScreen extends ConsumerStatefulWidget {
  const ChangePasswordScreen({super.key});
  @override
  ConsumerState<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends ConsumerState<ChangePasswordScreen> {
  final _current = TextEditingController();
  final _next = TextEditingController();
  final _confirm = TextEditingController();
  String? _error;
  String? _success;
  bool _loading = false;

  @override
  void dispose() {
    _current.dispose();
    _next.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_next.text != _confirm.text) {
      setState(() => _error = 'Les mots de passe ne correspondent pas');
      return;
    }
    setState(() {
      _error = null;
      _success = null;
      _loading = true;
    });
    try {
      await ref.read(authProvider.notifier).changePassword(_current.text, _next.text);
      setState(() => _success = 'Mot de passe modifié avec succès');
      _current.clear();
      _next.clear();
      _confirm.clear();
    } catch (e) {
      setState(() => _error = '$e'.replaceFirst('Exception: ', ''));
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Mot de passe')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: DsCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextField(controller: _current, decoration: dsInput('Mot de passe actuel'), obscureText: true),
              const SizedBox(height: 12),
              TextField(controller: _next, decoration: dsInput('Nouveau mot de passe'), obscureText: true),
              const SizedBox(height: 12),
              TextField(controller: _confirm, decoration: dsInput('Confirmer le mot de passe'), obscureText: true),
              if (_error != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: Ds.dangerBg, borderRadius: BorderRadius.circular(8)),
                  child: Text(_error!, style: const TextStyle(color: Ds.dangerText)),
                ),
              ],
              if (_success != null) ...[
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(color: Ds.successBg, borderRadius: BorderRadius.circular(8)),
                  child: Text(_success!, style: const TextStyle(color: Ds.successText)),
                ),
              ],
              const SizedBox(height: 16),
              SizedBox(
                height: 50,
                child: FilledButton(
                  onPressed: _loading ? null : _submit,
                  child: Text(_loading ? 'Envoi...' : 'Modifier'),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
