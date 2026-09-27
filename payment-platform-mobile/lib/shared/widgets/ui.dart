import 'package:flutter/material.dart';

/// Design system mobile moderne : navy brand + accent vibrant,
/// cartes arrondies, pills avec point lumineux, headers en gradient.
class Ds {
  static const accent = Color(0xFF0F3460);
  static const ink = Color(0xFF1A1A2E);
  static const deepText = Color(0xFF101828);
  static const textSecondary = Color(0xFF344054);
  static const muted = Color(0xFF667085);
  static const borderInput = Color(0xFFE0E0E0);
  static const cardBg = Colors.white;
  static const pageBg = Color(0xFFF2F5FA);

  // Accent vibrant (actions, highlights).
  static const pop = Color(0xFFF0A22E);
  static const teal = Color(0xFF0E9F8A);

  static const successBg = Color(0xFFE8F5E9);
  static const successText = Color(0xFF2E7D32);
  static const infoBg = Color(0xFFE3F2FD);
  static const infoText = Color(0xFF1565C0);
  static const warningBg = Color(0xFFFFF3E0);
  static const warningText = Color(0xFFE65100);
  static const dangerBg = Color(0xFFFCE4EC);
  static const dangerText = Color(0xFFC62828);

  static const headerGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)],
  );
  static const goldGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFFF0A22E), Color(0xFFD97B1A)],
  );
}

/// Pastille de statut moderne : pilule + point lumineux.
class StatusBadge extends StatelessWidget {
  final String status;
  const StatusBadge(this.status, {super.key});

  static (Color, Color) colorsFor(String s) {
    switch (s.toUpperCase()) {
      case 'CONFIRMED':
      case 'ACCEPTED':
      case 'ACTIVE':
      case 'DELIVERED':
      case 'RESOLVED':
        return (Ds.successBg, Ds.successText);
      case 'PENDING':
      case 'DRAFT':
      case 'CONFIRMED_DELIVERY':
      case 'IN_DELIVERY':
      case 'PREPARING':
      case 'READY_FOR_DELIVERY':
      case 'OPEN':
        return (Ds.warningBg, Ds.warningText);
      case 'REJECTED':
      case 'CANCELLED':
      case 'INACTIVE':
      case 'DELIVERY_REJECTED':
      case 'OUT_OF_STOCK':
        return (Ds.dangerBg, Ds.dangerText);
      default:
        return (Ds.infoBg, Ds.infoText);
    }
  }

  @override
  Widget build(BuildContext context) {
    final (bg, fg) = colorsFor(status);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(20)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            width: 7,
            height: 7,
            decoration: BoxDecoration(color: fg, shape: BoxShape.circle),
          ),
          const SizedBox(width: 6),
          Text(
            status.replaceAll('_', ' '),
            style: TextStyle(fontSize: 11.5, fontWeight: FontWeight.w700, color: fg),
          ),
        ],
      ),
    );
  }
}

/// Carte moderne : radius 20, ombre douce colorée.
class DsCard extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  final VoidCallback? onTap;
  const DsCard({super.key, required this.child, this.padding = const EdgeInsets.all(16), this.onTap});

  @override
  Widget build(BuildContext context) {
    final card = Container(
      padding: padding,
      decoration: BoxDecoration(
        color: Ds.cardBg,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Ds.accent.withValues(alpha: 0.07),
            blurRadius: 18,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: child,
    );
    if (onTap == null) return card;
    return InkWell(
      borderRadius: BorderRadius.circular(20),
      onTap: onTap,
      child: card,
    );
  }
}

/// Carte liste avec barre d'accent latérale colorée par statut.
class AccentCard extends StatelessWidget {
  final String status;
  final Widget child;
  final VoidCallback? onTap;
  const AccentCard({super.key, required this.status, required this.child, this.onTap});

  @override
  Widget build(BuildContext context) {
    final (_, fg) = StatusBadge.colorsFor(status);
    return DsCard(
      onTap: onTap,
      padding: EdgeInsets.zero,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            width: 5,
            decoration: BoxDecoration(
              color: fg,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(20),
                bottomLeft: Radius.circular(20),
              ),
            ),
          ),
          Expanded(child: Padding(padding: const EdgeInsets.all(14), child: child)),
        ],
      ),
    );
  }
}

/// Tuile statistique moderne avec tuile icône en gradient.
class StatCard extends StatelessWidget {
  final String label;
  final String value;
  final IconData icon;
  final Color color;
  final VoidCallback? onTap;
  const StatCard({super.key, required this.label, required this.value, required this.icon, required this.color, this.onTap});

  @override
  Widget build(BuildContext context) {
    return DsCard(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [color, color.withValues(alpha: 0.65)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(14),
              boxShadow: [
                BoxShadow(color: color.withValues(alpha: 0.35), blurRadius: 10, offset: const Offset(0, 4)),
              ],
            ),
            child: Icon(icon, color: Colors.white, size: 22),
          ),
          const SizedBox(height: 10),
          Text(value,
              style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800, color: Ds.deepText)),
          Text(label, style: const TextStyle(fontSize: 12, color: Ds.muted)),
        ],
      ),
    );
  }
}

