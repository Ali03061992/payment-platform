import 'package:dio/dio.dart';
import '../../core/api/api_models.dart';
import '../../core/api/api_helpers.dart';

/// Mêmes endpoints que OrderService / DisputeService / BalanceService (Angular).
class ShopApi {
  final Dio dio;
  ShopApi(this.dio);

  Future<PagedResponse<Map<String, dynamic>>> orders({int page = 0, int size = 20}) async {
    final res = await dio.get('/api/orders', queryParameters: {'page': page, 'size': size});
    final data = res.data;
    if (data is List) {
      return PagedResponse(
          items: unwrapList(data), totalElements: unwrapList(data).length, totalPages: 1, number: 0);
    }
    return PagedResponse.fromJson(asMap(data), (m) => m);
  }

  Future<Map<String, dynamic>> order(String id) async =>
      asMap((await dio.get('/api/orders/$id')).data);

  Future<Map<String, dynamic>> orderByReference(String ref) async =>
      asMap((await dio.get('/api/orders/reference/$ref')).data);

  Future<Map<String, dynamic>> createOrder(Map<String, dynamic> data) async =>
      asMap((await dio.post('/api/orders', data: data)).data);

  Future<Map<String, dynamic>> transition(String id, String action, [Map<String, dynamic>? body]) async =>
      asMap((await dio.post('/api/orders/$id/$action', data: body ?? {})).data);

  Future<List<Map<String, dynamic>>> comments(String orderId) async =>
      unwrapList((await dio.get('/api/orders/$orderId/comments')).data);

  Future<void> addComment(String orderId, String content) async {
    await dio.post('/api/orders/$orderId/comments', data: {'content': content});
  }

  Future<Map<String, dynamic>> reorder(String orderId) async =>
      asMap((await dio.post('/api/orders/$orderId/reorder')).data);

  Future<List<Map<String, dynamic>>> recentOrders([int limit = 5]) async =>
      unwrapList((await dio.get('/api/orders/recent', queryParameters: {'limit': limit})).data);

  Future<List<Map<String, dynamic>>> relationsByShop(String shopId) async =>
      unwrapList((await dio.get('/api/admin/supplier-shop-relations/shop/$shopId')).data);

  Future<List<Map<String, dynamic>>> productsOf(String supplierId) async =>
      unwrapList((await dio.get('/api/suppliers/$supplierId/products')).data);

  // Litiges
  Future<Map<String, dynamic>> createDispute(Map<String, dynamic> data) async =>
      asMap((await dio.post('/api/disputes', data: data)).data);

  Future<List<Map<String, dynamic>>> disputesByOrder(String orderId) async =>
      unwrapList((await dio.get('/api/disputes/order/$orderId')).data);

  Future<Map<String, dynamic>> dispute(String id) async =>
      asMap((await dio.get('/api/disputes/$id')).data);

  Future<Map<String, dynamic>> disputeMessage(String id, String content) async =>
      asMap((await dio.post('/api/disputes/$id/messages', data: {'content': content})).data);

  Future<Map<String, dynamic>> resolveDispute(String id, [String status = 'RESOLVED']) async =>
      asMap((await dio.post('/api/disputes/$id/resolve', data: {'status': status})).data);

  // Balance boutique
  Future<List<Map<String, dynamic>>> shopBalance(String shopId) async =>
      unwrapList((await dio.get('/api/balances/shop/$shopId')).data);
}
