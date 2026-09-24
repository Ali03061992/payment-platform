import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {

  constructor(private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot): boolean {
    const token = sessionStorage.getItem('token');
    if (!token) {
      this.router.navigate(['/login']);
      return false;
    }

    const userJson = sessionStorage.getItem('user');
    if (!userJson) {
      this.router.navigate(['/login']);
      return false;
    }

    const user = JSON.parse(userJson);
    const requiredRoles: string[] = route.data['roles'] || [];

    if (requiredRoles.length === 0) {
      return true;
    }

    const hasRole = user.roles.some((r: string) => requiredRoles.includes(r));
    if (hasRole) {
      return true;
    }

    this.router.navigate(['/403']);
    return false;
  }
}
