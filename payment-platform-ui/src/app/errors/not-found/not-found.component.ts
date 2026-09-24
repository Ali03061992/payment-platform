import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../../services/login.service';

/**
 * M4 : page 404 — le wildcard y redirige au lieu du silencieux /login.
 */
@Component({
    selector: 'app-not-found',
    template: `
        <div class="error-page">
            <h1>404</h1>
            <h2>Page introuvable</h2>
            <p>La page demandée n'existe pas ou a été déplacée.</p>
            <button type="button" (click)="back()">Retour</button>
        </div>
    `,
    styles: [`
        .error-page { text-align: center; padding: 64px 16px; }
        .error-page h1 { font-size: 72px; margin: 0; }
        .error-page button { margin-top: 16px; cursor: pointer; }
    `],
    standalone: false
})
export class NotFoundComponent {
  constructor(private router: Router, private loginService: LoginService) {}

  back(): void {
    if (this.loginService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/login']);
    }
  }
}
