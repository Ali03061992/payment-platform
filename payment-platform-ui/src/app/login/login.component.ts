import { AfterViewInit, Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { PushNotificationService } from '../services/push-notification.service';
import { LoginResponse } from '../models/user.model';

function runtimeEnv(name: string): string {
  try {
    const w = window as unknown as { __env?: Record<string, string> };
    return w.__env?.[name] ?? '';
  } catch {
    return '';
  }
}

export function isDevMode(): boolean {
  return runtimeEnv('IS_DEV') === 'true';
}

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    standalone: false
})
export class LoginComponent implements AfterViewInit {
  form = { username: '', password: '' };
  error = '';
  loading = false;

  oauthError = '';
  oauthLoading: '' | 'GOOGLE' | 'MICROSOFT' = '';
  googleReady = false;
  microsoftReady = false;

  devUsername = '';
  devLoading = false;

  constructor(
    private loginService: LoginService,
    private router: Router,
    private notificationService: NotificationService,
    private pushNotificationService: PushNotificationService
  ) {}

  ngAfterViewInit(): void {
    this.initOAuthButtons();
  }

  isDev(): boolean {
    return isDevMode();
  }

  get googleClientId(): string {
    return runtimeEnv('GOOGLE_CLIENT_ID');
  }

  get microsoftClientId(): string {
    return runtimeEnv('MICROSOFT_CLIENT_ID');
  }

  get microsoftTenantId(): string {
    return runtimeEnv('MICROSOFT_TENANT_ID');
  }

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    this.loginService.login(this.form).subscribe({
      next: (res) => this.afterAuth(res),
      error: (err) => {
        this.error = err.error?.message || 'Identifiants invalides';
        this.loading = false;
      }
    });
  }

  devLogin(): void {
    if (!this.devUsername.trim()) return;
    this.error = '';
    this.devLoading = true;
    this.loginService.devLogin(this.devUsername.trim()).subscribe({
      next: (res) => { this.devLoading = false; this.afterAuth(res); },
      error: (err) => {
        this.error = err.error?.message || 'Utilisateur dev introuvable';
        this.devLoading = false;
      }
    });
  }

  loginWithGoogle(): void {
    if (!this.googleReady || this.oauthLoading) return;
    this.oauthError = '';
    this.oauthLoading = 'GOOGLE';
    this.loadScript('https://accounts.google.com/gsi/client').then(() => {
      const google = (window as any).google;
      if (!google?.accounts?.id) throw new Error('GIS indisponible');
      google.accounts.id.initialize({
        client_id: this.googleClientId,
        callback: (resp: any) => this.oauth('GOOGLE', resp?.credential),
      });
      google.accounts.id.prompt();
      this.oauthLoading = '';
    }).catch(() => {
      this.googleReady = false;
      this.oauthError = 'Google indisponible (réseau ?)';
      this.oauthLoading = '';
    });
  }

  async loginWithMicrosoft(): Promise<void> {
    if (!this.microsoftReady || this.oauthLoading) return;
    this.oauthError = '';
    this.oauthLoading = 'MICROSOFT';
    try {
      await this.loadScript('https://cdn.jsdelivr.net/npm/@azure/msal-browser@3/lib/msal-browser.min.js');
      const msal = (window as any).msal;
      if (!msal?.PublicClientApplication) throw new Error('MSAL indisponible');
      const app = new msal.PublicClientApplication({
        auth: {
          clientId: this.microsoftClientId,
          authority: `https://login.microsoftonline.com/${this.microsoftTenantId || 'common'}`,
        },
      });
      await app.initialize();
      const result = await app.loginPopup({ scopes: ['openid', 'profile', 'email'] });
      if (!result?.idToken) throw new Error('Pas de jeton Microsoft');
      this.oauth('MICROSOFT', result.idToken);
    } catch {
      this.microsoftReady = false;
      this.oauthError = 'Microsoft indisponible (réseau ou popup bloquée ?)';
      this.oauthLoading = '';
    }
  }

  private oauth(provider: 'GOOGLE' | 'MICROSOFT', idToken: string): void {
    if (!idToken) {
      this.oauthError = 'Échec OAuth : jeton manquant';
      this.oauthLoading = '';
      return;
    }
    this.loginService.oauth(provider, idToken).subscribe({
      next: (res) => { this.oauthLoading = ''; this.afterAuth(res); },
      error: (err) => {
        this.oauthError = err.error?.message || 'Connexion OAuth refusée';
        this.oauthLoading = '';
      }
    });
  }

  private afterAuth(res: LoginResponse): void {
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
        sessionStorage.removeItem('refreshToken');
        this.error = 'Impossible de récupérer les informations utilisateur';
        this.loading = false;
      }
    });
  }

  private initOAuthButtons(): void {
    if (this.googleClientId) {
      this.loadScript('https://accounts.google.com/gsi/client')
        .then(() => { this.googleReady = !!(window as any).google?.accounts?.id; })
        .catch(() => { this.googleReady = false; });
    }
    if (this.microsoftClientId) {
      this.loadScript('https://cdn.jsdelivr.net/npm/@azure/msal-browser@3/lib/msal-browser.min.js')
        .then(() => { this.microsoftReady = !!(window as any).msal?.PublicClientApplication; })
        .catch(() => { this.microsoftReady = false; });
    }
  }

  private readonly loadedScripts = new Set<string>();

  private loadScript(src: string): Promise<void> {
    if (this.loadedScripts.has(src)) return Promise.resolve();
    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      this.loadedScripts.add(src);
      return Promise.resolve();
    }
    return new Promise<void>((resolve, reject) => {
      const el = document.createElement('script');
      el.src = src;
      el.async = true;
      el.onload = () => { this.loadedScripts.add(src); resolve(); };
      el.onerror = () => reject(new Error('load ' + src));
      document.head.appendChild(el);
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
