import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_provider.dart';
import '../../../shared/widgets/ui.dart';

const mobileRoles = ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'];

/// Même formulaire que le desktop (register.component).
class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});
  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _f = <String, TextEditingController>{
    'username': TextEditingController(),
    'email': TextEditingController(),
    'password': TextEditingController(),
    'firstName': TextEditingController(),
    'lastName': TextEditingController(),
    'phone': TextEditingController(),
  };
  String _role = 'SHOP_ADMIN';
  String? _error;
  String? _success;
  bool _loading = false;

  @override
  void dispose() {
    for (final c in _f.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _error = null;
      _success = null;
      _loading = true;
    });
    try {
      await ref.read(authProvider.notifier).register({
        for (final e in _f.entries) e.key: e.value.text.trim(),
        'role': _role,
      });
      setState(() => _success = 'Compte créé. Il sera activé par un administrateur.');
    } catch (e) {
      setState(() => _error = '$e'.replaceFirst('Exception: ', ''));
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
                constraints: const BoxConstraints(maxWidth: 480),
                padding: const EdgeInsets.all(32),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: const [
                    BoxShadow(color: Color.fromRGBO(0, 0, 0, 0.3), blurRadius: 60, offset: Offset(0, 20)),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text('Inscription',
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: Ds.ink)),
                    const SizedBox(height: 8),
                    const Text('Créez votre compte professionnel',
                        textAlign: TextAlign.center, style: TextStyle(fontSize: 14, color: Ds.muted)),
                    const SizedBox(height: 24),
                    TextField(controller: _f['username'], decoration: dsInput("Nom d'utilisateur")),
                    const SizedBox(height: 12),
                    TextField(controller: _f['email'], decoration: dsInput('Email'), keyboardType: TextInputType.emailAddress),
                    const SizedBox(height: 12),
                    TextField(controller: _f['password'], decoration: dsInput('Mot de passe'), obscureText: true),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(child: TextField(controller: _f['firstName'], decoration: dsInput('Prénom'))),
                        const SizedBox(width: 12),
                        Expanded(child: TextField(controller: _f['lastName'], decoration: dsInput('Nom'))),
                      ],
                    ),
                    const SizedBox(height: 12),
                    TextField(controller: _f['phone'], decoration: dsInput('Téléphone'), keyboardType: TextInputType.phone),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      initialValue: _role,
                      decoration: dsInput('Rôle'),
                      items: [for (final r in mobileRoles) DropdownMenuItem(value: r, child: Text(r))],
                      onChanged: (v) => setState(() => _role = v ?? _role),
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 12),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(color: Ds.dangerBg, borderRadius: BorderRadius.circular(8)),
                        child: Text(_error!, style: const TextStyle(color: Ds.dangerText, fontSize: 14)),
                      ),
                    ],
                    if (_success != null) ...[
                      const SizedBox(height: 12),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(color: Ds.successBg, borderRadius: BorderRadius.circular(8)),
                        child: Text(_success!, style: const TextStyle(color: Ds.successText, fontSize: 14)),
                      ),
                    ],
                    const SizedBox(height: 16),
                    SizedBox(
                      height: 52,
                      child: FilledButton(
                        onPressed: _loading ? null : _submit,
                        child: Text(_loading ? 'Envoi...' : "S'inscrire"),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Text('Déjà un compte ? ', style: TextStyle(color: Ds.muted)),
                        GestureDetector(
                          onTap: () => context.go('/login'),
                          child: const Text('Se connecter',
                              style: TextStyle(color: Ds.accent, fontWeight: FontWeight.w600)),
                        ),
                      ],
                    ),
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
