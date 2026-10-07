import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
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
    private toast: ToastService,
    private translate: TranslateService
  ) {}

  onSubmit(): void {
    if (!this.email) {
      const msg: string = this.translate.instant('FORGOT_PASSWORD.EMAIL_REQUIRED');
      this.error = msg;
      this.toast.error(msg);
      return;
    }
    this.loading = true;
    this.error = null;
    this.http.post<{ message: string }>('/api/auth/password-reset/request', { email: this.email }).subscribe({
      next: (res) => {
        this.sent = true;
        this.loading = false;
        this.toast.success(res.message || this.translate.instant('FORGOT_PASSWORD.EMAIL_SENT_FALLBACK'));
      },
      error: (err) => {
        // Email inconnu (404) ou autre échec : message affiché, on reste sur la page.
        const message: string = err.error?.message || this.translate.instant('FORGOT_PASSWORD.SEND_ERROR');
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
