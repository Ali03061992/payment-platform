import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../../services/login.service';

/**
 * M4 : page 403 — le RoleGuard y redirige au lieu du silencieux /dashboard.
 */
@Component({
    selector: 'app-forbidden',
    template: `
        <div class="error-page">
            <h1>403</h1>
            <h2>Accès refusé</h2>
            <p>Votre rôle ne permet pas d'accéder à cette page.</p>
            <button type="button" (click)="back()">Retour au tableau de bord</button>
        </div>
    `,
    styles: [`
        .error-page { text-align: center; padding: 64px 16px; }
        .error-page h1 { font-size: 72px; margin: 0; }
        .error-page button { margin-top: 16px; cursor: pointer; }
    `],
    standalone: false
})
export class ForbiddenComponent {
  constructor(private router: Router, private loginService: LoginService) {}

  back(): void {
    if (this.loginService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
