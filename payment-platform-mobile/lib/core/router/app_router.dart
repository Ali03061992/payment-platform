import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../auth/auth_provider.dart';
import '../notifications/notification_providers.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/register_screen.dart';
import '../../features/auth/setup_password_screen.dart';
import '../../features/auth/change_password_screen.dart';
import '../../features/dashboard/dashboard_screen.dart';
import '../../features/payments/payments_list_screen.dart';
import '../../features/payments/payment_create_screen.dart';
import '../../features/payments/payment_detail_screen.dart';
import '../../features/payments/payment_stats_screen.dart';
import '../../features/shop/shop_orders_screen.dart';
import '../../features/shop/order_create_screen.dart';
import '../../features/shop/order_detail_screen.dart';
import '../../features/shop/dispute_detail_screen.dart';
import '../../features/shop/shop_balance_screen.dart';
import '../../features/supplier/stock_dashboard_screen.dart';
import '../../features/supplier/stock_screen.dart';
import '../../features/supplier/add_product_screen.dart';
import '../../features/supplier/products_screen.dart';
import '../../features/supplier/categories_screen.dart';
import '../../features/supplier/families_screen.dart';
import '../../features/supplier/optimization_screen.dart';
import '../../features/supplier/low_stock_alerts_screen.dart';
import '../../features/supplier/supplier_orders_screen.dart';
import '../../features/supplier/deliveries_screen.dart';
import '../../features/supplier/agent_payments_screen.dart';
import '../../features/supplier/supplier_financial_screen.dart';
import '../../features/supplier/supplier_balance_screen.dart';
import '../../features/scan/scan_screen.dart';
import '../../features/export/export_screen.dart';
import '../../features/notifications/notifications_screen.dart';
import '../../shared/widgets/ui.dart';

const _supplierOnly = ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'];
const _supplierAdminOnly = ['SUPPLIER_ADMIN'];
const _shopOnly = ['SHOP_ADMIN', 'SHOP_AGENT'];
const _allMobile = [..._supplierOnly, ..._shopOnly];

/// Équivalent AuthGuard + RoleGuard (desktop) : sans token -> /login,
/// SYSTEM_ADMIN ou rôle non autorisé -> /403.
class AppRouter {
  static final Map<String, List<String>> _routeRoles = {
    '/': _allMobile,
    '/payments': _allMobile,
    '/payments/create': _shopOnly,
    '/payments/stats': _allMobile,
    '/scan': _allMobile,
    '/export': _allMobile,
    '/notifications': _allMobile,
    '/change-password': _allMobile,
    '/supplier/agent-payments': _supplierOnly,
    '/supplier/financial': _supplierAdminOnly,
    '/supplier/balance': _supplierAdminOnly,
    '/supplier/stock': _supplierOnly,
    '/supplier/optimization': _supplierAdminOnly,
    '/supplier/stock/create': _supplierAdminOnly,
    '/supplier/categories': _supplierAdminOnly,
    '/supplier/families': _supplierAdminOnly,
    '/supplier/products': _supplierAdminOnly,
    '/supplier/dashboard': _supplierOnly,
    '/supplier/low-stock-alerts': _supplierAdminOnly,
    '/supplier/orders': _supplierOnly,
    '/supplier/orders/create': _supplierAdminOnly,
    '/supplier/deliveries': _supplierOnly,
    '/shop/orders': _shopOnly,
    '/shop/orders/create': _shopOnly,
    '/shop/balance': _shopOnly,
  };

  static List<String>? _allowedFor(String loc) {
    if (_routeRoles.containsKey(loc)) return _routeRoles[loc];
    if (RegExp(r'^/payments/[^/]+$').hasMatch(loc)) return _allMobile;
    if (RegExp(r'^/supplier/orders/[^/]+$').hasMatch(loc)) return _supplierOnly;
    if (RegExp(r'^/shop/orders/[^/]+$').hasMatch(loc)) return _shopOnly;
    if (RegExp(r'^/shop/disputes/[^/]+$').hasMatch(loc)) return _shopOnly;
    return null;
  }

