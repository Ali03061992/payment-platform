import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';

/**
 * Intercepteur JWT : joint le token (mémoire LoginService) aux requêtes et
 * rejoue une fois après refresh silencieux (cookie HttpOnly, single-flight
 * partagé avec le SSE) sur 401. Les URLs d'auth sont exclues pour éviter
 * toute boucle de renouvellement.
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  /**
   * URLs d'auth exclues du refresh silencieux (pas de boucle : un 401 sur
   * ces appels est terminal).
   */
  private static readonly AUTH_URLS = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/logout',
    '/api/auth/password-setup',
    '/api/auth/webauthn/login',
  ];

  // LoginService via Injector (lazy) pour éviter le cycle HttpClient -> intercepteurs -> LoginService.
  constructor(private router: Router, private injector: Injector) {}

  /** Intercepte la requête, joint le Bearer et gère le refresh unique sur 401. */
  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const login = this.injector.get(LoginService);
    const token = login.getToken();
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
          login.purgeSession();
          this.router.navigate(['/login']);
          return throwError(() => error);
        }
        if (JwtInterceptor.AUTH_URLS.some((u) => url.includes(u))) {
          return throwError(() => error);
        }
        // Refresh silencieux (cookie HttpOnly, single-flight) + rejeu unique.
        return login.refresh().pipe(
          switchMap((newToken) =>
            next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${newToken}` } }))
          ),
          catchError(() => {
            login.purgeSession();
            this.router.navigate(['/login']);
            return throwError(() => error);
          })
        );
      })
    );
  }
}
