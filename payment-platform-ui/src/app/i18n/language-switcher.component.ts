import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-language-switcher',
  template: `
    <div class="lang-switcher">
      <button
        class="lang-btn"
        [class.active]="currentLang === 'fr'"
        (click)="switchLang('fr')"
        aria-label="Français">
        FR
      </button>
      <button
        class="lang-btn"
        [class.active]="currentLang === 'en'"
        (click)="switchLang('en')"
        aria-label="English">
        EN
      </button>
    </div>
  `,
  styles: [`
    .lang-switcher {
      display: flex;
      gap: 4px;
      align-items: center;
    }
    .lang-btn {
      background: rgba(255,255,255,0.1);
      border: 1px solid rgba(255,255,255,0.2);
      color: rgba(255,255,255,0.7);
      font-size: 11px;
      font-weight: 700;
      padding: 4px 8px;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.2s;
      letter-spacing: 0.5px;
    }
    .lang-btn:hover {
      background: rgba(255,255,255,0.15);
      color: #fff;
    }
    .lang-btn.active {
      background: #4fc3f7;
      color: #1a1a2e;
      border-color: #4fc3f7;
    }
  `],
  standalone: false
})
export class LanguageSwitcherComponent {
  currentLang: string;

  constructor(private translate: TranslateService) {
    const saved = localStorage.getItem('lang') || 'fr';
    this.currentLang = saved;
    this.translate.setDefaultLang('fr');
    this.translate.use(saved);
  }

  switchLang(lang: string): void {
    this.currentLang = lang;
    this.translate.use(lang);
    localStorage.setItem('lang', lang);
    document.documentElement.lang = lang;
  }
}
