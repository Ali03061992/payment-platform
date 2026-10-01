import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'notif_pop.dart';
import '../../shared/widgets/ui.dart';

/// Bannière "bam" : glisse depuis le haut à chaque push,
/// un toucher ouvre l'écran concerné.
class PopBanner extends StatefulWidget {
  const PopBanner({super.key});

  @override
  State<PopBanner> createState() => _PopBannerState();
}

class _PopBannerState extends State<PopBanner> {
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  void _scheduleHide() {
    _timer?.cancel();
    _timer = Timer(const Duration(seconds: 5), () {
      if (notifPopBus.current != null) notifPopBus.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: notifPopBus,
      builder: (_, __) {
        final pop = notifPopBus.current;
        if (pop != null) _scheduleHide();
        final top = MediaQuery.of(context).padding.top + 8;
        return AnimatedSlide(
          duration: const Duration(milliseconds: 320),
          curve: Curves.easeOutBack,
          offset: Offset(0, pop == null ? -1.6 : 0),
          child: AnimatedOpacity(
            duration: const Duration(milliseconds: 250),
            opacity: pop == null ? 0 : 1,
            child: pop == null
                ? const SizedBox.shrink()
                : Padding(
                    padding: EdgeInsets.only(top: top, left: 14, right: 14),
                    child: GestureDetector(
                      onTap: () {
                        final route = pop.route;
                        notifPopBus.clear();
                        if (route != '__refresh__') context.push(route);
                      },
                      onVerticalDragUpdate: (_) => notifPopBus.clear(),
                      child: Container(
                        padding: const EdgeInsets.all(14),
                        decoration: BoxDecoration(
                          color: Ds.ink,
                          borderRadius: BorderRadius.circular(20),
                          boxShadow: [
                            BoxShadow(
                                color: Ds.ink.withValues(alpha: 0.4),
                                blurRadius: 24,
                                offset: const Offset(0, 10)),
                          ],
                        ),
                        child: Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: Ds.pop.withValues(alpha: 0.2),
                                borderRadius: BorderRadius.circular(14),
                              ),
                              child: const Icon(Icons.notifications_active_rounded,
                                  color: Ds.pop, size: 24),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Text(pop.title,
                                      style: const TextStyle(
                                          color: Colors.white,
                                          fontWeight: FontWeight.w800,
                                          fontSize: 14)),
                                  Text(pop.body,
                                      maxLines: 2,
                                      overflow: TextOverflow.ellipsis,
                                      style: const TextStyle(
                                          color: Colors.white70, fontSize: 12.5)),
                                ],
                              ),
                            ),
                            const Icon(Icons.chevron_right_rounded,
                                color: Colors.white54),
                          ],
                        ),
                      ),
                    ),
                  ),
          ),
        );
      },
    );
  }
}
