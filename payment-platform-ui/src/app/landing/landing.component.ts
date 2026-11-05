import { AfterViewInit, Component, HostListener, OnDestroy } from '@angular/core';
import { BRAND } from '../core/brand.config';

interface GalleryScreen {
  src: string;
  labelKey: string;
}

@Component({
  selector: 'app-landing',
  templateUrl: './landing.component.html',
  styleUrls: ['./landing.component.css'],
  standalone: false
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  readonly brand = BRAND;
  readonly year = new Date().getFullYear();
  readonly phoneDisplay = '+216 24 176 545';
  readonly phoneLink = 'tel:+21624176545';
  readonly contactEmailLink =
    'mailto:ali.ben.amor.1992@hotmail.com?subject=' +
    encodeURIComponent("Demande d'information - NEXTERA");
  readonly whatsappLink =
    'https://wa.me/21624176545?text=' +
    encodeURIComponent('Bonjour, je souhaite des informations sur NEXTERA.');

  readonly galleryScreens: GalleryScreen[] = [
    { src: 'assets/marketing/screens/01-login.png', labelKey: 'LANDING.GALLERY_LOGIN' },
    { src: 'assets/marketing/screens/02-admin-dashboard.png', labelKey: 'LANDING.PREVIEW_ADMIN_TITLE' },
    { src: 'assets/marketing/screens/03-admin-organisations.png', labelKey: 'LANDING.GALLERY_ORGANISATIONS' },
    { src: 'assets/marketing/screens/04-paiements.png', labelKey: 'NAV.PAYMENTS' },
    { src: 'assets/marketing/screens/05-notifications.png', labelKey: 'LAYOUT.NOTIFICATIONS' },
    { src: 'assets/marketing/screens/06-securite-passkeys.png', labelKey: 'NAV.SECURITY' },
    { src: 'assets/marketing/screens/07-mobile-dashboard.png', labelKey: 'LANDING.GALLERY_MOBILE_DASHBOARD' },
    { src: 'assets/marketing/screens/08-fournisseur-dashboard.png', labelKey: 'LANDING.GALLERY_SUPPLIER_DASHBOARD' },
    { src: 'assets/marketing/screens/09-fournisseur-produits.png', labelKey: 'NAV.PRODUCTS' },
    { src: 'assets/marketing/screens/10-fournisseur-commandes.png', labelKey: 'LANDING.PREVIEW_SUPPLIER_TITLE' },
    { src: 'assets/marketing/screens/11-journal-factures.png', labelKey: 'NAV.INVOICE_JOURNAL' },
    { src: 'assets/marketing/screens/12-fournisseur-finances.png', labelKey: 'NAV.FINANCE' },
    { src: 'assets/marketing/screens/13-fournisseur-livraisons.png', labelKey: 'NAV.DELIVERIES' },
    { src: 'assets/marketing/screens/14-boutique-dashboard.png', labelKey: 'LANDING.GALLERY_SHOP_DASHBOARD' },
    { src: 'assets/marketing/screens/15-boutique-commandes.png', labelKey: 'NAV.ORDERS' },
    { src: 'assets/marketing/screens/16-nouvelle-commande.png', labelKey: 'NAV.NEW_ORDER' },
    { src: 'assets/marketing/screens/17-reclamations.png', labelKey: 'LANDING.PREVIEW_SHOP_TITLE' },
    { src: 'assets/marketing/screens/18-devis-boutique.png', labelKey: 'NAV.QUOTES' },
    { src: 'assets/marketing/screens/19-mobile-reclamations.png', labelKey: 'LANDING.GALLERY_MOBILE_RECLAMATIONS' }
  ];

  readonly galleryRowOne = this.galleryScreens.filter((_, index) => index % 2 === 0);
  readonly galleryRowTwo = this.galleryScreens.filter((_, index) => index % 2 === 1);

  readonly navSections = ['plateforme', 'conformite', 'solutions', 'contact'];

  menuOpen = false;
  activeSection = '';

  private scrollFrame = 0;

  ngAfterViewInit(): void {
    const initialSection = window.location.hash.replace('#', '');
    if (initialSection && document.getElementById(initialSection)) {
      window.setTimeout(() => this.scrollToSection(null, initialSection), 0);
    }
    this.recomputeActiveSection();
  }

  ngOnDestroy(): void {
    if (this.scrollFrame) {
      window.cancelAnimationFrame(this.scrollFrame);
    }
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  scrollToSection(event: Event | null, sectionId: string): void {
    event?.preventDefault();
    const target = document.getElementById(sectionId);
    if (!target) {
      return;
    }

    this.menuOpen = false;
    const header = document.querySelector<HTMLElement>('.site-header');
    const offset = (header?.offsetHeight ?? 72) + 14;
    const top = Math.max(0, window.scrollY + target.getBoundingClientRect().top - offset);
    const behavior: ScrollBehavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      ? 'auto'
      : 'smooth';

    window.scrollTo({ top, behavior });
    if (window.history?.replaceState) {
      window.history.replaceState(null, '', `#${sectionId}`);
    }
    this.activeSection = sectionId;
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (this.scrollFrame) {
      return;
    }
    this.scrollFrame = window.requestAnimationFrame(() => {
      this.scrollFrame = 0;
      this.recomputeActiveSection();
    });
  }

  @HostListener('window:hashchange')
  onHashChange(): void {
    const sectionId = window.location.hash.replace('#', '');
    if (sectionId) {
      this.scrollToSection(null, sectionId);
    }
  }

  @HostListener('window:resize')
  onWindowResize(): void {
    if (window.innerWidth > 1020) {
      this.menuOpen = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.menuOpen = false;
  }

  private recomputeActiveSection(): void {
    const header = document.querySelector<HTMLElement>('.site-header');
    const offset = (header?.offsetHeight ?? 72) + 56;
    let current = '';

    for (const sectionId of this.navSections) {
      const section = document.getElementById(sectionId);
      if (section && section.getBoundingClientRect().top - offset <= 0) {
        current = sectionId;
      }
    }

    const reachedBottom = window.scrollY + window.innerHeight >= document.documentElement.scrollHeight - 4;
    this.activeSection = reachedBottom ? 'contact' : current;
  }
}
