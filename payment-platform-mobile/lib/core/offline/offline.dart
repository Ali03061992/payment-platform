import 'package:hive_flutter/hive_flutter.dart';

class CacheService {
  static Future<void> init() async {
    await Hive.initFlutter();
  }

  static Future<void> put(String box, String key, Map<String, dynamic> value) async {
    final b = await Hive.openBox(box);
    await b.put(key, value);
  }

  static Future<Map<String, dynamic>?> get(String box, String key) async {
    final b = await Hive.openBox(box);
    final v = b.get(key);
    if (v is Map) return Map<String, dynamic>.from(v);
    return null;
  }
}

/// File d'attente offline : paiements/commandes créés sans réseau, rejoués avec même Idempotency-Key.
class OutboxQueue {
  final List<Map<String, dynamic>> _pending = [];
  List<Map<String, dynamic>> get pending => List.unmodifiable(_pending);
  void enqueue(Map<String, dynamic> op) => _pending.add(op);
  void remove(String key) => _pending.removeWhere((e) => e['key'] == key);
}
