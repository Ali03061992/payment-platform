import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

@Component({
    selector: 'app-change-password',
    templateUrl: './change-password.component.html',
    styleUrls: ['./change-password.component.css'],
    standalone: false
})
export class ChangePasswordComponent {
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  loading = false;
  submitted = false;
  error = '';

  constructor(
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router,
    private translate: TranslateService
  ) {}

  onSubmit(): void {
    this.error = '';
    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.error = this.translate.instant('CHANGE_PASSWORD.ALL_FIELDS_REQUIRED');
      return;
    }
    if (this.newPassword.length < 8) {
      this.error = this.translate.instant('CHANGE_PASSWORD.MIN_LENGTH_ERROR');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.error = this.translate.instant('CHANGE_PASSWORD.MISMATCH_ERROR');
      return;
    }
    this.loading = true;
    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.submitted = true;
        this.toastService.show(this.translate.instant('CHANGE_PASSWORD.SUCCESS_TOAST'), 'success');
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || this.translate.instant('CHANGE_PASSWORD.ERROR_TOAST');
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
