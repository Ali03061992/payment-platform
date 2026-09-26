import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';

/**
 * Intercepteur JWT : joint le token aux requêtes et rejoue une fois après refresh silencieux sur 401.
 * Les URLs d'auth sont exclues pour éviter toute boucle de renouvellement.
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  /**
   * M1 : URLs d'auth exclues du refresh silencieux (pas de boucle : un 401 sur
   * ces appels est terminal).
   */
  private static readonly AUTH_URLS = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/logout',
    '/api/auth/password-setup',
  ];

  /** Refresh en cours partagé (single-flight) entre les 401 concurrents. */
  private refreshInFlight: Observable<string> | null = null;

  // LoginService via Injector (lazy) pour éviter le cycle HttpClient -> intercepteurs -> LoginService.
  constructor(private router: Router, private injector: Injector) {}

  /** Intercepte la requête, joint le Bearer et gère le refresh unique sur 401. */
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = sessionStorage.getItem('token');
    let authReq = req;

    if (token) {
      authReq = req.clone({
        setHeaders: { Authorization: `Bearer ${token}` }
      });
    }

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 0) {
          return throwError(() => error);
        }
        if (error.status !== 401) {
          return throwError(() => error);
        }
        const url = req.url || '';
        if (url.includes('/api/auth/login')) {
          this.clearSession();
          this.router.navigate(['/login']);
          return throwError(() => error);
        }
        if (JwtInterceptor.AUTH_URLS.some((u) => url.includes(u))) {
          return throwError(() => error);
        }
        if (!sessionStorage.getItem('refreshToken')) {
          this.clearSession();
          this.router.navigate(['/login']);
          return throwError(() => error);
        }
        // M1 : refresh silencieux + rejeu unique de la requête d'origine.
        if (!this.refreshInFlight) {
          this.refreshInFlight = this.injector.get(LoginService).refresh().pipe(
            map((res) => res.accessToken),
            shareReplay(1)
          );
        }
        const ongoing = this.refreshInFlight;
        return ongoing.pipe(
          switchMap((newToken) =>
            next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } }))
          ),
          catchError(() => {
            this.clearSession();
            this.router.navigate(['/login']);
            return throwError(() => error);
          }),
          finalize(() => {
            this.refreshInFlight = null;
          })
        );
      })
    );
  }

  private clearSession(): void {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('refreshToken');
    sessionStorage.removeItem('user');
  }
}
