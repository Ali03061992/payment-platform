import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { RouterModule, Routes } from '@angular/router';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { PasswordSetupComponent } from './password-setup/password-setup.component';
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

import { JwtInterceptor } from './core/jwt.interceptor';
import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';

const adminRoles = ['SYSTEM_ADMIN'];
const supplierRoles = ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'];
const shopRoles = ['SHOP_ADMIN', 'SHOP_AGENT'];
const allRoles = [...adminRoles, ...supplierRoles, ...shopRoles, 'SALES'];

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
      { path: 'sales/accounts', component: AccountActivationComponent, canActivate: [RoleGuard], data: { roles: ['SALES', ...adminRoles] } },
      { path: 'admin/suppliers', component: SupplierManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/shops', component: ShopManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/relations', component: RelationManagementComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'admin/org-stats', component: OrganizationStatsComponent, canActivate: [RoleGuard], data: { roles: adminRoles } },
      { path: 'payments', component: PaymentListComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'payments/create', component: CreatePaymentComponent, canActivate: [RoleGuard], data: { roles: [...adminRoles, ...shopRoles] } },
      { path: 'payments/stats', component: PaymentStatsComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'payments/:id', component: PaymentDetailComponent, canActivate: [RoleGuard], data: { roles: allRoles } },
      { path: 'supplier/stock', component: StockManagementComponent, canActivate: [RoleGuard], data: { roles: supplierRoles } },
      { path: 'supplier/stock/create', component: AddProductComponent, canActivate: [RoleGuard], data: { roles: ['SUPPLIER_ADMIN'] } }
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
    PaymentStatsComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    RouterModule.forRoot(routes)
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
