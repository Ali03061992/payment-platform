import 'package:flutter/foundation.dart';

/// Bus global : un push reçu en avant-plan fait "pop" une bannière
/// dans l'app, même si les notifs système sont coupées.
class NotifPop {
  final String title;
  final String body;
  final String route;
  const NotifPop({required this.title, required this.body, required this.route});
}

class NotifPopBus extends ChangeNotifier {
  NotifPop? current;

  void pop(NotifPop pop) {
    current = pop;
    notifyListeners();
  }

  void clear() {
    current = null;
    notifyListeners();
  }
}

final notifPopBus = NotifPopBus();
