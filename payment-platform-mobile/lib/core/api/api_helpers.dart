import 'package:dio/dio.dart';

/// Helpers API partagés. Mêmes enveloppes que le front Angular
/// (items/content/data ou liste brute).
List<Map<String, dynamic>> unwrapList(dynamic data) {
  if (data is List) {
    return data.map((e) => Map<String, dynamic>.from(e as Map)).toList();
  }
  if (data is Map) {
    for (final key in ['items', 'content', 'data']) {
      final v = (data as Map)[key];
      if (v is List) {
        return v.map((e) => Map<String, dynamic>.from(e as Map)).toList();
      }
    }
  }
  return const [];
}

Map<String, dynamic> asMap(dynamic data) {
  if (data is Map) return Map<String, dynamic>.from(data);
  return const {};
}

String apiErrorMessage(Object e, [String fallback = 'Erreur inattendue']) {
  if (e is DioException) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) {
      return '${data['message']}';
    }
    switch (e.response?.statusCode) {
      case 400:
        return 'Requête invalide';
      case 401:
        return 'Session expirée, reconnectez-vous';
      case 403:
        return 'Accès refusé pour votre rôle';
      case 404:
        return 'Élément introuvable';
      case 409:
        return (data is Map && data['message'] != null)
            ? '${data['message']}'
            : 'Conflit (stock insuffisant ou transition refusée)';
      default:
        return fallback;
    }
  }
  final s = '$e'.replaceFirst('Exception: ', '');
  return s.isEmpty ? fallback : s;
}

String fmtAmount(dynamic v, [String currency = '']) {
  final n = v is num ? v.toDouble() : double.tryParse('$v') ?? 0;
  final s = n.toStringAsFixed(2);
  return currency.isEmpty ? s : '$s $currency';
}

String fmtDate(String? iso) {
  if (iso == null || iso.isEmpty) return '—';
  final d = DateTime.tryParse(iso)?.toLocal();
  if (d == null) return iso;
  final p = (int x) => x.toString().padLeft(2, '0');
  return '${p(d.day)}/${p(d.month)}/${d.year} ${p(d.hour)}:${p(d.minute)}';
}
