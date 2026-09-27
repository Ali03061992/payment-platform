import 'package:dio/dio.dart';
import '../../core/api/api_models.dart';
import '../../core/api/api_helpers.dart';

/// Mêmes endpoints que StockService / CatalogService / StockOptimizationService /
/// SupplierAgentService / ReportService / BalanceService (Angular).
class SupplierApi {
  final Dio dio;
  final String supplierId;
  SupplierApi(this.dio, this.supplierId);

  // Produits & stock
  Future<List<Map<String, dynamic>>> products([String? status]) async {
    final res = await dio.get('/api/suppliers/$supplierId/products',
        queryParameters: {if (status != null && status.isNotEmpty) 'status': status});
    return unwrapList(res.data);
  }

  Future<Map<String, dynamic>> createProduct(Map<String, dynamic> data) async =>
      asMap((await dio.post('/api/suppliers/$supplierId/products', data: data)).data);

  Future<Map<String, dynamic>> updateProduct(String id, Map<String, dynamic> data) async =>
      asMap((await dio.patch('/api/suppliers/$supplierId/products/$id', data: data)).data);

  Future<void> deactivateProduct(String id) async {
    await dio.patch('/api/suppliers/$supplierId/products/$id/deactivate', data: {'status': 'INACTIVE'});
  }

  Future<List<Map<String, dynamic>>> movements([String? productId]) async {
    final res = await dio.get('/api/suppliers/$supplierId/movements',
        queryParameters: {if (productId != null) 'productId': productId});
    return unwrapList(res.data);
  }

  Future<Map<String, dynamic>> createMovement(String productId, Map<String, dynamic> data) async =>
      asMap((await dio.post('/api/suppliers/$supplierId/movements', data: {...data, 'productId': productId})).data);

  Future<List<Map<String, dynamic>>> lowStockAlerts() async =>
      unwrapList((await dio.get('/api/suppliers/$supplierId/low-stock-alerts')).data);

  // Catalogue
  Future<List<Map<String, dynamic>>> categories() async =>
      unwrapList((await dio.get('/api/supplier/catalog/categories',
              queryParameters: {'supplierId': supplierId})).data);

  Future<Map<String, dynamic>> createCategory(String name, String code) async => asMap(
      (await dio.post('/api/supplier/catalog/categories', data: {'supplierId': supplierId, 'name': name, 'code': code}))
          .data);

  Future<void> deleteCategory(String id) async {
    await dio.delete('/api/supplier/catalog/categories/$id');
  }

  Future<List<Map<String, dynamic>>> families([String? categoryId]) async {
    final res = await dio.get('/api/supplier/catalog/families', queryParameters: {
      'supplierId': supplierId,
      if (categoryId != null) 'categoryId': categoryId,
    });
    return unwrapList(res.data);
  }

  Future<Map<String, dynamic>> createFamily(String name, String code, List<String> categoryIds) async =>
      asMap((await dio.post('/api/supplier/catalog/families',
              data: {'supplierId': supplierId, 'name': name, 'code': code, 'categoryIds': categoryIds})).data);

  Future<Map<String, dynamic>> updateFamily(String id, String name, String code, List<String> categoryIds) async =>
      asMap((await dio.put('/api/supplier/catalog/families/$id',
              data: {'supplierId': supplierId, 'name': name, 'code': code, 'categoryIds': categoryIds})).data);

  Future<void> deleteFamily(String id) async {
    await dio.delete('/api/supplier/catalog/families/$id');
  }

  // Optimisation
  Future<Map<String, dynamic>> optimization() async =>
      asMap((await dio.get('/api/suppliers/$supplierId/optimization')).data);

  Future<Map<String, dynamic>> configureOptimization(
      double leadTimeDays, double orderingCost, double holdingCostPercent) async {
    final res = await dio.post('/api/suppliers/$supplierId/optimization/configure', queryParameters: {
      'leadTimeDays': leadTimeDays,
      'orderingCost': orderingCost,
      'holdingCostPercent': holdingCostPercent,
    });
    return asMap(res.data);
  }

  // Commandes & livraisons (tunnel fournisseur)
  Future<PagedResponse<Map<String, dynamic>>> orders({int page = 0, int size = 20}) async {
    final res = await dio.get('/api/orders', queryParameters: {'page': page, 'size': size});
    final data = res.data;
    if (data is List) {
      return PagedResponse(
          items: unwrapList(data), totalElements: unwrapList(data).length, totalPages: 1, number: 0);
    }
    return PagedResponse.fromJson(asMap(data), (m) => m);
  }

  Future<Map<String, dynamic>> createOrder(Map<String, dynamic> data) async =>
      asMap((await dio.post('/api/orders', data: data)).data);

  Future<Map<String, dynamic>> transition(String id, String action, [Map<String, dynamic>? body]) async =>
      asMap((await dio.post('/api/orders/$id/$action', data: body ?? {})).data);

  Future<List<Map<String, dynamic>>> myDeliveries() async =>
      unwrapList((await dio.get('/api/orders/my-deliveries')).data);

  Future<List<Map<String, dynamic>>> deliveries([String? agentId]) async => unwrapList(
      (await dio.get('/api/orders/deliveries', queryParameters: {if (agentId != null) 'agentId': agentId})).data);

  Future<List<Map<String, dynamic>>> relationsBySupplier() async =>
      unwrapList((await dio.get('/api/admin/supplier-shop-relations/supplier/$supplierId')).data);

  Future<List<Map<String, dynamic>>> shopAgents(String shopId) async =>
      unwrapList((await dio.get('/api/orders/shop-agents', queryParameters: {'shopId': shopId})).data);

  // Agents fournisseur
  Future<List<Map<String, dynamic>>> agents() async =>
      unwrapList((await dio.get('/api/suppliers/$supplierId/agents')).data);

  Future<Map<String, dynamic>> createAgent(Map<String, dynamic> data) async =>
      asMap((await dio.post('/api/suppliers/$supplierId/agents', data: data)).data);

  Future<void> setAgentActive(String agentId, bool active) async {
    await dio.patch('/api/suppliers/$supplierId/agents/$agentId/${active ? 'activate' : 'disable'}');
  }

  // Finance
  Future<Map<String, dynamic>> financialReport() async =>
      asMap((await dio.get('/api/reports/supplier-financial')).data);

  Future<List<Map<String, dynamic>>> supplierBalance() async =>
      unwrapList((await dio.get('/api/balances/supplier/$supplierId')).data);

  Future<Map<String, dynamic>> supplierSummary() async => asMap(
      (await dio.get('/api/payments/supplier-summary', queryParameters: {'supplierId': supplierId})).data);
}
