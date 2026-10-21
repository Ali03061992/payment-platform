import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, UrlTree } from '@angular/router';
import { Observable, catchError, map, of } from 'rxjs';
import { LoginService } from '../services/login.service';

/**
 * Garde d'authentification : exige un JWT valide en mémoire. Si le token a
 * expiré mais que le cookie HttpOnly pp_rt est encore valide (PWA rouverte,
 * navigation après 30 min), tente un refresh silencieux avant de rediriger.
 */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {

  constructor(private router: Router, private login: LoginService) {}

  /** Autorise la route si un token valide est présent, sinon tente le refresh puis /login?returnUrl=… */
  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<boolean | UrlTree> {
    if (this.login.isLoggedIn()) {
      return of(true);
    }
    return this.login.restoreSession().pipe(
      map((restored) => restored
        ? true
        : this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })),
      catchError(() => of(this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } })))
    );
  }
}
