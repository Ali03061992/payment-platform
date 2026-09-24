import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, throwError } from 'rxjs';
import { LoginRequest, LoginResponse, User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class LoginService {
  private apiUrl = '/api/auth';

  constructor(private http: HttpClient) {}

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

  getMe(): Observable<User> {
    return this.http.get<User>(`${this.apiUrl}/me`);
  }

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

  isLoggedIn(): boolean {
    return !!sessionStorage.getItem('token');
  }

  getCurrentUser(): User | null {
    const userJson = sessionStorage.getItem('user');
    return userJson ? JSON.parse(userJson) : null;
  }

  hasRole(...roles: string[]): boolean {
    const user = this.getCurrentUser();
    if (!user) return false;
    return user.roles.some(r => roles.includes(r));
  }
}
