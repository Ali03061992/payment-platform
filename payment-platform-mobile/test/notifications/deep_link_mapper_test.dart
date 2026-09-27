import 'package:flutter_test/flutter_test.dart';
import 'package:payment_platform_mobile/core/router/deep_link_mapper.dart';

void main() {
  test('payment tag -> /payments/:id via url', () {
    expect(mapBackendUrlToMobileRoute('/dashboard/payments/123', tag: 'payment.created'), '/payments/123');
  });
  test('angular shop order url -> mobile', () {
    expect(mapBackendUrlToMobileRoute('/dashboard/shop/orders/abc', tag: 'order.confirmed'), '/shop/orders/abc');
  });
  test('low stock tag fallback', () {
    expect(mapBackendUrlToMobileRoute(null, tag: 'stock.low'), '/supplier/low-stock-alerts');
  });
  test('shop deliveries -> shop orders', () {
    expect(mapBackendUrlToMobileRoute('/dashboard/shop/deliveries'), '/shop/orders');
  });
  test('unknown -> notifications', () {
    expect(mapBackendUrlToMobileRoute(null, tag: 'unknown'), '/notifications');
  });
}
