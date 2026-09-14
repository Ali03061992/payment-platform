import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

@Component({
  selector: 'app-change-password',
  templateUrl: './change-password.component.html',
  styleUrls: ['./change-password.component.css']
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
    private router: Router
  ) {}

  onSubmit(): void {
    this.error = '';
    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.error = 'Tous les champs sont requis';
      return;
    }
    if (this.newPassword.length < 8) {
      this.error = 'Le nouveau mot de passe doit contenir au moins 8 caractères';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.error = 'Les mots de passe ne correspondent pas';
      return;
    }
    this.loading = true;
    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.submitted = true;
        this.toastService.show('Mot de passe modifié avec succès', 'success');
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.message || 'Erreur lors de la modification du mot de passe';
      }
    });
  }

  goToDashboard(): void {
    this.router.navigate(['/dashboard']);
  }
}
