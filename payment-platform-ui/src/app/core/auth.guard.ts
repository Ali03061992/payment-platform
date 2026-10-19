import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { LoginService } from '../services/login.service';

/**
 * Garde d'authentification : exige un JWT valide en mémoire (restauré au boot
 * via le cookie HttpOnly), sinon redirige vers /login en mémorisant la
 * destination pour la restaurer après reconnexion.
 */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {

  constructor(private router: Router, private login: LoginService) {}

  /** Autorise la route si un token valide est présent, sinon /login?returnUrl=… */
  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (this.login.isLoggedIn()) {
      return true;
    }
    this.login.purgeSession();
    this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }
}
