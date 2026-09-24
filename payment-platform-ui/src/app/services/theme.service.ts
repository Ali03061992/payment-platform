import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type Theme = 'light' | 'dark';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly STORAGE_KEY = 'pp-theme';
  private themeSubject = new BehaviorSubject<Theme>(this.getInitialTheme());

  theme$: Observable<Theme> = this.themeSubject.asObservable();

  constructor() {
    this.applyTheme(this.themeSubject.getValue());
  }

  private getInitialTheme(): Theme {
    const stored = localStorage.getItem(this.STORAGE_KEY);
    if (stored === 'dark' || stored === 'light') {
      return stored;
    }
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }
    return 'light';
  }

  private applyTheme(theme: Theme): void {
    document.documentElement.setAttribute('data-theme', theme);
  }

  toggle(): void {
    const current = this.themeSubject.getValue();
    const next: Theme = current === 'light' ? 'dark' : 'light';
    this.themeSubject.next(next);
    localStorage.setItem(this.STORAGE_KEY, next);
    this.applyTheme(next);
  }

  get isDark(): boolean {
    return this.themeSubject.getValue() === 'dark';
  }

  get currentTheme(): Theme {
    return this.themeSubject.getValue();
  }
}
