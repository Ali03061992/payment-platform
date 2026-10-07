import { Component, ElementRef, HostListener } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import { LoginService } from '../services/login.service';
import { UserPreferencesService } from '../services/user-preferences.service';

export const APP_LANGS = ['fr', 'en'] as const;
export type AppLang = (typeof APP_LANGS)[number];

@Component({
  selector: 'app-language-switcher',
  template: `
    <div class="lang-premium" #root>
      <button
        type="button"
        class="lang-trigger"
        (click)="toggle()"
        [attr.aria-expanded]="open"
        aria-haspopup="listbox"
        [attr.aria-label]="currentLang === 'fr' ? 'Langue : Francais' : 'Language: English'">
        <span class="lang-flag" aria-hidden="true" [innerHTML]="flagSvg(currentLang)"></span>
        <span class="lang-code">{{ currentLang.toUpperCase() }}</span>
        <svg class="lang-chevron" [class.open]="open" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="6 9 12 15 18 9"/></svg>
      </button>
      @if (open) {
        <div class="lang-menu" role="listbox" [attr.aria-label]="currentLang === 'fr' ? 'Choisir la langue' : 'Choose language'">
          @for (lang of langs; track lang) {
            <button
              type="button"
              role="option"
              class="lang-option"
              [class.active]="currentLang === lang"
              [attr.aria-selected]="currentLang === lang"
              (click)="switchLang(lang)">
              <span class="lang-flag" aria-hidden="true" [innerHTML]="flagSvg(lang)"></span>
              <span class="lang-name">{{ langLabel(lang) }}</span>
              @if (currentLang === lang) {
                <svg class="lang-check" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><polyline points="20 6 9 17 4 12"/></svg>
              }
            </button>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: inline-flex; justify-content: flex-end; margin-left: auto; }
    .lang-premium { position: relative; display: inline-flex; justify-content: flex-end; margin-left: auto; }
    .lang-trigger {
      display: inline-flex;
      align-items: center;
      gap: 7px;
      min-height: 32px;
      padding: 4px 6px 4px 8px;
      border-radius: 999px;
      background: color-mix(in srgb, var(--card-bg) 82%, transparent);
      backdrop-filter: blur(10px);
      -webkit-backdrop-filter: blur(10px);
      border: 1px solid var(--border-color);
      border-bottom: 2px solid transparent;
      border-image: none;
      color: var(--text-muted);
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 1.2px;
      text-transform: uppercase;
      cursor: pointer;
      box-shadow: none;
      transition: color 0.18s, border-color 0.18s, background 0.18s;
      position: relative;
    }
    .lang-trigger::after {
      content: "";
      position: absolute;
      left: 10px;
      right: 10px;
      bottom: -1px;
      height: 2px;
      border-radius: 2px;
      background: var(--grad-theme);
      opacity: 0.65;
    }
    .lang-trigger:hover { color: var(--text-primary); border-color: var(--sky-border); background: var(--sky-soft); }
    .lang-trigger:active { transform: translateY(0.5px); }
    .lang-trigger:focus-visible { outline: 2px solid var(--sky); outline-offset: 2px; }
    .lang-code { min-width: 20px; font-size: 11px; }
    .lang-chevron { color: var(--text-muted); transition: transform 0.18s; opacity: 0.8; }
    .lang-chevron.open { transform: rotate(180deg); color: var(--sky-dark); }
    .lang-flag { display: inline-flex; width: 22px; height: 15px; border-radius: 3px; overflow: hidden; box-shadow: 0 1px 2px rgba(10,10,15,0.3), inset 0 1px 1px rgba(255,255,255,0.45); }
    .lang-flag svg { display: block; width: 22px; height: 15px; }
    .lang-menu {
      position: absolute;
      right: 0;
      top: calc(100% + 10px);
      min-width: 178px;
      padding: 4px;
      border-radius: 12px;
      background: color-mix(in srgb, var(--card-bg) 92%, transparent);
      backdrop-filter: blur(14px);
      -webkit-backdrop-filter: blur(14px);
      border: 1px solid var(--border-color);
      box-shadow: var(--shadow-md);
      z-index: 1200;
      animation: langIn 0.16s ease-out;
    }
    @keyframes langIn { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: none; } }
    .lang-option {
      display: flex;
      align-items: center;
      gap: 9px;
      width: 100%;
      min-height: 38px;
      padding: 6px 9px;
      border: 0;
      border-left: 2px solid transparent;
      border-radius: 8px;
      background: transparent;
      color: var(--text-secondary);
      font-size: 12px;
      font-weight: 600;
      letter-spacing: 0.2px;
      cursor: pointer;
      transition: background 0.14s, border-color 0.14s, color 0.14s;
    }
    .lang-option:hover { background: var(--sky-soft); color: var(--text-primary); }
    .lang-option.active { background: linear-gradient(90deg, var(--sky-soft), transparent 75%); border-left-color: var(--sky-dark); color: var(--text-primary); }
    .lang-name { flex: 1; text-align: right; font-size: 12px; }
    .lang-check { color: var(--sky-dark); }
    [data-theme="dark"] .lang-check { color: var(--sky); }
    [data-theme="dark"] .lang-trigger { background: rgba(26, 26, 46, 0.7); }
    @media (prefers-reduced-motion: reduce) { .lang-menu { animation: none; } .lang-chevron { transition: none; } }
  `],
  standalone: false
})
export class LanguageSwitcherComponent {
  readonly langs: AppLang[] = [...APP_LANGS];
  currentLang: AppLang;
  open = false;

