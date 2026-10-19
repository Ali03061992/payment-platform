import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError, finalize, shareReplay, map, catchError, switchMap, tap } from 'rxjs';
import { LoginRequest, LoginResponse, User } from '../models/user.model';

/** Réponse de /api/auth/refresh : accessToken + rotation éventuelle du refresh token. */
interface RefreshResponse {
  accessToken: string;
  refreshToken?: string;
  refreshExpiresIn?: number;
}

/**
 * Service d'authentification centralisé.
 *
 * Access token : en mémoire uniquement (jamais persisté) — la PWA relancée le
 * régénère via restoreSession() (APP_INITIALIZER) avant le routing.
 * Refresh token : cookie HttpOnly pp_rt posé par le backend (web) ; le body
 * reste accepté pour migration depuis l'ancien sessionStorage et pour Flutter.
 * Utilisateur minimal (id, rôles, organisation) : localStorage (auteur) +
 * miroir sessionStorage pour les composants qui lisent encore directement ;
 * revalidé au boot par GET /me.
 */
@Injectable({ providedIn: 'root' })
export class LoginService {
  private apiUrl = '/api/auth';
  private accessToken: string | null = null;
  /** Refresh en cours partagé (single-flight) : interceptor 401 + SSE. */
  private refreshInFlight: Observable<string> | null = null;
  private static readonly USER_KEY = 'user';
  /** Clés legacy (avant migration cookie) purgées dès qu'une session est adoptée. */
  private static readonly LEGACY_KEYS = ['token', 'refreshToken'];

  constructor(private http: HttpClient) {}

  /** Connecte un utilisateur et adopte la session (access token mémoire, cookie côté serveur). */
  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap(res => this.adoptSession(res))
    );
  }

  /** Adopte une réponse login/passkey : le refresh token n'est pas stocké (cookie HttpOnly). */
  adoptSession(res: LoginResponse): void {
    this.accessToken = res.accessToken;
    this.purgeLegacy();
  }

  /**
   * Renouvellement silencieux (single-flight partagé interceptor/SSE).
   * Envoie le cookie HttpOnly automatiquement ; un refreshToken legacy
   * sessionStorage (pré-migration) est envoyé en body une dernière fois.
   */
  refresh(): Observable<string> {
    if (!this.refreshInFlight) {
      const legacy = sessionStorage.getItem('refreshToken');
      const body: { refreshToken?: string } = legacy ? { refreshToken: legacy } : {};
      this.refreshInFlight = this.http.post<RefreshResponse>(`${this.apiUrl}/refresh`, body).pipe(
        map(res => {
          this.accessToken = res.accessToken;
          this.purgeLegacy();
          return res.accessToken;
        }),
        catchError(err => {
          this.purgeLegacy();
          return throwError(() => err);
        }),
        finalize(() => { this.refreshInFlight = null; }),
        shareReplay(1)
      );
    }
    return this.refreshInFlight;
  }

  /**
   * Restaure la session au boot (PWA relancée : mémoire vide, le cookie
   * HttpOnly pp_rt régénère un access token sans ressaisie). Récupère
   * ensuite le profil via /me et re-synchrone l'utilisateur. Ne lève jamais.
   */
  restoreSession(): Observable<boolean> {
    if (this.accessToken) return of(true);
    return this.refresh().pipe(
      switchMap(() => this.getMe()),
      map(user => {
        this.setCurrentUser(user);
        return true;
      }),
      catchError(() => of(false))
    );
  }

  /** Récupère le profil de l'utilisateur connecté. */
  getMe(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  /** Déconnecte : révocation serveur (cookie ou body legacy) puis purge locale. */
  logout(): void {
    const legacy = sessionStorage.getItem('refreshToken');
    const body: { refreshToken?: string } = legacy ? { refreshToken: legacy } : {};
    // Best-effort : la déconnexion locale ne doit jamais être bloquée par le réseau.
    this.http.post(`${this.apiUrl}/logout`, body).subscribe({ error: () => {} });
    this.purgeSession();
  }

  /** Purge complète locale (mémoire + storages) — sans appel réseau. */
  purgeSession(): void {
    this.accessToken = null;
    try { localStorage.removeItem(LoginService.USER_KEY); } catch {}
    try { sessionStorage.removeItem(LoginService.USER_KEY); } catch {}
    this.purgeLegacy();
  }

  /** Access token en mémoire, ou nul. */
  getToken(): string | null {
    return this.accessToken;
  }

  /** Indique si un token d'accès valide (non expiré) est en mémoire. */
  isLoggedIn(): boolean {
    return !!this.accessToken && !this.isExpired(this.accessToken);
  }

  /** Lit l'utilisateur minimal persisté, ou nul si absent. */
  getCurrentUser(): User | null {
    try {
      const userJson = localStorage.getItem(LoginService.USER_KEY);
      return userJson ? JSON.parse(userJson) : null;
    } catch {
      return null;
    }
  }

  /** Persiste l'utilisateur minimal (auteur localStorage + miroir sessionStorage). */
  setCurrentUser(user: User | null): void {
    try {
      if (user) {
        const json = JSON.stringify(user);
        localStorage.setItem(LoginService.USER_KEY, json);
        sessionStorage.setItem(LoginService.USER_KEY, json);
      } else {
        localStorage.removeItem(LoginService.USER_KEY);
        sessionStorage.removeItem(LoginService.USER_KEY);
      }
    } catch {}
  }

  /** Applique un patch sur l'utilisateur persisté (ex. tourSeen). */
  updateCurrentUser(patch: Partial<User>): void {
    const current = this.getCurrentUser();
    if (!current) return;
    this.setCurrentUser({ ...current, ...patch } as User);
  }

  /** Vérifie que l'utilisateur courant possède au moins un des rôles demandés. */
  hasRole(...roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return user.roles.some(r => roles.includes(r));
  }

  private purgeLegacy(): void {
    for (const key of LoginService.LEGACY_KEYS) {
      try { sessionStorage.removeItem(key); } catch {}
    }
  }

  private isExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1] || ''));
      if (!payload.exp) return false;
      return payload.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
