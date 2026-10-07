import { Injectable, Injector } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoginService } from './login.service';
import { UserPreferencesService } from './user-preferences.service';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'pp-theme';
  private readonly DEFAULT_ACCENT1 = '#0284c7';
  private readonly DEFAULT_ACCENT2 = '#e63946';
  private themeSubject = new BehaviorSubject<Theme>(this.getInitialTheme());
  private accent1Subject = new BehaviorSubject<string>(this.DEFAULT_ACCENT1);
  private accent2Subject = new BehaviorSubject<string>(this.DEFAULT_ACCENT2);

  theme$: Observable<Theme> = this.themeSubject.asObservable();
  accent1$: Observable<string> = this.accent1Subject.asObservable();
  accent2$: Observable<string> = this.accent2Subject.asObservable();

  constructor(private injector: Injector) {
    this.applyTheme(this.themeSubject.getValue());
    this.loadStoredAccents();
    this.initUserAccents();
  }

  private get loginService(): LoginService | null {
    try {
      return this.injector.get(LoginService, null);
    } catch {
      return null;
    }
  }

  private get prefs(): UserPreferencesService | null {
    try {
      return this.injector.get(UserPreferencesService, null);
    } catch {
      return null;
    }
  }

  private getInitialTheme(): Theme {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored === 'dark' || stored === 'light') {
        return stored;
      }
    } catch {}
    try {
      if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        return 'dark';
      }
    } catch {}
    return 'light';
  }

  private applyTheme(theme: Theme): void {
    try {
      document.documentElement.setAttribute('data-theme', theme);
    } catch {}
  }

  toggle(): void {
    const current = this.themeSubject.getValue();
    const next: Theme = current === 'light' ? 'dark' : 'light';
    this.themeSubject.next(next);
    try {
      localStorage.setItem(this.STORAGE_KEY, next);
    } catch {}
    this.applyTheme(next);
  }

  get isDark(): boolean {
    return this.themeSubject.getValue() === 'dark';
  }

  get currentTheme(): Theme {
    return this.themeSubject.getValue();
  }

  get currentAccent1(): string {
    return this.accent1Subject.getValue();
  }

  get currentAccent2(): string {
    return this.accent2Subject.getValue();
  }

  private isHex6(value: unknown): value is string {
    return typeof value === 'string' && /^#[0-9a-fA-F]{6}$/.test(value);
  }

  private shade(hex: string, amt: number): string {
    const n = hex.replace('#', '');
    const r = parseInt(n.substring(0, 2), 16);
    const g = parseInt(n.substring(2, 4), 16);
    const b = parseInt(n.substring(4, 6), 16);
    const t = amt < 0 ? 0 : 255;
    const p = Math.abs(amt) / 100;
    const R = Math.round((t - r) * p + r);
    const G = Math.round((t - g) * p + g);
    const B = Math.round((t - b) * p + b);
    const toHex = (v: number) => Math.max(0, Math.min(255, v)).toString(16).padStart(2, '0');
    return `#${toHex(R)}${toHex(G)}${toHex(B)}`;
  }

  private hexToRgba(hex: string, alpha: number): string {
    const n = hex.replace('#', '');
    const r = parseInt(n.substring(0, 2), 16);
    const g = parseInt(n.substring(2, 4), 16);
    const b = parseInt(n.substring(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  private writeStyle(c1: string, c2: string): void {
    try {
      const root = document.documentElement.style;
      root.setProperty('--theme-primary', c1);
      root.setProperty('--theme-primary-dark', this.shade(c1, -18));
      root.setProperty('--theme-primary-soft', this.hexToRgba(c1, 0.12));
      root.setProperty('--theme-secondary', c2);
      root.setProperty('--theme-secondary-dark', this.shade(c2, -18));
      root.setProperty('--theme-secondary-soft', this.hexToRgba(c2, 0.12));
      root.setProperty('--grad-theme', `linear-gradient(135deg, ${c1} 0%, ${c2} 100%)`);
    } catch {}
  }

  private persistAccents(c1: string, c2: string): void {
    try {
      const svc = this.loginService;
      const user = svc && typeof svc.getCurrentUser === 'function' ? svc.getCurrentUser() : null;
      if (user?.id !== undefined && user?.id !== null) {
        localStorage.setItem(`pp-accents-${user.id}`, JSON.stringify([c1, c2]));
        return;
      }
    } catch {}
    try {
      localStorage.setItem('pp-accents', JSON.stringify([c1, c2]));
    } catch {}
  }

  private pushBackend(c1: string, c2: string): void {
    try {
      const prefs = this.prefs;
      if (prefs && typeof prefs.update === 'function') {
        prefs.update({ accentColor1: c1, accentColor2: c2 }).subscribe({ error: () => {} });
      }
    } catch {}
  }

  setAccents(c1: string, c2: string): void {
    if (!this.isHex6(c1) || !this.isHex6(c2)) return;
    this.accent1Subject.next(c1.toLowerCase());
    this.accent2Subject.next(c2.toLowerCase());
    this.applyAccents();
  }

  applyAccents(): void {
    const c1 = this.accent1Subject.getValue();
    const c2 = this.accent2Subject.getValue();
    this.writeStyle(c1, c2);
    this.persistAccents(c1, c2);
    this.pushBackend(c1, c2);
  }

  resetAccents(): void {
    this.accent1Subject.next(this.DEFAULT_ACCENT1);
    this.accent2Subject.next(this.DEFAULT_ACCENT2);
    this.applyAccents();
  }

  private loadStoredAccents(): void {
    try {
      const svc = this.loginService;
      const user = svc && typeof svc.getCurrentUser === 'function' ? svc.getCurrentUser() : null;
      if (user && this.isHex6((user as any).accentColor1) && this.isHex6((user as any).accentColor2)) {
        this.accent1Subject.next(((user as any).accentColor1 as string).toLowerCase());
        this.accent2Subject.next(((user as any).accentColor2 as string).toLowerCase());
        this.writeStyle(this.accent1Subject.getValue(), this.accent2Subject.getValue());
        return;
      }
      let raw: string | null = null;
      try {
        if (user?.id !== undefined && user?.id !== null) raw = localStorage.getItem(`pp-accents-${user.id}`);
      } catch {}
      if (!raw) {
        try {
          raw = localStorage.getItem('pp-accents');
        } catch {}
      }
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && this.isHex6(parsed[0]) && this.isHex6(parsed[1])) {
          this.accent1Subject.next((parsed[0] as string).toLowerCase());
          this.accent2Subject.next((parsed[1] as string).toLowerCase());
        }
      }
      this.writeStyle(this.accent1Subject.getValue(), this.accent2Subject.getValue());
    } catch {}
  }

  private applyUserAccents(user: any): void {
    try {
      if (!user) return;
      if (this.isHex6(user.accentColor1) && this.isHex6(user.accentColor2)) {
        this.accent1Subject.next((user.accentColor1 as string).toLowerCase());
        this.accent2Subject.next((user.accentColor2 as string).toLowerCase());
        this.writeStyle(this.accent1Subject.getValue(), this.accent2Subject.getValue());
        this.persistAccents(this.accent1Subject.getValue(), this.accent2Subject.getValue());
        return;
      }
      if (user?.id !== undefined && user?.id !== null) {
        const raw = localStorage.getItem(`pp-accents-${user.id}`);
        if (raw) {
          const parsed = JSON.parse(raw);
          if (Array.isArray(parsed) && this.isHex6(parsed[0]) && this.isHex6(parsed[1])) {
            this.accent1Subject.next((parsed[0] as string).toLowerCase());
            this.accent2Subject.next((parsed[1] as string).toLowerCase());
            this.writeStyle(this.accent1Subject.getValue(), this.accent2Subject.getValue());
          }
        }
      }
    } catch {}
  }

  private initUserAccents(): void {
    try {
      const svc = this.loginService;
      const stream = (svc as any)?.currentUser$;
      if (stream && typeof stream.subscribe === 'function') {
        stream.subscribe((user: any) => this.applyUserAccents(user));
        return;
      }
      const user = svc && typeof svc.getCurrentUser === 'function' ? svc.getCurrentUser() : null;
      this.applyUserAccents(user);
    } catch {}
  }
}
