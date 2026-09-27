import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'auth_storage.dart';
import 'auth_interceptor.dart';
import '../api/dio_client.dart';
import '../notifications/notification_api.dart';

final authStorageProvider = Provider((_) => AuthStorage());

final refreshDioProvider = Provider((ref) {
  return Dio(BaseOptions(baseUrl: resolveBaseUrl(), headers: {'Content-Type': 'application/json'}));
});

final authInterceptorProvider = Provider((ref) =>
    AuthInterceptor(storage: ref.watch(authStorageProvider), refreshDio: ref.watch(refreshDioProvider)));

final dioProvider = Provider((ref) => buildDio(ref.watch(authInterceptorProvider)));

final notificationApiProvider = Provider((ref) => NotificationApi(ref.watch(dioProvider)));

class AuthState {
  final bool loading;
  final Map<String, dynamic>? user;
  AuthState({this.loading = false, this.user});

  bool get isLoggedIn => user != null;

  List<String> get roles => rolesOf(user);
}

class AuthNotifier extends StateNotifier<AuthState> {
  final Ref ref;
  AuthNotifier(this.ref) : super(AuthState());

  List<String> get roles => state.roles;

  bool get isLoggedIn => state.isLoggedIn;

  bool hasAnyRole(List<String> allowed) => roles.any(allowed.contains);

  Future<void> bootstrap() async {
    final storage = ref.read(authStorageProvider);
    final user = await storage.readUser();
    final token = await storage.readAccess();
    if (user != null && token != null) {
      if (rolesOf(user).contains('SYSTEM_ADMIN')) {
        await logout();
        return;
      }
      state = AuthState(user: user);
      try {
        final me = await ref.read(dioProvider).get('/api/auth/me');
        final fresh = Map<String, dynamic>.from(me.data as Map);
        await storage.saveUser(fresh);
        state = AuthState(user: fresh);
      } catch (_) {}
    }
  }

  /// Même flux que le desktop (LoginComponent) : POST /login (tokens),
  /// puis GET /me (profil). Le login ne renvoie jamais le user.
  Future<void> login(String username, String password) async {
    state = AuthState(loading: true);
    try {
      final dio = ref.read(dioProvider);
      final res = await dio.post('/api/auth/login', data: {'username': username, 'password': password});
      final data = Map<String, dynamic>.from(res.data as Map);
      final storage = ref.read(authStorageProvider);
      await storage.saveTokens(
        access: data['accessToken'] as String,
        refresh: (data['refreshToken'] ?? '') as String,
      );
      final me = await dio.get('/api/auth/me');
      final user = Map<String, dynamic>.from(me.data as Map);
      if (rolesOf(user).contains('SYSTEM_ADMIN')) {
        await storage.clear();
        state = AuthState();
        throw Exception('Compte SYSTEM_ADMIN : utilisez le front desktop.');
      }
      await storage.saveUser(user);
      state = AuthState(user: user);
    } on DioException catch (e) {
      state = AuthState();
      final msg = (e.response?.data is Map)
          ? (e.response!.data as Map)['message']?.toString()
          : null;
      throw Exception(msg ?? 'Identifiants invalides');
    } catch (e) {
      if (e is Exception) rethrow;
      state = AuthState();
      throw Exception('Impossible de récupérer les informations utilisateur');
    }
  }

  /// Inscription (M5 : le compte naît DISABLED, validation admin).
  Future<void> register(Map<String, dynamic> data) async {
    try {
      await ref.read(dioProvider).post('/api/auth/register', data: data);
    } on DioException catch (e) {
      final msg = (e.response?.data is Map)
          ? (e.response!.data as Map)['message']?.toString()
          : null;
      throw Exception(msg ?? "Échec de l'inscription");
    }
  }

  /// Changement de mot de passe (révoque les refresh, M1).
  Future<void> changePassword(String currentPassword, String newPassword) async {
    try {
      await ref.read(dioProvider).post('/api/auth/change-password',
          data: {'currentPassword': currentPassword, 'newPassword': newPassword});
    } on DioException catch (e) {
      final msg = (e.response?.data is Map)
          ? (e.response!.data as Map)['message']?.toString()
          : null;
      throw Exception(msg ?? 'Échec du changement de mot de passe');
    }
  }

  Future<void> logout() async {    try {
      final refresh = await ref.read(authStorageProvider).readRefresh();
      if (refresh != null) {
        await ref.read(dioProvider).post('/api/auth/logout', data: {'refreshToken': refresh});
      }
    } catch (_) {}
    try {
      await ref.read(notificationApiProvider).unregisterToken(await _cachedFcmToken());
    } catch (_) {}
    await ref.read(authStorageProvider).clear();
    state = AuthState();
  }

  Future<String?> _cachedFcmToken() async {
    return null;
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) => AuthNotifier(ref));