  static GoRouter build(WidgetRef ref) {
    return GoRouter(
      initialLocation: '/login',
      redirect: (context, state) {
        final auth = ref.read(authProvider);
        final loc = state.matchedLocation;
        const public = ['/login', '/register', '/setup-password', '/403', '/404'];
        if (!auth.isLoggedIn) return public.contains(loc) ? null : '/login';
        if (auth.roles.contains('SYSTEM_ADMIN')) return '/403';
        final allowed = _allowedFor(loc);
        if (allowed != null && !auth.roles.any(allowed.contains)) return '/403';
        return null;
      },
      routes: [
        GoRoute(path: '/login', builder: (_, __) => const LoginScreen()),
        GoRoute(path: '/register', builder: (_, __) => const RegisterScreen()),
        GoRoute(path: '/setup-password', builder: (_, __) => const SetupPasswordScreen()),
        GoRoute(path: '/403', builder: (_, __) => const _Forbidden()),
        GoRoute(path: '/404', builder: (_, __) => const _NotFound()),
        ShellRoute(
          builder: (context, state, child) => AppScaffold(child: child),
          routes: [
            GoRoute(path: '/', builder: (_, __) => const DashboardScreen()),
            GoRoute(
              path: '/payments',
              builder: (_, s) => PaymentsListScreen(initialStatus: s.uri.queryParameters['status']),
            ),
            GoRoute(path: '/payments/create', builder: (_, __) => const PaymentCreateScreen()),
            GoRoute(path: '/payments/stats', builder: (_, __) => const PaymentStatsScreen()),
            GoRoute(
                path: '/payments/:id',
                builder: (_, s) => PaymentDetailScreen(id: s.pathParameters['id']!)),
            GoRoute(path: '/scan', builder: (_, __) => const ScanScreen()),
            GoRoute(path: '/export', builder: (_, __) => const ExportScreen()),
            GoRoute(path: '/notifications', builder: (_, __) => const NotificationsScreen()),
            GoRoute(path: '/change-password', builder: (_, __) => const ChangePasswordScreen()),
            GoRoute(path: '/supplier/dashboard', builder: (_, __) => const StockDashboardScreen()),
            GoRoute(path: '/supplier/stock', builder: (_, __) => const StockScreen()),
            GoRoute(path: '/supplier/stock/create', builder: (_, __) => const AddProductScreen()),
            GoRoute(path: '/supplier/products', builder: (_, __) => const ProductsScreen()),
            GoRoute(path: '/supplier/categories', builder: (_, __) => const CategoriesScreen()),
            GoRoute(path: '/supplier/families', builder: (_, __) => const FamiliesScreen()),
            GoRoute(path: '/supplier/optimization', builder: (_, __) => const OptimizationScreen()),
            GoRoute(
                path: '/supplier/low-stock-alerts',
                builder: (_, __) => const LowStockAlertsScreen()),
            GoRoute(path: '/supplier/orders', builder: (_, __) => const SupplierOrdersScreen()),
            GoRoute(
                path: '/supplier/orders/create',
                builder: (_, __) => const OrderCreateScreen(supplierView: true)),
            GoRoute(
                path: '/supplier/orders/:id',
                builder: (_, s) =>
                    OrderDetailScreen(id: s.pathParameters['id']!, supplierView: true)),
            GoRoute(path: '/supplier/deliveries', builder: (_, __) => const DeliveriesScreen()),
            GoRoute(
                path: '/supplier/agent-payments', builder: (_, __) => const AgentPaymentsScreen()),
            GoRoute(
                path: '/supplier/financial', builder: (_, __) => const SupplierFinancialScreen()),
            GoRoute(path: '/supplier/balance', builder: (_, __) => const SupplierBalanceScreen()),
            GoRoute(path: '/shop/orders', builder: (_, __) => const ShopOrdersScreen()),
            GoRoute(
                path: '/shop/orders/create', builder: (_, __) => const OrderCreateScreen()),
            GoRoute(
                path: '/shop/orders/:id',
                builder: (_, s) => OrderDetailScreen(id: s.pathParameters['id']!)),
            GoRoute(
                path: '/shop/disputes/:id',
                builder: (_, s) => DisputeDetailScreen(id: s.pathParameters['id']!)),
            GoRoute(path: '/shop/balance', builder: (_, __) => const ShopBalanceScreen()),
          ],
        ),
      ],
      errorBuilder: (_, __) => const _NotFound(),
    );
  }
}

class _Forbidden extends StatelessWidget {
  const _Forbidden();
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: Ds.pageBg,
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('403', style: TextStyle(fontSize: 64, fontWeight: FontWeight.bold, color: Ds.accent)),
              const Text('Accès refusé pour votre rôle'),
              const SizedBox(height: 16),
              FilledButton(onPressed: () => context.go('/'), child: const Text('Accueil')),
            ],
          ),
        ),
      );
}

