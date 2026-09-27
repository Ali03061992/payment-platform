class ApiException implements Exception {
  final int? statusCode;
  final String message;
  ApiException(this.message, [this.statusCode]);
  @override
  String toString() => 'ApiException($statusCode): $message';
}

class PagedResponse<T> {
  final List<T> items;
  final int totalElements;
  final int totalPages;
  final int number;
  PagedResponse({required this.items, required this.totalElements, required this.totalPages, required this.number});

  static PagedResponse<T> fromJson<T>(Map<String, dynamic> json, T Function(Map<String, dynamic>) fromItem) {
    final raw = json['items'] as List? ?? const [];
    return PagedResponse<T>(
      items: raw.map((e) => fromItem(Map<String, dynamic>.from(e as Map))).toList(),
      totalElements: (json['totalElements'] ?? 0) as int,
      totalPages: (json['totalPages'] ?? 0) as int,
      number: (json['number'] ?? json['currentPage'] ?? 0) as int,
    );
  }
}
