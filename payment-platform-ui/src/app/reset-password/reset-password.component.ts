import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { TranslateService } from '@ngx-translate/core';
import { ToastService } from '../services/toast.service';

@Component({
    selector: 'app-reset-password',
    templateUrl: './reset-password.component.html',
    styleUrls: ['./reset-password.component.css'],
    standalone: false
})
export class ResetPasswordComponent implements OnInit {
  token = '';
  newPassword = '';
  confirmPassword = '';
  loading = false;
  success = false;
  tokenValid = false;
  checkingToken = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private toast: ToastService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.checkingToken = false;
      this.toast.error(this.translate.instant('RESET_PASSWORD.INVALID_LINK_MSG'));
      return;
    }
    this.http.get<{ valid: boolean }>(`/api/auth/password-reset/validate?token=${this.token}`).subscribe({
      next: (res) => {
        this.tokenValid = res.valid;
        this.checkingToken = false;
        if (!this.tokenValid) {
          this.toast.error(this.translate.instant('RESET_PASSWORD.INVALID_LINK_USED'));
        }
      },
      error: () => {
        this.checkingToken = false;
        this.toast.error(this.translate.instant('PASSWORD_SETUP.VERIFY_ERROR'));
      }
    });
  }

  onSubmit(): void {
    if (this.newPassword.length < 8) {
      this.toast.error(this.translate.instant('PASSWORD_SETUP.PASSWORD_MIN_ERROR'));
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.toast.error(this.translate.instant('PASSWORD_SETUP.PASSWORD_MISMATCH'));
      return;
    }
    this.loading = true;
    this.http.post<{ message: string }>('/api/auth/password-reset/confirm', {
      token: this.token,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
      },
      error: (err) => {
        this.toast.error(err.error?.message || this.translate.instant('RESET_PASSWORD.RESET_ERROR'));
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