  constructor(private translate: TranslateService, private host: ElementRef, private sanitizer: DomSanitizer, private loginService: LoginService, private prefs: UserPreferencesService) {
    this.currentLang = this.resolveInitialLang();
    this.translate.addLangs([...APP_LANGS]);
    this.translate.setDefaultLang('fr');
    this.applyLang(this.currentLang);
  }

  toggle(): void {
    this.open = !this.open;
  }

  switchLang(lang: AppLang): void {
    this.currentLang = lang;
    try {
      localStorage.setItem('lang', lang);
    } catch {}
    try {
      const user = this.loginService.getCurrentUser();
      if (user?.id !== undefined && user?.id !== null) localStorage.setItem(`pp-lang-${user.id}`, lang);
    } catch {}
    this.applyLang(lang);
    this.open = false;
    try {
      this.prefs.update({ preferredLang: lang }).subscribe({ error: () => {} });
    } catch {}
  }

  langLabel(lang: AppLang): string {
    return lang === 'fr' ? 'Francais' : 'English';
  }

  flagSvg(lang: AppLang): SafeHtml {
    if (lang === 'fr') {
      return this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 26 18" aria-hidden="true"><defs><linearGradient id="frGloss" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity="0.55"/><stop offset="0.35" stop-color="#fff" stop-opacity="0.08"/><stop offset="0.7" stop-color="#000" stop-opacity="0.12"/><stop offset="1" stop-color="#000" stop-opacity="0.22"/></linearGradient></defs><rect width="26" height="18" rx="3" fill="#f8fafc"/><rect width="9" height="18" fill="#0055A4"/><rect x="9" width="8" height="18" fill="#ffffff"/><rect x="17" width="9" height="18" fill="#EF4135"/><path d="M0 9 Q6.5 6.5 13 9 T26 9" stroke="#000" stroke-opacity="0.08" stroke-width="1.4" fill="none"/><rect width="26" height="18" rx="3" fill="url(#frGloss)"/></svg>`);
    }
    return this.sanitizer.bypassSecurityTrustHtml(`<svg viewBox="0 0 26 18" aria-hidden="true"><defs><linearGradient id="enGloss" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity="0.5"/><stop offset="0.4" stop-color="#fff" stop-opacity="0.06"/><stop offset="0.75" stop-color="#000" stop-opacity="0.14"/><stop offset="1" stop-color="#000" stop-opacity="0.25"/></linearGradient></defs><rect width="26" height="18" rx="3" fill="#012169"/><path d="M0 0 L26 18 M26 0 L0 18" stroke="#fff" stroke-width="3.4"/><path d="M0 0 L26 18 M26 0 L0 18" stroke="#C8102E" stroke-width="1.4"/><path d="M13 0 V18 M0 9 H26" stroke="#fff" stroke-width="5.2"/><path d="M13 0 V18 M0 9 H26" stroke="#C8102E" stroke-width="3"/><rect width="26" height="18" rx="3" fill="url(#enGloss)"/></svg>`);
  }

  @HostListener('document:click', ['$event'])
  onOutside(event: Event): void {
    if (!this.host.nativeElement.contains(event.target)) this.open = false;
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.open = false;
  }

  private applyLang(lang: AppLang): void {
    this.translate.use(lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = 'ltr';
  }

  private resolveInitialLang(): AppLang {
    try {
      const user = this.loginService.getCurrentUser();
      if (user?.preferredLang === 'fr' || user?.preferredLang === 'en') return user.preferredLang;
      if (user?.id !== undefined && user?.id !== null) {
        const perUser = localStorage.getItem(`pp-lang-${user.id}`);
        if (perUser === 'fr' || perUser === 'en') return perUser;
        if (perUser === 'ar') {
          try { localStorage.setItem(`pp-lang-${user.id}`, 'fr'); } catch {}
          return 'fr';
        }
      }
    } catch {}
    let raw = 'fr';
    try {
      raw = localStorage.getItem('lang') || 'fr';
    } catch {}
    if (raw === 'ar') {
      try { localStorage.setItem('lang', 'fr'); } catch {}
      return 'fr';
    }
    return raw === 'en' ? 'en' : 'fr';
  }
}
