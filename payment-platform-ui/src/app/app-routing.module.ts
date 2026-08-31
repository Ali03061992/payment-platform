import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './core/auth.guard';
import { RoleGuard } from './core/role.guard';

const adminRoles = ['SYSTEM_ADMIN'];
const supplierRoles = ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'];
const shopRoles = ['SHOP_ADMIN', 'SHOP_AGENT'];
const allRoles = [...adminRoles, ...supplierRoles, ...shopRoles];

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', loadChildren: () => import('./login/login.module').then(m => m.LoginModule) },
  { path: 'register', loadChildren: () => import('./register/register.module').then(m => m.RegisterModule) },
  { path: 'setup-password', loadChildren: () => import('./password-setup/password-setup.module').then(m => m.PasswordSetupModule) },
  {
    path: 'dashboard',
    loadChildren: () => import('./dashboard-layout/dashboard-layout.module').then(m => m.DashboardLayoutModule),
    canActivate: [AuthGuard]
  },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'top' })],
  exports: [RouterModule]
})
export class AppRoutingModule { }