/// Tuile d'action rapide colorée.
class QuickTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final List<Color> gradient;
  final VoidCallback onTap;
  const QuickTile({super.key, required this.icon, required this.label, required this.gradient, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      child: Ink(
        padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 8),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: gradient, begin: Alignment.topLeft, end: Alignment.bottomRight),
          borderRadius: BorderRadius.circular(18),
          boxShadow: [
            BoxShadow(color: gradient.last.withValues(alpha: 0.4), blurRadius: 12, offset: const Offset(0, 5)),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: Colors.white, size: 26),
            const SizedBox(height: 6),
            Text(label,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w700)),
          ],
        ),
      ),
    );
  }
}

/// Titre de section avec trait d'accent.
class SectionHeader extends StatelessWidget {
  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;
  const SectionHeader(this.title, {super.key, this.actionLabel, this.onAction});

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(width: 4, height: 20,
            decoration: BoxDecoration(color: Ds.pop, borderRadius: BorderRadius.circular(4))),
        const SizedBox(width: 8),
        Expanded(
          child: Text(title,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800, color: Ds.deepText)),
        ),
        if (actionLabel != null)
          TextButton(onPressed: onAction, child: Text(actionLabel!)),
      ],
    );
  }
}

/// Mini graphique en barres dessiné à la main (aucune dépendance).
class MiniBars extends StatelessWidget {
  final List<double> values;
  final double height;
  const MiniBars(this.values, {super.key, this.height = 56});

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: height,
      child: CustomPaint(painter: _BarsPainter(values)),
    );
  }
}

class _BarsPainter extends CustomPainter {
  final List<double> values;
  _BarsPainter(this.values);

  @override
  void paint(Canvas canvas, Size size) {
    if (values.isEmpty) return;
    final max = values.reduce((a, b) => a > b ? a : b);
    final n = values.length;
    final gap = 5.0;
    final bw = (size.width - gap * (n - 1)) / n;
    for (var i = 0; i < n; i++) {
      final h = max <= 0 ? 4.0 : 4.0 + (values[i] / max) * (size.height - 4);
      final x = i * (bw + gap);
      final rect = RRect.fromRectAndRadius(
        Rect.fromLTWH(x, size.height - h, bw, h),
        const Radius.circular(4),
      );
      final paint = Paint()
        ..shader = const LinearGradient(
          colors: [Ds.accent, Ds.teal],
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
        ).createShader(Rect.fromLTWH(x, 0, bw, size.height));
      canvas.drawRRect(rect, paint);
    }
  }

  @override
  bool shouldRepaint(covariant _BarsPainter old) => old.values != values;
}

/// Timeline verticale pour historiques (paiements, commandes).
class Timeline extends StatelessWidget {
  final List<TimelineEntry> entries;
  const Timeline(this.entries, {super.key});