class _NotFound extends StatelessWidget {
  const _NotFound();
  @override
  Widget build(BuildContext context) => Scaffold(
        backgroundColor: Ds.pageBg,
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('404', style: TextStyle(fontSize: 64, fontWeight: FontWeight.bold, color: Ds.accent)),
              const Text('Page introuvable'),
              const SizedBox(height: 16),
              FilledButton(onPressed: () => context.go('/'), child: const Text('Accueil')),
            ],
          ),
        ),
      );
}

class _MenuEntry {
  final String path;
  final String label;
  final IconData icon;
  const _MenuEntry(this.path, this.label, this.icon);
}

List<_MenuEntry> _menuFor(List<String> roles) {
  final isSupplier = roles.any((r) => r.startsWith('SUPPLIER'));
  final isAdmin = roles.any((r) => r.endsWith('_ADMIN'));
  if (isSupplier) {
    return [
      const _MenuEntry('/', 'Tableau de bord', Icons.home),
      const _MenuEntry('/supplier/dashboard', 'Dashboard stock', Icons.inventory),
      const _MenuEntry('/supplier/orders', 'Commandes', Icons.receipt_long),
      const _MenuEntry('/supplier/deliveries', 'Livraisons', Icons.local_shipping),
      const _MenuEntry('/payments', 'Paiements', Icons.payments),
      const _MenuEntry('/supplier/agent-payments', 'Paiements agents', Icons.group),
      const _MenuEntry('/supplier/stock', 'Stock', Icons.warehouse),
      const _MenuEntry('/supplier/products', 'Produits', Icons.shopping_basket),
      if (isAdmin) const _MenuEntry('/supplier/categories', 'Catégories', Icons.category),
      if (isAdmin) const _MenuEntry('/supplier/families', 'Familles', Icons.folder),
      if (isAdmin) const _MenuEntry('/supplier/optimization', 'Optimisation', Icons.auto_graph),
      if (isAdmin)
        const _MenuEntry('/supplier/low-stock-alerts', 'Alertes stock', Icons.warning_amber),
      if (isAdmin) const _MenuEntry('/supplier/financial', 'Finance', Icons.bar_chart),
      if (isAdmin) const _MenuEntry('/supplier/balance', 'Balance', Icons.account_balance_wallet),
      const _MenuEntry('/payments/stats', 'Stats paiements', Icons.analytics),
      const _MenuEntry('/scan', 'Scanner', Icons.qr_code_scanner),
      const _MenuEntry('/export', 'Export', Icons.file_download),
      const _MenuEntry('/notifications', 'Notifications', Icons.notifications),
      const _MenuEntry('/change-password', 'Mot de passe', Icons.lock),
    ];
  }
  return [
    const _MenuEntry('/', 'Tableau de bord', Icons.home),
    const _MenuEntry('/shop/orders', 'Commandes', Icons.shopping_bag),
    const _MenuEntry('/payments', 'Paiements', Icons.payments),
    const _MenuEntry('/payments/create', 'Créer paiement', Icons.add_card),
    const _MenuEntry('/payments/stats', 'Stats paiements', Icons.analytics),
    const _MenuEntry('/shop/balance', 'Balance', Icons.account_balance_wallet),
    const _MenuEntry('/scan', 'Scanner', Icons.qr_code_scanner),
    const _MenuEntry('/export', 'Export', Icons.file_download),
    const _MenuEntry('/notifications', 'Notifications', Icons.notifications),
    const _MenuEntry('/change-password', 'Mot de passe', Icons.lock),
  ];
}

