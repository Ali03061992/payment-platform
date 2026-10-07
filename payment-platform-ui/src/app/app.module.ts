import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { ServiceWorkerModule } from '@angular/service-worker';
import { QRCodeComponent } from 'angularx-qrcode';
import { environment } from '../environments/environment';

import { AppComponent } from './app.component';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { PasswordSetupComponent } from './password-setup/password-setup.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './reset-password/reset-password.component';
import { ChangePasswordComponent } from './change-password/change-password.component';
import { LayoutComponent } from './layout/layout.component';
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
import { FilterByRiskPipe } from './supplier/stock-optimization/filter-by-risk.pipe';
import { OrderManagementComponent } from './supplier/order-management/order-management.component';
import { DeliveryManagementComponent } from './supplier/delivery-management/delivery-management.component';
import { SupplierCreateOrderComponent } from './supplier/create-order/create-order.component';
import { OrderListComponent } from './shop/order-list/order-list.component';
import { CreateOrderComponent } from './shop/create-order/create-order.component';
import { ShopOrderDetailComponent } from './shop/order-detail/order-detail.component';
import { DisputeDetailComponent } from './shop/dispute-detail/dispute-detail.component';
import { DisputeListComponent } from './shop/dispute-list/dispute-list.component';
import { ChatbotComponent } from './components/chatbot/chatbot.component';
import { BalanceViewComponent } from './shop/balance-view/balance-view.component';
import { PwaUpdateComponent } from './pwa-update/pwa-update.component';
import { QrScannerComponent } from './qr-scanner/qr-scanner.component';
import { AgentPaymentsComponent } from './supplier/agent-payments/agent-payments.component';
import { SupplierFinancialComponent } from './supplier/supplier-financial/supplier-financial.component';
import { SupplierBalanceComponent } from './supplier/supplier-balance/supplier-balance.component';
import { ExportComponent } from './payments/export/export.component';
import { LowStockAlertsComponent } from './supplier/low-stock-alerts/low-stock-alerts.component';
import { NotificationsComponent } from './dashboard/notifications/notifications.component';
import { AuditLogManagementComponent } from './admin/audit-log-management/audit-log-management.component';
import { ForbiddenComponent } from './errors/forbidden/forbidden.component';
import { NotFoundComponent } from './errors/not-found/not-found.component';
import { NotificationBannerComponent } from './components/notification-banner/notification-banner.component';
import { ToastComponent } from './components/toast/toast.component';
import { TourComponent } from './components/tour/tour.component';
import { AppIconComponent } from './components/icon/icon.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { PaginationComponent } from './components/pagination/pagination.component';
import { StatusLabelPipe } from './pipes/status-label.pipe';
import { TimeAgoPipe } from './pipes/time-ago.pipe';

import { JwtInterceptor } from './core/jwt.interceptor';
import { AppRoutingModule } from './app-routing.module';
import { AppTranslateModule } from './i18n/app-translate.module';
import { LanguageSwitcherComponent } from './i18n/language-switcher.component';
import { APP_INITIALIZER } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LoginService } from './services/login.service';
import { ThemePickerComponent } from './components/theme-picker/theme-picker.component';

/**
 * Précharge les traductions avant le bootstrap : les appels synchrones
 * TranslateService.instant() (chatbot, toasts, libellés TS) renverraient
 * sinon la clé brute tant que les JSON ./assets/i18n ne sont pas chargés.
 */
export function initAppTranslations(translate: TranslateService, loginService: LoginService): () => Promise<void> {
  return () => new Promise<void>((resolve) => {
    const raw = localStorage.getItem('lang') || 'fr';
    let saved: 'fr' | 'en' = raw === 'en' ? 'en' : 'fr';
    if (raw === 'ar') {
      try { localStorage.setItem('lang', 'fr'); } catch {}
      saved = 'fr';
    }
    try {
      const cached = loginService.getCurrentUser();
      if (cached?.id !== undefined && cached?.id !== null) {
        const perUser = localStorage.getItem(`pp-lang-${cached.id}`);
        if (perUser === 'en' || perUser === 'fr') saved = perUser;
        else if (perUser === 'ar') {
          try { localStorage.setItem(`pp-lang-${cached.id}`, 'fr'); } catch {}
          saved = 'fr';
        }
      }
      if (cached?.preferredLang === 'en' || cached?.preferredLang === 'fr') saved = cached.preferredLang;
    } catch {}
    translate.setDefaultLang('fr');
    translate.use(saved).subscribe({
      next: () => {
        try {
          document.documentElement.lang = saved;
          localStorage.setItem('lang', saved);
        } catch {}
        resolve();
      },
      error: () => resolve()
    });
  });
}

@NgModule({ declarations: [
        AppComponent,
        LoginComponent,
        RegisterComponent,
        PasswordSetupComponent,
        ForgotPasswordComponent,
        ResetPasswordComponent,
        ChangePasswordComponent,
        LayoutComponent,
        DashboardComponent,
        DashboardAdminComponent,
        DashboardSupplierComponent,
        DashboardShopComponent,
        DashboardAgentComponent,
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
        DisputeDetailComponent,
        DisputeListComponent,
        ChatbotComponent,
        BalanceViewComponent,
        PwaUpdateComponent,
        QrScannerComponent,
        AgentPaymentsComponent,
        SupplierFinancialComponent,
        SupplierBalanceComponent,
        ExportComponent,
        LowStockAlertsComponent,
        NotificationsComponent,
        NotificationBannerComponent,
        AuditLogManagementComponent,
        ForbiddenComponent,
        NotFoundComponent,
        ToastComponent,
        TourComponent,
        AppIconComponent,
        ConfirmDialogComponent,
        PaginationComponent,
        StatusLabelPipe,
        TimeAgoPipe,
        LanguageSwitcherComponent,
        ThemePickerComponent
    ],
    bootstrap: [AppComponent], imports: [BrowserModule,
        FormsModule,
        QRCodeComponent,
        AppRoutingModule,
        AppTranslateModule,
        ServiceWorkerModule.register('ngsw-worker.js', { enabled: environment.production })
    ], providers: [
        { provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true },
        provideHttpClient(withInterceptorsFromDi()),
        { provide: APP_INITIALIZER, useFactory: initAppTranslations, deps: [TranslateService, LoginService], multi: true }
    ] })
export class AppModule { }
