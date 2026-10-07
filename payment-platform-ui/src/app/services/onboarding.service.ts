import { Injectable } from '@angular/core';
import { LoginService } from './login.service';
import { TranslateService } from '@ngx-translate/core';
import { UserPreferencesService } from './user-preferences.service';

export interface TourStep {
  title: string;
  description: string;
  icon: string;
  targetSelector?: string;
}

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly STORAGE_KEY = 'onboarding_completed';

  constructor(private loginService: LoginService, private translate: TranslateService, private prefs: UserPreferencesService) {}

  isOnboardingCompleted(): boolean {
    try {
      const user = this.loginService.getCurrentUser();
      if ((user as any)?.tourSeen === true) return true;
      if (!user) return true;
      const flag = localStorage.getItem(this.STORAGE_KEY);
      return flag === `user_${user.id}`;
    } catch {
      return false;
    }
  }

  completeOnboarding(): void {
    let user: any = null;
    try {
      user = this.loginService.getCurrentUser();
    } catch {}
    const flag = user ? `user_${user.id}` : 'true';
    try {
      localStorage.setItem(this.STORAGE_KEY, flag);
    } catch {}
    try {
      this.prefs.update({ tourSeen: true }).subscribe({ error: () => {} });
    } catch {}
    try {
      const svc = this.loginService as any;
      const patch = { tourSeen: true };
      if (typeof svc.updateCurrentUser === 'function') {
        svc.updateCurrentUser(patch);
      } else if (typeof svc.setCurrentUser === 'function') {
        const current = typeof svc.getCurrentUser === 'function' ? svc.getCurrentUser() : user;
        svc.setCurrentUser({ ...(current || {}), ...patch });
      } else if (typeof svc.updateCachedUser === 'function') {
        svc.updateCachedUser(patch);
      } else if (typeof svc.saveUser === 'function') {
        const current = typeof svc.getCurrentUser === 'function' ? svc.getCurrentUser() : user;
        svc.saveUser({ ...(current || {}), ...patch });
      } else if (user) {
        const merged = { ...user, ...patch };
        try {
          sessionStorage.setItem('user', JSON.stringify(merged));
        } catch {}
      }
    } catch {}
  }

  resetOnboarding(): void {
    try {
      localStorage.removeItem(this.STORAGE_KEY);
    } catch {}
  }

  getTourSteps(): TourStep[] {
    const user = this.loginService.getCurrentUser();
    const roles = user?.roles || [];

    if (roles.includes('SYSTEM_ADMIN')) {
      return this.getAdminSteps();
    }
    if (roles.includes('SUPPLIER_ADMIN') || roles.includes('SUPPLIER_AGENT')) {
      return this.getSupplierSteps();
    }
    if (roles.includes('SHOP_ADMIN') || roles.includes('SHOP_AGENT')) {
      return this.getShopSteps();
    }
    return this.getDefaultSteps();
  }

  private getAdminSteps(): TourStep[] {
    return [
      {
        title: this.translate.instant('TOUR.STEP_WELCOME_TITLE'),
        description: this.translate.instant('TOUR.STEP_ADMIN_DASH_DESC'),
        icon: 'sparkles',
      },
      {
        title: this.translate.instant('TOUR.STEP_ADMIN_USERS_TITLE'),
        description: this.translate.instant('TOUR.STEP_ADMIN_USERS_DESC'),
        icon: 'users',
        targetSelector: '.sidebar-nav',
      },
      {
        title: this.translate.instant('TOUR.STEP_ADMIN_ORGS_TITLE'),
        description: this.translate.instant('TOUR.STEP_ADMIN_ORGS_DESC'),
        icon: 'factory',
        targetSelector: '.content-area',
      },
      {
        title: this.translate.instant('TOUR.STEP_ADMIN_PAY_TITLE'),
        description: this.translate.instant('TOUR.STEP_ADMIN_PAY_DESC'),
        icon: 'card',
        targetSelector: '.header-actions',
      },
    ];
  }

  private getSupplierSteps(): TourStep[] {
    return [
      {
        title: this.translate.instant('TOUR.STEP_WELCOME_TITLE'),
        description: this.translate.instant('TOUR.STEP_WELCOME_SUP_DESC'),
        icon: 'sparkles',
      },
      {
        title: this.translate.instant('TOUR.STEP_SUP_STOCK_TITLE'),
        description: this.translate.instant('TOUR.STEP_SUP_STOCK_DESC'),
        icon: 'box',
        targetSelector: '.sidebar-nav',
      },
      {
        title: this.translate.instant('TOUR.STEP_SUP_OPTIM_TITLE'),
        description: this.translate.instant('TOUR.STEP_SUP_OPTIM_DESC'),
        icon: 'cpu',
        targetSelector: '.content-area',
      },
      {
        title: this.translate.instant('TOUR.STEP_SUP_ORDERS_TITLE'),
        description: this.translate.instant('TOUR.STEP_SUP_ORDERS_DESC'),
        icon: 'cart',
        targetSelector: '.header-actions',
      },
    ];
  }

  private getShopSteps(): TourStep[] {
    return [
      {
        title: this.translate.instant('TOUR.STEP_WELCOME_TITLE'),
        description: this.translate.instant('TOUR.STEP_WELCOME_SHOP_DESC'),
        icon: 'sparkles',
      },
      {
        title: this.translate.instant('TOUR.STEP_SHOP_ORDERS_TITLE'),
        description: this.translate.instant('TOUR.STEP_SHOP_ORDERS_DESC'),
        icon: 'cart',
        targetSelector: '.sidebar-nav',
      },
      {
        title: this.translate.instant('TOUR.STEP_SHOP_BALANCE_TITLE'),
        description: this.translate.instant('TOUR.STEP_SHOP_BALANCE_DESC'),
        icon: 'scale',
        targetSelector: '.content-area',
      },
      {
        title: this.translate.instant('TOUR.STEP_SHOP_QR_TITLE'),
        description: this.translate.instant('TOUR.STEP_SHOP_QR_DESC'),
        icon: 'scan',
        targetSelector: '.header-actions',
      },
    ];
  }

  private getDefaultSteps(): TourStep[] {
    return [
      {
        title: this.translate.instant('TOUR.STEP_WELCOME_TITLE'),
        description: this.translate.instant('TOUR.STEP_WELCOME_DEF_DESC'),
        icon: 'sparkles',
      },
      {
        title: this.translate.instant('TOUR.STEP_DEF_PAY_TITLE'),
        description: this.translate.instant('TOUR.STEP_DEF_PAY_DESC'),
        icon: 'card',
        targetSelector: '.sidebar-nav',
      },
      {
        title: this.translate.instant('TOUR.STEP_DEF_QR_TITLE'),
        description: this.translate.instant('TOUR.STEP_DEF_QR_DESC'),
        icon: 'scan',
        targetSelector: '.header-actions',
      },
    ];
  }
}
