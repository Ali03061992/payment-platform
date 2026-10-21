import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { LoginService } from '../services/login.service';
import { PushNotificationService } from '../services/push-notification.service';
import { ThemeService } from '../services/theme.service';
import { WebAuthnService } from '../services/web-authn.service';
import { BRAND } from '../core/brand.config';

@Component({
    selector: 'app-login',
    templateUrl: './login.component.html',
    styleUrls: ['./login.component.css'],
    standalone: false
})
export class LoginComponent implements OnInit {
  readonly brand = BRAND;
  form = { username: '', password: '' };
  error = '';
  loading = false;
  passkeyLoading = false;
  passkeySupported = false;

  constructor(
    private loginService: LoginService,
    private router: Router,
    private route: ActivatedRoute,
    private pushNotificationService: PushNotificationService,
    private webAuthn: WebAuthnService,
    private translate: TranslateService,
    private themeService: ThemeService
  ) {}

  ngOnInit(): void {
    void WebAuthnService.isAvailable().then((available) => this.passkeySupported = available);
  }

  onSubmit(): void {
    this.error = '';
    this.loading = true;
    // login() adopte la session (access token mémoire, cookie HttpOnly côté serveur).
    this.loginService.login(this.form).subscribe({
      next: () => this.afterLogin(),
      error: (err) => {
        this.error = err.error?.message || this.translate.instant('AUTH.INVALID_CREDENTIALS');
        this.loading = false;
      }
    });
  }

  /** Connexion par passkey discoverable : l'empreinte/biométrie est demandée directement, sans saisie de username. */
  onPasskeyLogin(): void {
    this.error = '';
    this.passkeyLoading = true;
    this.webAuthn.login().subscribe({
      next: (res) => {
        this.loginService.adoptSession(res);
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
        this.loginService.setCurrentUser(user);
        // Applique immédiatement les 2 accents de l'utilisateur (sinon ils
        // n'apparaissaient qu'au prochain reload).
        this.themeService.refreshUserAccents();
        this.loading = false;
        this.passkeyLoading = false;
        this.router.navigateByUrl(this.safeReturnUrl() || '/dashboard');
        // Opt-in push explicite (page Sécurité) : plus aucune demande de permission
        // automatique au login. On se contente d'écouter les messages au premier plan.
        this.pushNotificationService.listenToMessages();
      },
      error: () => {
        this.loginService.purgeSession();
        this.error = this.translate.instant('AUTH.USER_FETCH_ERROR');
        this.loading = false;
        this.passkeyLoading = false;
      }
    });
  }

  /**
   * Destination demandée avant redirection login (deep-link après notif) :
   * uniquement les chemins internes (pas de // ni de schéma) — anti open-redirect.
   */
  private safeReturnUrl(): string | null {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    if (!returnUrl) return null;
    if (!returnUrl.startsWith('/') || returnUrl.startsWith('//')) return null;
    return returnUrl;
  }
}
