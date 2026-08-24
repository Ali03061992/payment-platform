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

import { JwtInterceptor } from './core/jwt.interceptor';
import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';

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
      {
        path: 'admin/users',
        component: UserManagementComponent,
        canActivate: [RoleGuard],
        data: { roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'admin/users/create',
        component: CreateUserComponent,
        canActivate: [RoleGuard],
        data: { roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'sales/accounts',
        component: AccountActivationComponent,
        canActivate: [RoleGuard],
        data: { roles: ['SALES', 'SYSTEM_ADMIN'] }
      },
      {
        path: 'supplier/stock',
        component: StockManagementComponent,
        canActivate: [RoleGuard],
        data: { roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] }
      },
      {
        path: 'supplier/stock/create',
        component: AddProductComponent,
        canActivate: [RoleGuard],
        data: { roles: ['SUPPLIER_ADMIN'] }
      }
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
    AddProductComponent
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
