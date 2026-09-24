import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { PushNotificationService } from '../services/push-notification.service';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    standalone: false
})
export class LoginComponent {
  form = { username: '', password: '' };
  error = '';
  loading = false;

  constructor(
    private loginService: LoginService,
    private router: Router,
    private notificationService: NotificationService,
    private pushNotificationService: PushNotificationService
  ) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.loginService.login(this.form).subscribe({
      next: (res) => {
        sessionStorage.setItem('token', res.accessToken);
        this.loginService.getMe().subscribe({
          next: (user) => {
            sessionStorage.setItem('user', JSON.stringify(user));
            this.loading = false;
            this.router.navigate(['/dashboard']);
            this.requestNotificationPermission();
            this.pushNotificationService.requestPermissionAndGetToken();
            this.pushNotificationService.listenToMessages();
          },
          error: () => {
            sessionStorage.removeItem('token');
            this.error = 'Impossible de récupérer les informations utilisateur';
            this.loading = false;
          }
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Identifiants invalides';
        this.loading = false;
      }
    });
  }

  private requestNotificationPermission(): void {
    if ('Notification' in window && Notification.permission === 'default') {
      // Wait a moment before requesting permission to avoid blocking the UI
      setTimeout(() => {
        this.notificationService.requestPermission();
      }, 2000);
    }
  }
}
