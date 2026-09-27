import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../auth/auth_interceptor.dart';

String resolveBaseUrl() {
  // Priorite : --dart-define > .env. Sans config : web = same-origin
  // (proxy nginx /api, pas de CORS), natif = emulateur Android.
  const definedUrl = String.fromEnvironment('API_BASE_URL');
  if (definedUrl.isNotEmpty) return definedUrl;
  final envUrl = dotenv.maybeGet('API_BASE_URL');
  if (envUrl != null && envUrl.isNotEmpty) return envUrl;
  if (kIsWeb) return '';
  return 'http://10.0.2.2:8081';
}

Dio buildDio(AuthInterceptor authInterceptor) {
  final baseUrl = resolveBaseUrl();
  final dio = Dio(BaseOptions(
    baseUrl: baseUrl,
    connectTimeout: const Duration(seconds: 15),
    receiveTimeout: const Duration(seconds: 20),
    headers: {'Content-Type': 'application/json'},
  ));
  dio.interceptors.add(authInterceptor);
  dio.interceptors.add(LogInterceptor(requestBody: false, responseBody: false));
  return dio;
}
