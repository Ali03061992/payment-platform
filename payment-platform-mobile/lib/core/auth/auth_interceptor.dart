import 'package:dio/dio.dart';
import 'auth_storage.dart';

/// Equivalent JwtInterceptor Angular + refresh auto (rotation M1).
class AuthInterceptor extends Interceptor {
  final AuthStorage storage;
  final Dio refreshDio;
  bool _refreshing = false;

  AuthInterceptor({required this.storage, required this.refreshDio});

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    if (options.path.contains('/api/auth/')) {
      handler.next(options);
      return;
    }
    final token = await storage.readAccess();
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) async {
    if (err.response?.statusCode != 401) {
      handler.next(err);
      return;
    }
    if (_refreshing) {
      handler.next(err);
      return;
    }
    _refreshing = true;
    try {
      final refresh = await storage.readRefresh();
      if (refresh == null) {
        handler.next(err);
        return;
      }
      final res = await refreshDio.post('/api/auth/refresh', data: {'refreshToken': refresh});
      final data = Map<String, dynamic>.from(res.data as Map);
      await storage.saveTokens(access: data['accessToken'] as String, refresh: data['refreshToken'] as String);
      final retry = await _retry(err.requestOptions);
      handler.resolve(retry);
    } catch (_) {
      handler.next(err);
    } finally {
      _refreshing = false;
    }
  }

  Future<Response<dynamic>> _retry(RequestOptions req) async {
    final token = await storage.readAccess();
    final opts = Options(method: req.method, headers: req.headers);
    opts.headers?['Authorization'] = 'Bearer $token';
    final dio = Dio(BaseOptions(baseUrl: req.baseUrl));
    return dio.request(req.path,
        data: req.data, queryParameters: req.queryParameters, options: opts);
  }
}
