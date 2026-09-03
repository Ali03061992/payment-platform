import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-password-setup',
  templateUrl: './password-setup.component.html',
  styleUrls: ['./password-setup.component.css']
})
export class PasswordSetupComponent implements OnInit {
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
      this.toast.error('Lien invalide. Veuillez demander un nouveau lien de configuration.');
      return;
    }
    this.http.get<{ valid: boolean }>(`/api/auth/password-setup/validate?token=${this.token}`).subscribe({
      next: (res) => {
        this.tokenValid = res.valid;
        this.checkingToken = false;
        if (!this.tokenValid) {
          this.toast.error('Ce lien est invalide ou a déjà été utilisé. Veuillez demander un nouveau lien.');
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
    this.http.post<{ message: string }>('/api/auth/password-setup/complete', {
      token: this.token,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
      },
      error: (err) => {
        this.toast.error(err.error?.message || 'Erreur lors de la configuration du mot de passe.');
        this.loading = false;
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
