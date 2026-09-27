import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_provider.dart';
import '../../../shared/widgets/ui.dart';

/// Initialisation du mot de passe (desktop password-setup.component).
class SetupPasswordScreen extends ConsumerStatefulWidget {
  const SetupPasswordScreen({super.key});
  @override
  ConsumerState<SetupPasswordScreen> createState() => _SetupPasswordScreenState();
}

class _SetupPasswordScreenState extends ConsumerState<SetupPasswordScreen> {
  final _token = TextEditingController();
  final _next = TextEditingController();
  final _confirm = TextEditingController();
  String? _error;
  String? _success;
  bool _loading = false;

  @override
  void dispose() {
    _token.dispose();
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
      await ref.read(dioProvider).post('/api/auth/password-setup',
          data: {'token': _token.text.trim(), 'newPassword': _next.text});
      setState(() => _success = 'Mot de passe initialisé, vous pouvez vous connecter');
    } catch (e) {
      setState(() => _error = 'Lien invalide ou expiré');
    } finally {
      setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)],
          ),
        ),
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(20),
              child: Container(
                constraints: const BoxConstraints(maxWidth: 420),
                padding: const EdgeInsets.all(32),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text('Initialiser le mot de passe',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Ds.ink)),
                    const SizedBox(height: 24),
                    TextField(controller: _token, decoration: dsInput("Jeton reçu par email")),
                    const SizedBox(height: 12),
                    TextField(controller: _next, decoration: dsInput('Nouveau mot de passe'), obscureText: true),
                    const SizedBox(height: 12),
                    TextField(controller: _confirm, decoration: dsInput('Confirmer'), obscureText: true),
                    if (_error != null) ...[
                      const SizedBox(height: 12),
                      Text(_error!, style: const TextStyle(color: Ds.dangerText)),
                    ],
                    if (_success != null) ...[
                      const SizedBox(height: 12),
                      Text(_success!, style: const TextStyle(color: Ds.successText)),
                    ],
                    const SizedBox(height: 16),
                    SizedBox(
                      height: 50,
                      child: FilledButton(
                        onPressed: _loading ? null : _submit,
                        child: Text(_loading ? 'Envoi...' : 'Valider'),
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextButton(onPressed: () => context.go('/login'), child: const Text('Retour connexion')),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
