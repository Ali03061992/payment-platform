import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/auth/auth_provider.dart';
import '../../../shared/widgets/ui.dart';

/// Login signature : gradient profond, halos lumineux, carte verre,
/// entrée en cascade.
class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});
  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _user = TextEditingController();
  final _pass = TextEditingController();
  String? _error;

  @override
  void dispose() {
    _user.dispose();
    _pass.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    try {
      await ref.read(authProvider.notifier).login(_user.text.trim(), _pass.text);
      if (mounted) context.go('/');
    } catch (e) {
      setState(() => _error = '$e'.replaceFirst('Exception: ', ''));
    }
  }

  @override
  Widget build(BuildContext context) {
    final loading = ref.watch(authProvider).loading;
    return Scaffold(
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: const BoxDecoration(gradient: Ds.headerGradient),
        child: Stack(
          children: [
            // Halos décoratifs.
            Positioned(
              top: -70,
              right: -70,
              child: Container(
                width: 220,
                height: 220,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Ds.pop.withValues(alpha: 0.22),
                ),
              ),
            ),
            Positioned(
              bottom: -90,
              left: -60,
              child: Container(
                width: 260,
                height: 260,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: Ds.teal.withValues(alpha: 0.25),
                ),
              ),
            ),
            SafeArea(
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    children: [
                      _rise(
                        0,
                        Container(
                          width: 84,
                          height: 84,
                          decoration: BoxDecoration(
                            gradient: Ds.goldGradient,
                            borderRadius: BorderRadius.circular(26),
                            boxShadow: [
                              BoxShadow(
                                  color: Ds.pop.withValues(alpha: 0.5),
                                  blurRadius: 30,
                                  offset: const Offset(0, 10)),
                            ],
                          ),
                          child: const Icon(Icons.payments_rounded, color: Colors.white, size: 44),
                        ),
                      ),
                      const SizedBox(height: 18),
                      _rise(
                        1,
                        const Text('Payment Platform',
                            style: TextStyle(
                                fontSize: 26, fontWeight: FontWeight.w800, color: Colors.white)),
                      ),
                      _rise(
                        2,
                        const Text('Fournisseurs & boutiques, en mouvement',
                            style: TextStyle(fontSize: 13.5, color: Colors.white70)),
                      ),
                      const SizedBox(height: 28),
                      _rise(
                        3,
                        Container(
                          constraints: const BoxConstraints(maxWidth: 420),
                          padding: const EdgeInsets.all(26),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(26),
                            boxShadow: [
                              BoxShadow(
                                  color: Colors.black.withValues(alpha: 0.3),
                                  blurRadius: 50,
                                  offset: const Offset(0, 18)),
                            ],
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              const Text('Connexion',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                      fontSize: 22, fontWeight: FontWeight.w800, color: Ds.deepText)),
                              const SizedBox(height: 4),
                              const Text('Accédez à votre espace',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(fontSize: 13, color: Ds.muted)),
                              const SizedBox(height: 20),
                              TextField(
                                controller: _user,
                                decoration: dsInput("Nom d'utilisateur", 'system.admin'),
                                textInputAction: TextInputAction.next,
                              ),
                              const SizedBox(height: 12),
                              TextField(
                                controller: _pass,
                                obscureText: true,
                                decoration: dsInput('Mot de passe', '••••••••'),
                                textInputAction: TextInputAction.done,
                                onSubmitted: (_) => loading ? null : _submit(),
                              ),
                              if (_error != null) ...[
                                const SizedBox(height: 12),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
                                  decoration: BoxDecoration(
                                      color: Ds.dangerBg, borderRadius: BorderRadius.circular(12)),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.error_outline,
                                          color: Ds.dangerText, size: 18),
                                      const SizedBox(width: 8),
                                      Expanded(
                                        child: Text(_error!,
                                            style: const TextStyle(
                                                fontSize: 13, color: Ds.dangerText)),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                              const SizedBox(height: 18),
                              Container(
                                height: 54,
                                decoration: BoxDecoration(
                                  gradient: const LinearGradient(
                                    colors: [Color(0xFF0F3460), Color(0xFF1E5AA8)],
                                    begin: Alignment.topLeft,
                                    end: Alignment.bottomRight,
                                  ),
                                  borderRadius: BorderRadius.circular(16),
                                  boxShadow: [
                                    BoxShadow(
                                        color: Ds.accent.withValues(alpha: 0.4),
                                        blurRadius: 16,
                                        offset: const Offset(0, 6)),
                                  ],
                                ),
                                child: FilledButton(
                                  onPressed: loading ? null : _submit,
                                  style: FilledButton.styleFrom(
                                    backgroundColor: Colors.transparent,
                                    shadowColor: Colors.transparent,
                                    shape: RoundedRectangleBorder(
                                        borderRadius: BorderRadius.circular(16)),
                                  ),
                                  child: loading
                                      ? const SizedBox(
                                          width: 22,
                                          height: 22,
                                          child: CircularProgressIndicator(
                                              color: Colors.white, strokeWidth: 2.5),
                                        )
                                      : const Text('Se connecter',
                                          style: TextStyle(fontSize: 16, color: Colors.white)),
                                ),
                              ),
                              const SizedBox(height: 14),
                              Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Text('Pas encore de compte ? ',
                                      style: TextStyle(fontSize: 13.5, color: Ds.muted)),
                                  GestureDetector(
                                    onTap: () => context.go('/register'),
                                    child: const Text('S’inscrire',
                                        style: TextStyle(
                                            fontSize: 13.5,
                                            color: Ds.accent,
                                            fontWeight: FontWeight.w700)),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Entrée en cascade : fondu + glissade.
  Widget _rise(int step, Widget child) {
    return TweenAnimationBuilder<double>(
      tween: Tween(begin: 0, end: 1),
      duration: Duration(milliseconds: 450 + step * 120),
      curve: Curves.easeOutCubic,
      builder: (_, v, __) => Opacity(
        opacity: v,
        child: Transform.translate(offset: Offset(0, 24 * (1 - v)), child: child),
      ),
    );
  }
}