class AppScaffold extends ConsumerWidget {
  final Widget child;
  const AppScaffold({super.key, required this.child});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final unread = ref.watch(unreadCountProvider);
    final auth = ref.watch(authProvider);
    final roles = auth.roles;
    final isSupplier = roles.any((r) => r.startsWith('SUPPLIER'));
    final username = auth.user?['username']?.toString() ?? '';
    final tabs = isSupplier
        ? const [
            {'path': '/supplier/dashboard', 'label': 'Stock', 'icon': Icons.inventory},
            {'path': '/supplier/orders', 'label': 'Commandes', 'icon': Icons.receipt_long},
            {'path': '/payments', 'label': 'Paiements', 'icon': Icons.payments},
            {'path': '/notifications', 'label': 'Notifs', 'icon': Icons.notifications},
          ]
        : const [
            {'path': '/', 'label': 'Accueil', 'icon': Icons.home},
            {'path': '/shop/orders', 'label': 'Commandes', 'icon': Icons.shopping_bag},
            {'path': '/payments', 'label': 'Paiements', 'icon': Icons.payments},
            {'path': '/notifications', 'label': 'Notifs', 'icon': Icons.notifications},
          ];
    return Scaffold(
      backgroundColor: Ds.pageBg,
      appBar: AppBar(title: const Text('Payment Platform')),
      drawer: Drawer(
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.only(
            topRight: Radius.circular(26),
            bottomRight: Radius.circular(26),
          ),
        ),
        child: ListView(
          padding: EdgeInsets.zero,
          children: [
            DrawerHeader(
              decoration: const BoxDecoration(gradient: Ds.headerGradient),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  Container(
                    width: 52,
                    height: 52,
                    decoration: BoxDecoration(
                      gradient: Ds.goldGradient,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Center(
                      child: Text(
                        username.isNotEmpty ? username[0].toUpperCase() : '?',
                        style: const TextStyle(
                            color: Colors.white, fontSize: 24, fontWeight: FontWeight.w800),
                      ),
                    ),
                  ),
                  const SizedBox(height: 10),
                  Text(username, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold)),
                  Text(roles.join(' • '), style: const TextStyle(color: Colors.white70, fontSize: 12)),
                ],
              ),
            ),
            for (final m in _menuFor(roles))
              ListTile(
                leading: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Ds.accent.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Icon(m.icon, color: Ds.accent, size: 20),
                ),
                title: Text(m.label, style: const TextStyle(fontWeight: FontWeight.w500)),
                onTap: () {
                  Navigator.pop(context);
                  context.go(m.path);
                },
              ),
            const Divider(),
            ListTile(
              leading: Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Ds.dangerBg,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(Icons.logout_rounded, color: Ds.dangerText, size: 20),
              ),
              title: const Text('Déconnexion', style: TextStyle(color: Ds.dangerText, fontWeight: FontWeight.w600)),
              onTap: () async {
                Navigator.pop(context);
                await ref.read(authProvider.notifier).logout();
                if (context.mounted) context.go('/login');
              },
            ),
          ],
        ),
      ),
      body: child,
      extendBody: true,
      bottomNavigationBar: _FloatingNav(tabs: tabs, unread: unread),
    );
  }
}

/// Barre de navigation flottante signature.
class _FloatingNav extends StatelessWidget {
  final List<Map<String, Object>> tabs;
  final int unread;
  const _FloatingNav({required this.tabs, required this.unread});

  @override
  Widget build(BuildContext context) {
    final loc = GoRouterState.of(context).matchedLocation;
    var current = 0;
    for (var i = 0; i < tabs.length; i++) {
      if (loc == tabs[i]['path'] || loc.startsWith('${tabs[i]['path']}/')) {
        current = i;
      }
    }
    return SafeArea(
      child: Container(
        margin: const EdgeInsets.fromLTRB(18, 0, 18, 14),
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
        decoration: BoxDecoration(
          color: Ds.ink,
          borderRadius: BorderRadius.circular(26),
          boxShadow: [
            BoxShadow(color: Ds.ink.withValues(alpha: 0.4), blurRadius: 22, offset: const Offset(0, 8)),
          ],
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            for (var i = 0; i < tabs.length; i++)
              _NavItem(
                icon: tabs[i]['icon'] as IconData,
                label: tabs[i]['label'] as String,
                selected: i == current,
                badge: tabs[i]['path'] == '/notifications' ? unread : 0,
                onTap: () => context.go(tabs[i]['path'] as String),
              ),
          ],
        ),
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final bool selected;
  final int badge;
  final VoidCallback onTap;
  const _NavItem(
      {required this.icon,
      required this.label,
      required this.selected,
      required this.badge,
      required this.onTap});

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 220),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? Ds.pop : Colors.transparent,
          borderRadius: BorderRadius.circular(18),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Badge(
              isLabelVisible: badge > 0,
              label: Text('$badge'),
              backgroundColor: Ds.dangerText,
              child: Icon(icon, color: Colors.white, size: 22),
            ),
            Text(label,
                style: TextStyle(
                    color: selected ? Ds.ink : Colors.white70,
                    fontSize: 10.5,
                    fontWeight: FontWeight.w700)),
          ],
        ),
      ),
    );
  }
}
