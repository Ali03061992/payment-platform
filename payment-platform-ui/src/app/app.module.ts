import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { RouterModule, Routes } from '@angular/router';
import { ServiceWorkerModule } from '@angular/service-worker';
import { QRCodeModule } from 'angularx-qrcode';
import { environment } from '../environments/environment';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { PasswordSetupComponent } from './password-setup/password-setup.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { LayoutComponent } from './layout/layout.component';
import { DashboardComponent } from './dashboard/dashboard.component';
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
import { FilterByRiskPipe } from './supplier/stock-optimization/filter-by-risk.pipe';
import { OrderManagementComponent } from './supplier/order-management/order-management.component';
import { DeliveryManagementComponent } from './supplier/delivery-management/delivery-management.component';
import { SupplierCreateOrderComponent } from './supplier/create-order/create-order.component';
import { OrderListComponent } from './shop/order-list/order-list.component';
import { CreateOrderComponent } from './shop/create-order/create-order.component';
import { ShopOrderDetailComponent } from './shop/order-detail/order-detail.component';
import { BalanceViewComponent } from './shop/balance-view/balance-view.component';
import { PwaUpdateComponent } from './pwa-update/pwa-update.component';
import { QrScannerComponent } from './qr-scanner/qr-scanner.component';
import { AgentPaymentsComponent } from './supplier/agent-payments/agent-payments.component';
import { ExportComponent } from './payments/export/export.component';
import { NotificationBannerComponent } from './components/notification-banner/notification-banner.component';
import { ToastComponent } from './components/toast/toast.component';
import { StatusLabelPipe } from './pipes/status-label.pipe';

import { JwtInterceptor } from './core/jwt.interceptor';
import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';

const adminRoles = ['SYSTEM_ADMIN'];
const supplierRoles = ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'];
const shopRoles = ['SHOP_ADMIN', 'SHOP_AGENT'];
const allRoles = [...adminRoles, ...supplierRoles, ...shopRoles];

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
      { path: 'payments', component: PaymentListComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'payments/create', component: CreatePaymentComponent, canActivate: [RoleGuard], data: { roles: [...adminRoles, ...shopRoles] } },
      { path: 'payments/stats', component: PaymentStatsComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'payments/:id', component: PaymentDetailComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'scan', component: QrScannerComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'export', component: ExportComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'supplier/agent-payments', component: AgentPaymentsComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/stock', component: StockManagementComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/optimization', component: StockOptimizationComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/stock/create', component: AddProductComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/categories', component: CategoryManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/families', component: FamilyManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/products', component: ProductManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/dashboard', component: StockDashboardComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/orders', component: OrderManagementComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/orders/create', component: SupplierCreateOrderComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } },
      { path: 'supplier/deliveries', component: DeliveryManagementComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_AGENT'] } },
      { path: 'shop/orders', component: OrderListComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/orders/create', component: CreateOrderComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/orders/:id', component: ShopOrderDetailComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'shop/balance', component: BalanceViewComponent, canActivate: [RoleGuard], data: { roles: shopRoles } },
      { path: 'change-password', component: ChangePasswordComponent }
    ]
  },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    PasswordSetupComponent,
    ChangePasswordComponent,
    LayoutComponent,
    DashboardComponent,
    UserManagementComponent,
    CreateUserComponent,
    AccountActivationComponent,
    StockManagementComponent,
    AddProductComponent,
    SupplierManagementComponent,
    ShopManagementComponent,
    RelationManagementComponent,
    OrganizationStatsComponent,
    PaymentListComponent,
    CreatePaymentComponent,
    PaymentDetailComponent,
    PaymentStatsComponent,
    ProductManagementComponent,
    CategoryManagementComponent,
    FamilyManagementComponent,
    StockDashboardComponent,
    StockOptimizationComponent,
    FilterByRiskPipe,
    OrderManagementComponent,
    DeliveryManagementComponent,
    SupplierCreateOrderComponent,
    OrderListComponent,
    CreateOrderComponent,
    ShopOrderDetailComponent,
    BalanceViewComponent,
    PwaUpdateComponent,
    QrScannerComponent,
    AgentPaymentsComponent,
    ExportComponent,
    NotificationBannerComponent,
    ToastComponent,
    StatusLabelPipe
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    QRCodeModule,
    RouterModule.forRoot(routes, { scrollPositionRestoration: 'top' }),
    ServiceWorkerModule.register('ngsw-worker.js', { enabled: environment.production })
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
