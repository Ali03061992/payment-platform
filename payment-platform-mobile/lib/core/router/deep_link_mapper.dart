/// Convertit data.url backend (path Angular /dashboard/...) -> route mobile go_router.
/// Regle doc 03 : strip /dashboard, garder :id, fallback /notifications.
String mapBackendUrlToMobileRoute(String? url, {String? tag}) {
  if (url == null || url.isEmpty) return _routeForTag(tag);
  var path = url.split('?').first;
  if (path.startsWith('/dashboard')) path = path.substring('/dashboard'.length);
  if (path.isEmpty) path = '/';
  if (path == '/shop/deliveries') return '/shop/orders';
  return path.isEmpty ? _routeForTag(tag) : path;
}

String _routeForTag(String? tag) {
  final t = (tag ?? '').toLowerCase();
  if (t.startsWith('payment')) return '/payments';
  if (t.startsWith('order') || t.startsWith('delivery')) return '/supplier/orders';
  if (t.contains('dispute')) return '/shop/orders';
  if (t.contains('stock')) return '/supplier/low-stock-alerts';
  return '/notifications';
}

String routeForNotificationType(String? type) => _routeForTag(type);
