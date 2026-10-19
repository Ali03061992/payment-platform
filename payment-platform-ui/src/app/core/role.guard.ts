import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { LoginService } from '../services/login.service';

/**
 * Garde de rôles : exige le token, l'utilisateur persisté et l'un des rôles
 * de la route, sinon /login (session absente) ou /403 (mauvais rôle).
 */
@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {

  constructor(private router: Router, private login: LoginService) {}

  /** Autorise la route si l'utilisateur possède l'un des rôles requis. */
  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean {
    if (!this.login.isLoggedIn()) {
      this.login.purgeSession();
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    const user = this.login.getCurrentUser();
    if (!user) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    const requiredRoles: string[] = route.data['roles'] || [];
    if (requiredRoles.length === 0) {
      return true;
    }

    if (user.roles.some((r: string) => requiredRoles.includes(r))) {
      return true;
    }

    this.router.navigate(['/403']);
    return false;
  }
}
