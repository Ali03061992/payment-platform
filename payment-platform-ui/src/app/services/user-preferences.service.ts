import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { LoginService } from './login.service';

export interface UserPreferencesPatch {
  preferredLang?: 'fr' | 'en';
  accentColor1?: string;
  accentColor2?: string;
  tourSeen?: boolean;
}

@Injectable({ providedIn: 'root' })
export class UserPreferencesService {
  private apiUrl = '/api/users/me/preferences';

  constructor(private http: HttpClient, private loginService: LoginService) {}

  private userId(): string | null {
    try {
      const user = this.loginService.getCurrentUser();
      if (user?.id !== undefined && user?.id !== null) return String(user.id);
    } catch {}
    return null;
  }

  private langKey(): string {
    const id = this.userId();
    return id ? `pp-lang-${id}` : 'lang';
  }

  private accentsKey(): string | null {
    const id = this.userId();
    return id ? `pp-accents-${id}` : null;
  }

  private readLocal(): UserPreferencesPatch {
    const out: UserPreferencesPatch = {};
    try {
      const id = this.userId();
      const langRaw = id ? localStorage.getItem(`pp-lang-${id}`) : null;
      const globalLang = localStorage.getItem('lang');
      const lang = langRaw || globalLang;
      if (lang === 'fr' || lang === 'en') out.preferredLang = lang;
      const key = this.accentsKey();
      const rawAccents = key ? localStorage.getItem(key) : null;
      if (rawAccents) {
        try {
          const parsed = JSON.parse(rawAccents);
          if (Array.isArray(parsed) && parsed.length >= 2) {
            if (typeof parsed[0] === 'string') out.accentColor1 = parsed[0];
            if (typeof parsed[1] === 'string') out.accentColor2 = parsed[1];
          }
        } catch {}
      }
    } catch {}
    return out;
  }

  private persistLocal(patch: UserPreferencesPatch): void {
    try {
      if (patch.preferredLang === 'fr' || patch.preferredLang === 'en') {
        localStorage.setItem('lang', patch.preferredLang);
        const id = this.userId();
        if (id) localStorage.setItem(`pp-lang-${id}`, patch.preferredLang);
      }
      if (typeof patch.accentColor1 === 'string' || typeof patch.accentColor2 === 'string') {
        const key = this.accentsKey();
        if (key) {
          let c1 = patch.accentColor1 || '';
          let c2 = patch.accentColor2 || '';
          try {
            const existing = localStorage.getItem(key);
            if (existing) {
              const parsed = JSON.parse(existing);
              if (Array.isArray(parsed)) {
                if (!c1 && typeof parsed[0] === 'string') c1 = parsed[0];
                if (!c2 && typeof parsed[1] === 'string') c2 = parsed[1];
              }
            }
          } catch {}
          localStorage.setItem(key, JSON.stringify([c1 || '#0284c7', c2 || '#e63946']));
        }
      }
    } catch {}
  }

  get(): Observable<UserPreferencesPatch> {
    return this.http.get<UserPreferencesPatch>(this.apiUrl).pipe(
      tap(prefs => this.persistLocal(prefs || {})),
      catchError(() => of(this.readLocal()))
    );
  }

  update(patch: UserPreferencesPatch): Observable<UserPreferencesPatch> {
    this.persistLocal(patch);
    return this.http.patch<UserPreferencesPatch>(this.apiUrl, patch).pipe(
      tap(saved => this.persistLocal((saved as UserPreferencesPatch) || patch)),
      catchError(() => of(patch))
    );
  }
}
