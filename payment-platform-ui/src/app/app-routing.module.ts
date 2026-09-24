import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';

import { LayoutComponent } from './layout/layout.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { PasswordSetupComponent } from './password-setup/password-setup.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { DashboardAdminComponent } from './dashboard/dashboard-admin/dashboard-admin.component';
import { DashboardSupplierComponent } from './dashboard/dashboard-supplier/dashboard-supplier.component';
import { DashboardShopComponent } from './dashboard/dashboard-shop/dashboard-shop.component';
import { DashboardAgentComponent } from './dashboard/dashboard-agent/dashboard-agent.component';
import { UserManagementComponent } from './admin/user-management/user-management.component';
import { CreateUserComponent } from './admin/create-user/create-user.component';
import { AccountActivationComponent } from './sales/account-activation/account-activation.component';
import { StockManagementComponent } from './supplier/stock-management/stock-management.component';
import { AddProductComponent } from './supplier/add-product/add-product.component';
import { SupplierManagementComponent } from './admin/supplier-management/supplier-management.component';
import { ShopManagementComponent } from './admin/shop-management/shop-management.component';
import { RelationManagementComponent } from './admin/relation-management/relation-management.component';
import { OrganizationStatsComponent } from './admin/organization-stats/organization-stats.component';
import { PaymentListComponent } from './payments/payment-list/payment-list.component';
import { CreatePaymentComponent } from './payments/create-payment/create-payment.component';
import { PaymentDetailComponent } from './payments/payment-detail/payment-detail.component';
import { PaymentStatsComponent } from './payments/payment-stats/payment-stats.component';
import { ProductManagementComponent } from './supplier/product-management/product-management.component';
import { CategoryManagementComponent } from './supplier/category-management/category-management.component';
import { FamilyManagementComponent } from './supplier/family-management/family-management.component';
import { StockDashboardComponent } from './supplier/stock-dashboard/stock-dashboard.component';
import { StockOptimizationComponent } from './supplier/stock-optimization/stock-optimization.component';
import { OrderManagementComponent } from './supplier/order-management/order-management.component';
import { DeliveryManagementComponent } from './supplier/delivery-management/delivery-management.component';
import { SupplierCreateOrderComponent } from './supplier/create-order/create-order.component';
import { OrderListComponent } from './shop/order-list/order-list.component';
import { CreateOrderComponent } from './shop/create-order/create-order.component';
import { ShopOrderDetailComponent } from './shop/order-detail/order-detail.component';
import { DisputeDetailComponent } from './shop/dispute-detail/dispute-detail.component';
import { BalanceViewComponent } from './shop/balance-view/balance-view.component';
import { QrScannerComponent } from './qr-scanner/qr-scanner.component';
import { AgentPaymentsComponent } from './supplier/agent-payments/agent-payments.component';
import { SupplierFinancialComponent } from './supplier/supplier-financial/supplier-financial.component';
import { SupplierBalanceComponent } from './supplier/supplier-balance/supplier-balance.component';
import { ExportComponent } from './payments/export/export.component';
import { LowStockAlertsComponent } from './supplier/low-stock-alerts/low-stock-alerts.component';
import { NotificationsComponent } from './dashboard/notifications/notifications.component';
import { AuditLogManagementComponent } from './admin/audit-log-management/audit-log-management.component';

export const adminRoles = ['SYSTEM_ADMIN'];
export const supplierRoles = ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'];
export const shopRoles = ['SHOP_ADMIN', 'SHOP_AGENT'];
export const allRoles = [...adminRoles, ...supplierRoles, ...shopRoles];

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'setup-password', component: PasswordSetupComponent },
  {
    path: 'dashboard',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', component: DashboardComponent },
      { path: 'admin/users', component: UserManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/users/create', component: CreateUserComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'sales/accounts', component: AccountActivationComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/suppliers', component: SupplierManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/shops', component: ShopManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/relations', component: RelationManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/org-stats', component: OrganizationStatsComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/audit-logs', component: AuditLogManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'payments', component: PaymentListComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'payments/create', component: CreatePaymentComponent, canActivate: [RoleGuard], data: { roles: [...adminRoles, ...shopRoles] } },
      { path: 'payments/stats', component: PaymentStatsComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'payments/:id', component: PaymentDetailComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'scan', component: QrScannerComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'export', component: ExportComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'supplier/agent-payments', component: AgentPaymentsComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/financial', component: SupplierFinancialComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/balance', component: SupplierBalanceComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/stock', component: StockManagementComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/optimization', component: StockOptimizationComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/stock/create', component: AddProductComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/categories', component: CategoryManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/families', component: FamilyManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/products', component: ProductManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/dashboard', component: StockDashboardComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/low-stock-alerts', component: LowStockAlertsComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/orders', component: OrderManagementComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/orders/create', component: SupplierCreateOrderComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/deliveries', component: DeliveryManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_AGENT'] } },
      { path: 'shop/orders', component: OrderListComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/orders/create', component: CreateOrderComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/orders/:id', component: ShopOrderDetailComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/disputes/:id', component: DisputeDetailComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/balance', component: BalanceViewComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'notifications', component: NotificationsComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'change-password', component: ChangePasswordComponent }
    ]
  },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'top' })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
