import 'dart:convert';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AuthStorage {
  static const _accessKey = 'accessToken';
  static const _refreshKey = 'refreshToken';
  static const _userKey = 'user';
  final FlutterSecureStorage _secure = const FlutterSecureStorage();

  Future<void> saveTokens({required String access, required String refresh}) async {
    await _secure.write(key: _accessKey, value: access);
    await _secure.write(key: _refreshKey, value: refresh);
  }

  Future<String?> readAccess() => _secure.read(key: _accessKey);
  Future<String?> readRefresh() => _secure.read(key: _refreshKey);

  Future<void> saveUser(Map<String, dynamic> user) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_userKey, jsonEncode(user));
  }

  Future<Map<String, dynamic>?> readUser() async {
    final prefs = await SharedPreferences.getInstance();
    final raw = prefs.getString(_userKey);
    if (raw == null) return null;
    return Map<String, dynamic>.from(jsonDecode(raw) as Map);
  }

  Future<void> clear() async {
    await _secure.delete(key: _accessKey);
    await _secure.delete(key: _refreshKey);
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_userKey);
  }
}

List<String> rolesOf(Map<String, dynamic>? user) {
  if (user == null) return const [];
  final raw = user['roles'] ?? user['role'] ?? const [];
  if (raw is String) return [raw];
  if (raw is List) return raw.map((e) => e.toString()).toList();
  return const [];
}

bool isMobileRole(String role) => const {
      'SUPPLIER_ADMIN',
      'SUPPLIER_AGENT',
      'SHOP_ADMIN',
      'SHOP_AGENT',
    }.contains(role);
