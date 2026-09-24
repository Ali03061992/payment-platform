import { Injectable } from '@angular/core';
import { LoginService } from './login.service';

export interface TourStep {
  title: string;
  description: string;
  icon: string;
  targetSelector?: string;
}

@Injectable({ providedIn: 'root' })
export class OnboardingService {
  private readonly STORAGE_KEY = 'onboarding_completed';

  constructor(private loginService: LoginService) {}

  isOnboardingCompleted(): boolean {
    const flag = localStorage.getItem(this.STORAGE_KEY);
    if (flag === 'true') {
      return true;
    }
    const user = this.loginService.getCurrentUser();
    if (!user) return true;
    return flag === `user_${user.id}`;
  }

  completeOnboarding(): void {
    const user = this.loginService.getCurrentUser();
    const flag = user ? `user_${user.id}` : 'true';
    localStorage.setItem(this.STORAGE_KEY, flag);
  }

  resetOnboarding(): void {
    localStorage.removeItem(this.STORAGE_KEY);
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
        title: 'Bienvenue sur Payment Platform',
        description: 'Cette plateforme vous permet de gérer les utilisateurs, fournisseurs, boutiques et paiements.',
        icon: '👋',
      },
      {
        title: 'Tableau de bord',
        description: 'Consultez les statistiques globales : nombre d\'utilisateurs, comptes actifs et fournisseurs enregistrés.',
        icon: '📊',
        targetSelector: '.stats-grid',
      },
      {
        title: 'Gestion des utilisateurs',
        description: 'Créez, activez ou désactivez les comptes utilisateurs depuis le menu latéral.',
        icon: '👥',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Fournisseurs & Boutiques',
        description: 'Gérez les relations fournisseur-boutique et suivez les statistiques d\'organisations.',
        icon: '🏭',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Paiements & QR Scanner',
        description: 'Suivez tous les paiements et scannez les QR codes directement depuis l\'application.',
        icon: '💳',
        targetSelector: 'nav.sidebar-nav',
      },
    ];
  }

  private getSupplierSteps(): TourStep[] {
    return [
      {
        title: 'Bienvenue sur Payment Platform',
        description: 'En tant que fournisseur, vous pouvez gérer votre stock, produits et suivre vos commandes.',
        icon: '👋',
      },
      {
        title: 'Gestion du stock',
        description: 'Ajoutez et gérez vos produits, catégories et familles depuis le menu latéral.',
        icon: '📦',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Optimisation',
        description: 'Utilisez l\'intelligence artificielle pour optimiser votre gestion de stock.',
        icon: '🧠',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Commandes & Livraisons',
        description: 'Suivez les commandes passées par les boutiques et gérez vos livraisons.',
        icon: '🛒',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Paiements',
        description: 'Consultez l\'historique des paiements et les paiements de vos agents.',
        icon: '💳',
        targetSelector: 'nav.sidebar-nav',
      },
    ];
  }

  private getShopSteps(): TourStep[] {
    return [
      {
        title: 'Bienvenue sur Payment Platform',
        description: 'En tant que boutique, vous pouvez passer des commandes et suivre vos paiements.',
        icon: '👋',
      },
      {
        title: 'Mes commandes',
        description: 'Consultez et créez de nouvelles commandes auprès de vos fournisseurs.',
        icon: '🛒',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Balance',
        description: 'Suivez votre solde et l\'historique de vos transactions.',
        icon: '⚖️',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Paiements',
        description: 'Consultez vos paiements et génerez des exports.',
        icon: '💳',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Scanner QR',
        description: 'Scanpez les QR codes pour effectuer des paiements rapidement.',
        icon: '📱',
        targetSelector: 'nav.sidebar-nav',
      },
    ];
  }

  private getDefaultSteps(): TourStep[] {
    return [
      {
        title: 'Bienvenue sur Payment Platform',
        description: 'Explorez les fonctionnalités disponibles pour votre rôle.',
        icon: '👋',
      },
      {
        title: 'Paiements',
        description: 'Consultez et gérez vos paiements depuis le menu latéral.',
        icon: '💳',
        targetSelector: 'nav.sidebar-nav',
      },
      {
        title: 'Scanner QR',
        description: 'Utilisez le scanner QR pour effectuer des paiements.',
        icon: '📱',
        targetSelector: 'nav.sidebar-nav',
      },
    ];
  }
}
