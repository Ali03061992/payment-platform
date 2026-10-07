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
            <h2>{{ 'ERRORS.FORBIDDEN_TITLE' | translate }}</h2>
            <p>{{ 'ERRORS.FORBIDDEN_MSG' | translate }}</p>
            <button type="button" (click)="back()">{{ 'ERRORS.FORBIDDEN_BACK' | translate }}</button>
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

  /** Retourne au tableau de bord si connecté, sinon vers le login. */
  back(): void {
    if (this.loginService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
