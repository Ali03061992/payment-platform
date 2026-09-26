import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

/**
 * Garde d'authentification : exige un JWT présent et non expiré, sinon redirige vers /login.
 */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {

  constructor(private router: Router) {}

  /** Autorise la route si un token valide est présent, sinon redirige vers /login. */
  canActivate(): boolean {
    const token = sessionStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login']);
      return false;
    }

    if (this.isTokenExpired(token)) {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
      this.router.navigate(['/login']);
      return false;
    }

    return true;
  }

  private isTokenExpired(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const exp = payload.exp * 1000;
      return Date.now() > exp;
    } catch {
      return true;
    }
  }
}
