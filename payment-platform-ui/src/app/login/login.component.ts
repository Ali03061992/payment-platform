import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { LoginService } from '../services/login.service';
import { PushNotificationService } from '../services/push-notification.service';
import { WebAuthnService } from '../services/web-authn.service';

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
  passkeyLoading = false;
  readonly passkeySupported = WebAuthnService.isSupported();

  constructor(
    private loginService: LoginService,
    private router: Router,
    private pushNotificationService: PushNotificationService,
    private webAuthn: WebAuthnService,
    private translate: TranslateService
  ) {}

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.loginService.login(this.form).subscribe({
      next: (res) => {
        sessionStorage.setItem('token', res.accessToken);
        this.afterLogin();
      },
      error: (err) => {
        this.error = err.error?.message || this.translate.instant('AUTH.INVALID_CREDENTIALS');
        this.loading = false;
      }
    });
  }

  /** Connexion par passkey : options -> navigator.credentials.get -> tokens. */
  onPasskeyLogin(): void {
    this.error = '';
    this.passkeyLoading = true;
    this.webAuthn.login(this.form.username).subscribe({
      next: (res) => {
        sessionStorage.setItem('token', res.accessToken);
        if (res.refreshToken) {
          sessionStorage.setItem('refreshToken', res.refreshToken);
        }
        this.afterLogin();
      },
      error: (err) => {
        this.passkeyLoading = false;
        if (err?.name === 'NotAllowedError') {
          this.error = this.translate.instant('AUTH.PASSKEY_CANCELLED');
        } else {
          this.error = err?.error?.message || this.translate.instant('AUTH.PASSKEY_LOGIN_ERROR');
        }
      }
    });
  }

  private afterLogin(): void {
    this.loginService.getMe().subscribe({
      next: (user) => {
        sessionStorage.setItem('user', JSON.stringify(user));
        this.loading = false;
        this.passkeyLoading = false;
        this.router.navigate(['/dashboard']);
        // Opt-in push explicite (page Sécurité) : plus aucune demande de permission
        // automatique au login. On se contente d'écouter les messages au premier plan.
        this.pushNotificationService.listenToMessages();
      },
      error: () => {
        sessionStorage.removeItem('token');
        this.error = this.translate.instant('AUTH.USER_FETCH_ERROR');
        this.loading = false;
        this.passkeyLoading = false;
      }
    });
  }
}