  @override
  Widget build(BuildContext context) {
    if (entries.isEmpty) {
      return const DsCard(child: Text('Aucun événement', style: TextStyle(color: Ds.muted)));
    }
    return DsCard(
      child: Column(
        children: [
          for (var i = 0; i < entries.length; i++)
            IntrinsicHeight(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Column(
                    children: [
                      Container(
                        width: 12,
                        height: 12,
                        decoration: BoxDecoration(
                          color: i == 0 ? Ds.pop : Ds.accent,
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 2),
                          boxShadow: [
                            BoxShadow(
                                color: (i == 0 ? Ds.pop : Ds.accent).withValues(alpha: 0.4),
                                blurRadius: 6),
                          ],
                        ),
                      ),
                      if (i != entries.length - 1)
                        Expanded(child: Container(width: 2, color: const Color(0xFFE4E7EC))),
                    ],
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Padding(
                      padding: EdgeInsets.only(bottom: i == entries.length - 1 ? 0 : 16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(entries[i].title,
                              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
                          Text(entries[i].subtitle,
                              style: const TextStyle(color: Ds.muted, fontSize: 12)),
                          if (entries[i].detail.isNotEmpty)
                            Text(entries[i].detail, style: const TextStyle(fontSize: 13)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class TimelineEntry {
  final String title;
  final String subtitle;
  final String detail;
  const TimelineEntry(this.title, this.subtitle, [this.detail = '']);
}

/// Squelette de chargement animé (shimmer maison).
class SkeletonList extends StatefulWidget {
  final int count;
  const SkeletonList({super.key, this.count = 5});

  @override
  State<SkeletonList> createState() => _SkeletonListState();
}

class _SkeletonListState extends State<SkeletonList> with SingleTickerProviderStateMixin {
  late final AnimationController _c;

  @override
  void initState() {
    super.initState();
    _c = AnimationController(vsync: this, duration: const Duration(milliseconds: 1100))..repeat(reverse: true);
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _c,
      builder: (_, __) => ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: widget.count,
        itemBuilder: (_, i) => Container(
          height: 92,
          margin: const EdgeInsets.only(bottom: 10),
          decoration: BoxDecoration(
            color: Color.lerp(const Color(0xFFE4E7EC), const Color(0xFFF2F4F7), _c.value),
            borderRadius: BorderRadius.circular(20),
          ),
        ),
      ),
    );
  }
}

/// État vide illustré.
class EmptyView extends StatelessWidget {
  final String message;
  final IconData icon;
  const EmptyView({super.key, required this.message, this.icon = Icons.inbox_outlined});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(22),
              decoration: BoxDecoration(
                gradient: LinearGradient(colors: [
                  Ds.accent.withValues(alpha: 0.1),
                  Ds.teal.withValues(alpha: 0.1),
                ]),
                shape: BoxShape.circle,
              ),
              child: Icon(icon, size: 52, color: Ds.accent),
            ),
            const SizedBox(height: 14),
            Text(message,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Ds.muted, fontSize: 14, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}

class LoadingView extends StatelessWidget {
  const LoadingView({super.key});
  @override
  Widget build(BuildContext context) => const SkeletonList();
}

class ErrorView extends StatelessWidget {
  final String message;
  final VoidCallback onRetry;
  const ErrorView({super.key, required this.message, required this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(18),
              decoration: const BoxDecoration(color: Ds.dangerBg, shape: BoxShape.circle),
              child: const Icon(Icons.wifi_off_rounded, size: 44, color: Ds.dangerText),
            ),
            const SizedBox(height: 12),
            Text(message, textAlign: TextAlign.center, style: const TextStyle(color: Ds.textSecondary)),
            const SizedBox(height: 16),
            FilledButton.icon(
                onPressed: onRetry, icon: const Icon(Icons.refresh), label: const Text('Réessayer')),
          ],
        ),
      ),
    );
  }
}

/// Ligne label/valeur.
class InfoRow extends StatelessWidget {
  final String label;
  final String value;
  const InfoRow(this.label, this.value, {super.key});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(width: 150, child: Text(label, style: const TextStyle(color: Ds.muted, fontSize: 13))),
          Expanded(
              child: Text(value,
                  style: const TextStyle(color: Ds.deepText, fontSize: 13, fontWeight: FontWeight.w600))),
        ],
      ),
    );
  }
}

/// Pagination.
class Pager extends StatelessWidget {
  final int page;
  final int totalPages;
  final VoidCallback onPrev;
  final VoidCallback onNext;
  const Pager({super.key, required this.page, required this.totalPages, required this.onPrev, required this.onNext});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          IconButton(onPressed: page > 0 ? onPrev : null, icon: const Icon(Icons.chevron_left)),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
            decoration: BoxDecoration(color: Ds.accent.withValues(alpha: 0.08), borderRadius: BorderRadius.circular(16)),
            child: Text('Page ${page + 1}${totalPages > 0 ? ' / $totalPages' : ''}',
                style: const TextStyle(fontWeight: FontWeight.w700, color: Ds.accent)),
          ),
          IconButton(
              onPressed: totalPages == 0 || page + 1 >= totalPages ? null : onNext,
              icon: const Icon(Icons.chevron_right)),
        ],
      ),
    );
  }
}

Future<bool> confirmDialog(BuildContext context, {required String title, required String message, String ok = 'Confirmer'}) async {
  final res = await showDialog<bool>(
    context: context,
    builder: (ctx) => AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: Text(title),
      content: Text(message),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Annuler')),
        FilledButton(onPressed: () => Navigator.pop(ctx, true), child: Text(ok)),
      ],
    ),
  );
  return res ?? false;
}

Future<String?> promptDialog(BuildContext context,
    {required String title, String label = 'Motif', String ok = 'Valider', bool required = true}) async {
  final ctrl = TextEditingController();
  final res = await showDialog<String>(
    context: context,
    builder: (ctx) => AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      title: Text(title),
      content: TextField(controller: ctrl, decoration: InputDecoration(labelText: label), maxLines: 3),
      actions: [
        TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Annuler')),
        FilledButton(
          onPressed: () {
            if (required && ctrl.text.trim().isEmpty) return;
            Navigator.pop(ctx, ctrl.text.trim());
          },
          child: Text(ok),
        ),
      ],
    ),
  );
  ctrl.dispose();
  return res;
}

void showSnack(BuildContext context, String message, {bool error = false}) {
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      content: Text(message),
      behavior: SnackBarBehavior.floating,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      backgroundColor: error ? Ds.dangerText : Ds.ink,
    ),
  );
}

InputDecoration dsInput(String label, [String? hint]) => InputDecoration(
      labelText: label,
      hintText: hint,
      filled: true,
      fillColor: Colors.white,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: Ds.borderInput, width: 1.5),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(14),
        borderSide: const BorderSide(color: Ds.accent, width: 2),
      ),
    );
