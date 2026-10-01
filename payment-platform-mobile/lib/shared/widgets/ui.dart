import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// Design system mobile : sobriété façon Apple, brand navy,
/// blanc aéré, ombres douces, graphiques fl_chart (App Store safe).
class Ds {
  static const accent = Color(0xFF0EA5E9);
  static const accentDeep = Color(0xFF0284C7);
  static const ink = Color(0xFF1A1A2E);
  static const deepText = Color(0xFF101828);
  static const textSecondary = Color(0xFF344054);
  static const muted = Color(0xFF667085);
  static const borderInput = Color(0xFFE0E0E0);
  static const cardBg = Colors.white;
  static const pageBg = Color(0xFFF6F7F9);

  // Touches d'accent, usage parcimonieux.
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
    colors: [Color(0xFF0C4A6E), Color(0xFF0284C7), Color(0xFF38BDF8)],
  );
  static const goldGradient = LinearGradient(
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
    colors: [Color(0xFF38BDF8), Color(0xFF0284C7)],
  );
}

/// Échelle typographique pro (Plus Jakarta Sans, store-safe).
/// À utiliser partout au lieu de TextStyle ad hoc.
class Tx {
  static TextStyle display({Color color = Ds.deepText, double size = 30}) =>
      GoogleFonts.plusJakartaSans(
          fontSize: size, fontWeight: FontWeight.w800, letterSpacing: -0.75, color: color, height: 1.15);

  static TextStyle h1({Color color = Ds.deepText, double size = 22}) =>
      GoogleFonts.plusJakartaSans(
          fontSize: size, fontWeight: FontWeight.w800, letterSpacing: -0.5, color: color, height: 1.2);

  static TextStyle h2({Color color = Ds.deepText, double size = 17}) =>
      GoogleFonts.plusJakartaSans(
          fontSize: size, fontWeight: FontWeight.w700, letterSpacing: -0.25, color: color, height: 1.25);

  static TextStyle title({Color color = Ds.deepText, double size = 15}) =>
      GoogleFonts.plusJakartaSans(fontSize: size, fontWeight: FontWeight.w700, color: color, height: 1.3);

  static TextStyle body({Color color = Ds.textSecondary, double size = 14}) =>
      GoogleFonts.plusJakartaSans(fontSize: size, fontWeight: FontWeight.w500, color: color, height: 1.45);

  static TextStyle small({Color color = Ds.muted, double size = 12.5}) =>
      GoogleFonts.plusJakartaSans(fontSize: size, fontWeight: FontWeight.w500, color: color, height: 1.4);

  static TextStyle caption({Color color = Ds.muted, double size = 11}) =>
      GoogleFonts.plusJakartaSans(fontSize: size, fontWeight: FontWeight.w600, color: color, height: 1.35);

  static TextStyle amount({Color color = Ds.deepText, double size = 20}) =>
      GoogleFonts.plusJakartaSans(
          fontSize: size, fontWeight: FontWeight.w800, letterSpacing: -0.25, color: color);

  static TextStyle heroAmount({Color color = Colors.white, double size = 34}) =>
      GoogleFonts.plusJakartaSans(
          fontSize: size, fontWeight: FontWeight.w800, letterSpacing: -0.5, color: color);

  static TextStyle btn({Color color = Colors.white, double size = 15}) =>
      GoogleFonts.plusJakartaSans(fontSize: size, fontWeight: FontWeight.w700, color: color);
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
            style: Tx.caption(color: fg),
          ),
        ],
      ),
    );
  }
}

/// Carte : radius généreux, ombre douce neutre.
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
        borderRadius: BorderRadius.circular(22),
        boxShadow: const [
          BoxShadow(
            color: Color.fromRGBO(16, 24, 40, 0.06),
            blurRadius: 16,
            offset: Offset(0, 4),
          ),
        ],
      ),
      child: child,
    );
    if (onTap == null) return card;
    return InkWell(
      borderRadius: BorderRadius.circular(22),
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
          Text(value, style: Tx.amount()),
          Text(label, style: Tx.small()),
        ],
      ),
    );
  }
}

/// Tuile d'action façon iOS : icône sur fond teinté, label sobre.
class QuickTile extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color tint;
  final VoidCallback onTap;
  const QuickTile({super.key, required this.icon, required this.label, required this.tint, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      child: Ink(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 4),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(18),
          boxShadow: const [
            BoxShadow(
                color: Color.fromRGBO(16, 24, 40, 0.05), blurRadius: 12, offset: Offset(0, 4)),
          ],
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: tint.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(14),
              ),
              child: Icon(icon, color: tint, size: 24),
            ),
            const SizedBox(height: 6),
            Text(label, textAlign: TextAlign.center, style: Tx.small(color: Ds.deepText)),
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
          child: Text(title, style: Tx.h2(size: 16)),
        ),
        if (actionLabel != null)
          TextButton(onPressed: onAction, child: Text(actionLabel!)),
      ],
    );
  }
}

