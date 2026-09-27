import 'package:dio/dio.dart';
import '../api/api_models.dart';

class AppNotification {
  final String id;
  final String type;
  final String message;
  final bool read;
  final DateTime createdAt;
  AppNotification({required this.id, required this.type, required this.message, required this.read, required this.createdAt});

  factory AppNotification.fromJson(Map<String, dynamic> j) => AppNotification(
        id: '${j['id']}',
        type: '${j['type'] ?? 'info'}',
        message: '${j['message'] ?? ''}',
        read: (j['readStatus'] ?? j['read'] ?? 'UNREAD').toString().toUpperCase() != 'UNREAD' &&
            (j['read'] == true || (j['readStatus'] ?? '') == 'READ'),
        createdAt: DateTime.tryParse('${j['createdAt'] ?? ''}') ?? DateTime.now(),
      );
}

class NotificationApi {
  final Dio dio;
  NotificationApi(this.dio);

  Future<PagedResponse<AppNotification>> list({int page = 0, int size = 20, String? type}) async {
    final res = await dio.get('/api/notifications', queryParameters: {
      'page': page,
      'size': size,
      if (type != null && type.isNotEmpty) 'type': type,
    });
    final data = res.data;
    if (data is List) {
      return PagedResponse(
          items: data.map((e) => AppNotification.fromJson(Map<String, dynamic>.from(e as Map))).toList(),
          totalElements: data.length,
          totalPages: 1,
          number: 0);
    }
    return PagedResponse.fromJson(Map<String, dynamic>.from(data as Map), AppNotification.fromJson);
  }

  Future<int> unreadCount() async {
    final res = await dio.get('/api/notifications/unread-count');
    final data = Map<String, dynamic>.from(res.data as Map);
    return (data['count'] ?? 0) as int;
  }

  Future<void> markRead(String id) => dio.post('/api/notifications/$id/read');
  Future<int> markAllRead() async {
    final res = await dio.post('/api/notifications/read-all');
    return ((res.data as Map)['updated'] ?? 0) as int;
  }

  Future<void> registerToken(String? token) async {
    if (token == null || token.isEmpty) return;
    await dio.post('/api/fcm-tokens', data: {'token': token});
  }

  Future<void> unregisterToken(String? token) async {
    if (token == null || token.isEmpty) return;
    await dio.delete('/api/fcm-tokens', data: {'token': token});
  }
}
