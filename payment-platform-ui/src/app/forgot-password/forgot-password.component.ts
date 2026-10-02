import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../services/toast.service';

@Component({
    selector: 'app-forgot-password',
    templateUrl: './forgot-password.component.html',
    styleUrls: ['./forgot-password.component.css'],
    standalone: false
})
export class ForgotPasswordComponent {
  email = '';
  loading = false;
  sent = false;
  error: string | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private toast: ToastService
  ) {}

  onSubmit(): void {
    if (!this.email) {
      this.error = 'Veuillez saisir votre adresse email.';
      this.toast.error(this.error);
      return;
    }
    this.loading = true;
    this.error = null;
    this.http.post<{ message: string }>('/api/auth/password-reset/request', { email: this.email }).subscribe({
      next: (res) => {
        this.sent = true;
        this.loading = false;
        this.toast.success(res.message || 'Email envoyé.');
      },
      error: (err) => {
        // Email inconnu (404) ou autre échec : message affiché, on reste sur la page.
        const message: string = err.error?.message || "Erreur lors de l'envoi de l'email.";
        this.error = message;
        this.toast.error(message);
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