/// Donut élégant (fl_chart) avec légende.
class DonutSlice {
  final String label;
  final double value;
  final Color color;
  const DonutSlice(this.label, this.value, this.color);
}

class DonutChart extends StatelessWidget {
  final List<DonutSlice> slices;
  final String centerLabel;
  final String centerValue;
  const DonutChart({super.key, required this.slices, this.centerLabel = '', this.centerValue = ''});

  @override
  Widget build(BuildContext context) {
    final total = slices.fold<double>(0, (a, s) => a + s.value);
    return Column(
      children: [
        SizedBox(
          height: 190,
          child: Stack(
            alignment: Alignment.center,
            children: [
              PieChart(
                PieChartData(
                  sectionsSpace: 3,
                  centerSpaceRadius: 58,
                  sections: List.generate(slices.length, (i) {
                    return PieChartSectionData(
                      value: slices[i].value <= 0 ? 0.001 : slices[i].value,
                      color: slices[i].color,
                      radius: 24,
                      showTitle: false,
                    );
                  }),
                ),
              ),
              Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(centerValue, style: Tx.amount(size: 24)),
                  Text(centerLabel, style: Tx.caption(size: 11.5)),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 14,
          runSpacing: 6,
          alignment: WrapAlignment.center,
          children: slices.map((s) {
            return _LegendDot(
              label: s.label,
              value: total <= 0 ? '0' : '${(s.value / total * 100).toStringAsFixed(0)}%',
              color: s.color,
            );
          }).toList(),
        ),
      ],
    );
  }
}

class _LegendDot extends StatelessWidget {
  final String label;
  final String value;
  final Color color;
  const _LegendDot({required this.label, required this.value, required this.color});

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
            width: 9,
            height: 9,
            decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
        const SizedBox(width: 5),
        Text('$label · $value', style: Tx.small(color: Ds.textSecondary)),
      ],
    );
  }
}

/// Barres verticales (fl_chart) pour montants par statut.
class DsBarChart extends StatelessWidget {
  final List<DonutSlice> bars;
  final double height;
  const DsBarChart(this.bars, {super.key, this.height = 170});

  @override
  Widget build(BuildContext context) {
    if (bars.isEmpty) {
      return const SizedBox(height: 40, child: Center(child: Text('—', style: TextStyle(color: Ds.muted))));
    }
    final maxY = bars.map((b) => b.value).reduce((a, b) => a > b ? a : b);
    return SizedBox(
      height: height,
      child: BarChart(
        BarChartData(
          maxY: maxY <= 0 ? 10 : maxY * 1.2,
          gridData: const FlGridData(show: false),
          borderData: FlBorderData(show: false),
          titlesData: FlTitlesData(
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                getTitlesWidget: (v, _) {
                  final i = v.toInt();
                  if (i < 0 || i >= bars.length) return const SizedBox.shrink();
                  return Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(bars[i].label,
                        style: const TextStyle(fontSize: 10, color: Ds.muted, fontWeight: FontWeight.w600)),
                  );
                },
              ),
            ),
          ),
          barGroups: List.generate(bars.length, (i) {
            return BarChartGroupData(x: i, barRods: [
              BarChartRodData(
                toY: bars[i].value,
                color: bars[i].color,
                width: 26,
                borderRadius: const BorderRadius.only(
                    topLeft: Radius.circular(8), topRight: Radius.circular(8)),
              ),
            ]);
          }),
        ),
      ),
    );
  }
}

/// Timeline verticale épurée.
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
                        width: 11,
                        height: 11,
                        decoration: BoxDecoration(
                          color: i == 0 ? Ds.accent : Colors.white,
                          shape: BoxShape.circle,
                          border: Border.all(
                              color: i == 0 ? Ds.accent : const Color(0xFFD0D5DD), width: 2),
                        ),
                      ),
                      if (i != entries.length - 1)
                        Expanded(child: Container(width: 2, color: const Color(0xFFEAECF0))),
                    ],
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Padding(
                      padding: EdgeInsets.only(bottom: i == entries.length - 1 ? 0 : 16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(entries[i].title, style: Tx.title(size: 13.5)),
                          Text(entries[i].subtitle, style: Tx.small(size: 12)),
                          if (entries[i].detail.isNotEmpty)
                            Text(entries[i].detail, style: Tx.body(size: 13)),
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
                textAlign: TextAlign.center, style: Tx.body(color: Ds.muted)),
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
            Text(message, textAlign: TextAlign.center, style: Tx.body()),
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
          SizedBox(width: 150, child: Text(label, style: Tx.small())),
          Expanded(child: Text(value, style: Tx.body(color: Ds.deepText))),
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
