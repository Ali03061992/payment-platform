import 'package:dio/dio.dart';
import 'package:uuid/uuid.dart';
import '../../core/api/api_models.dart';
import '../../core/api/api_helpers.dart';

/// Mêmes endpoints que PaymentService (Angular).
class PaymentsApi {
  final Dio dio;
  PaymentsApi(this.dio);

  Future<PagedResponse<Map<String, dynamic>>> list({int page = 0, int size = 20, String? status}) async {
    final res = await dio.get('/api/payments', queryParameters: {
      'page': page,
      'size': size,
      if (status != null && status.isNotEmpty) 'status': status,
    });
    final data = res.data;
    if (data is List) {
      return PagedResponse(
          items: unwrapList(data), totalElements: unwrapList(data).length, totalPages: 1, number: 0);
    }
    return PagedResponse.fromJson(asMap(data), (m) => m);
  }

  Future<Map<String, dynamic>> getById(String id) async {
    final res = await dio.get('/api/payments/$id');
    return asMap(res.data);
  }

  Future<Map<String, dynamic>> getByReference(String ref) async {
    final res = await dio.get('/api/payments/reference/$ref');
    return asMap(res.data);
  }

  Future<Map<String, dynamic>> create(Map<String, dynamic> data, {String? idempotencyKey}) async {
    final res = await dio.post('/api/payments',
        data: data,
        options: Options(headers: {'Idempotency-Key': idempotencyKey ?? const Uuid().v4()}));
    return asMap(res.data);
  }

  Future<Map<String, dynamic>> confirm(String id) async =>
      asMap((await dio.post('/api/payments/$id/confirm')).data);

  Future<Map<String, dynamic>> reject(String id, String reason) async =>
      asMap((await dio.post('/api/payments/$id/reject', data: {'rejectionReason': reason})).data);

  Future<Map<String, dynamic>> cancel(String id) async =>
      asMap((await dio.post('/api/payments/$id/cancel')).data);

  Future<Map<String, dynamic>> stats() async =>
      asMap((await dio.get('/api/payments/stats')).data);

  Future<Map<String, dynamic>> supplierSummary(String supplierId) async =>
      asMap((await dio.get('/api/payments/supplier-summary',
              queryParameters: {'supplierId': supplierId})).data);

  Future<List<Map<String, dynamic>>> agentSummary(String supplierId, String from, String to) async {
    final res = await dio.get('/api/payments/agent-summary',
        queryParameters: {'supplierId': supplierId, 'from': from, 'to': to});
    return unwrapList(res.data);
  }
}
