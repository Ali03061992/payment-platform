import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, throwError } from 'rxjs';
import { LoginRequest, LoginResponse, User } from '../models/user.model';

/**
 * Service d'authentification (login, refresh silencieux, session en sessionStorage).
 * Stocke les tokens d'accès et de refresh après chaque émission serveur.
 */
@Injectable({ providedIn: 'root' })
export class LoginService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

  /** Connecte un utilisateur et mémorise les tokens reçus. */
  login(data: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, data).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.accessToken);
        if (res.refreshToken) {
          sessionStorage.setItem('refreshToken', res.refreshToken);
        }
      })
    );
  }

  /**
   * M1 : renouvellement silencieux (rotation côté serveur : le refreshToken
   * stocké est remplacé à chaque appel réussi).
   */
  refresh(): Observable<LoginResponse> {
    const stored = sessionStorage.getItem('refreshToken');
    if (!stored) {
      return throwError(() => new Error('No refresh token'));
    }
    return this.http.post<LoginResponse>(`${this.apiUrl}/refresh`, { refreshToken: stored }).pipe(
      tap(res => {
        sessionStorage.setItem('token', res.accessToken);
        if (res.refreshToken) {
          sessionStorage.setItem('refreshToken', res.refreshToken);
        }
      })
    );
  }

  /** Récupère le profil de l'utilisateur connecté. */
  getMe(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

  /** Déconnecte en best-effort (révocation serveur puis purge locale). */
  logout(): void {
    const stored = sessionStorage.getItem('refreshToken');
    if (stored) {
      // Best-effort : la déconnexion locale ne doit jamais être bloquée par le réseau.
      this.http.post(`${this.apiUrl}/logout`, { refreshToken: stored }).subscribe({
        error: () => {}
      });
    }
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('user');
  }

  /** Indique si un token d'accès est présent en session. */
  isLoggedIn(): boolean {
    return !!sessionStorage.getItem('token');
  }

  /** Lit l'utilisateur mémorisé en session, ou nul si absent. */
  getCurrentUser(): User | null {
    const userJson = sessionStorage.getItem('user');
    return userJson ? JSON.parse(userJson) : null;
  }

  /** Vérifie que l'utilisateur courant possède au moins un des rôles demandés. */
  hasRole(...roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return user.roles.some(r => roles.includes(r));
  }
}
