import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/auth_provider.dart';
import '../../features/payments/payments_api.dart';
import '../../features/shop/shop_api.dart';
import '../../features/supplier/supplier_api.dart';

final paymentsApiProvider = Provider((ref) => PaymentsApi(ref.watch(dioProvider)));
final shopApiProvider = Provider((ref) => ShopApi(ref.watch(dioProvider)));

/// Null si l'utilisateur n'est pas rattaché à un fournisseur.
final supplierApiProvider = Provider<SupplierApi?>((ref) {
  final user = ref.watch(authProvider).user;
  final orgId = user?['organizationId']?.toString() ?? '';
  final roles = ref.watch(authProvider).roles;
  if (orgId.isEmpty || !roles.any((r) => r.startsWith('SUPPLIER'))) return null;
  return SupplierApi(ref.watch(dioProvider), orgId);
});

String? orgIdOf(Map<String, dynamic>? user) {
  final v = user?['organizationId']?.toString() ?? '';
  return v.isEmpty ? null : v;
}
