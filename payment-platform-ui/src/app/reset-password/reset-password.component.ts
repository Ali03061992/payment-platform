import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
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
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
    if (!this.token) {
      this.checkingToken = false;
      this.toast.error('Lien invalide. Veuillez refaire une demande de réinitialisation.');
      return;
    }
    this.http.get<{ valid: boolean }>(`/api/auth/password-reset/validate?token=${this.token}`).subscribe({
      next: (res) => {
        this.tokenValid = res.valid;
        this.checkingToken = false;
        if (!this.tokenValid) {
          this.toast.error('Ce lien est invalide, expiré ou déjà utilisé. Veuillez refaire une demande.');
        }
      },
      error: () => {
        this.checkingToken = false;
        this.toast.error('Erreur lors de la vérification du lien.');
      }
    });
  }

  onSubmit(): void {
    if (this.newPassword.length < 8) {
      this.toast.error('Le mot de passe doit contenir au moins 8 caractères.');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.toast.error('Les mots de passe ne correspondent pas.');
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
        this.toast.error(err.error?.message || 'Erreur lors de la réinitialisation.');
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
