import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { ConfirmDialogService } from '../components/confirm-dialog/confirm-dialog.service';
import { NotificationPreferences, WebAuthnCredential } from '../models/passkey.model';
import { ToastService } from '../services/toast.service';
import { WebAuthnService } from '../services/web-authn.service';
import { WebPushService } from '../services/web-push.service';

/**
 * Page Sécurité : gestion des passkeys (ajout / renommage / révocation)
 * et des notifications push (abonnement Web Push + préférences).
 */
@Component({
    selector: 'app-security',
    templateUrl: './security.component.html',
    styleUrls: ['./security.component.css'],
    standalone: false
})
export class SecurityComponent implements OnInit {

  credentials: WebAuthnCredential[] = [];
  credentialsLoading = false;
  passkeySupported = false;

  addVisible = false;
  newLabel = '';
  registering = false;

  renamingId: string | null = null;
  renameValue = '';
  renaming = false;

  pushSupported = false;
  pushEnabled = false;
  pushLoading = false;

  preferences: NotificationPreferences = { pushEnabled: true, emailEnabled: false };
  preferencesSaving = false;

  error = '';

  constructor(
    private webAuthn: WebAuthnService,
    private webPush: WebPushService,
    private toast: ToastService,
    private dialogs: ConfirmDialogService,
    private translate: TranslateService
  ) {
    this.passkeySupported = WebAuthnService.isSupported();
    this.pushSupported = this.webPush.isSupported();
  }

  ngOnInit(): void {
    this.loadCredentials();
    if (this.pushSupported) {
      this.loadPushState();
    }
  }

  // ---------- Passkeys ----------

  loadCredentials(): void {
    this.credentialsLoading = true;
    this.webAuthn.list().subscribe({
      next: (list) => {
        this.credentials = list;
        this.credentialsLoading = false;
      },
      error: () => {
        this.credentialsLoading = false;
        this.error = this.translate.instant('SECURITY.ERROR_GENERIC');
      }
    });
  }

  showAddForm(): void {
    this.error = '';
    this.addVisible = true;
    this.newLabel = '';
  }

  cancelAdd(): void {
    this.addVisible = false;
    this.newLabel = '';
  }

  addPasskey(): void {
    const label = this.newLabel.trim();
    if (!label) {
      this.error = this.translate.instant('SECURITY.LABEL_REQUIRED');
      return;
    }
    this.error = '';
    this.registering = true;
    this.webAuthn.register(label).subscribe({
      next: (credential) => {
        this.registering = false;
        this.addVisible = false;
        this.newLabel = '';
        this.credentials = [...this.credentials, credential];
        this.toast.show(this.translate.instant('SECURITY.REGISTER_TOAST'), 'success');
      },
      error: (err) => {
        this.registering = false;
        this.error = this.errorMessage(err, 'SECURITY.REGISTER_ERROR');
      }
    });
  }

  startRename(credential: WebAuthnCredential): void {
    this.error = '';
    this.renamingId = credential.id;
    this.renameValue = credential.label;
  }

  cancelRename(): void {
    this.renamingId = null;
    this.renameValue = '';
  }

  saveRename(credential: WebAuthnCredential): void {
    const label = this.renameValue.trim();
    if (!label) {
      this.error = this.translate.instant('SECURITY.LABEL_REQUIRED');
      return;
    }
    this.error = '';
    this.renaming = true;
    this.webAuthn.rename(credential.id, label).subscribe({
      next: () => {
        this.renaming = false;
        this.renamingId = null;
        this.renameValue = '';
        this.credentials = this.credentials.map((c) =>
          c.id === credential.id ? { ...c, label } : c);
        this.toast.show(this.translate.instant('SECURITY.RENAME_TOAST'), 'success');
      },
      error: (err) => {
        this.renaming = false;
        this.error = err?.error?.message || this.translate.instant('SECURITY.ERROR_GENERIC');
      }
    });
  }

  revoke(credential: WebAuthnCredential): void {
    this.dialogs.confirm({
      title: this.translate.instant('SECURITY.REVOKE_TITLE'),
      message: this.translate.instant('SECURITY.REVOKE_MESSAGE', { label: credential.label }),
      confirmLabel: this.translate.instant('SECURITY.REVOKE_CONFIRM'),
      danger: true
    }).subscribe((confirmed) => {
      if (!confirmed) {
        return;
      }
      this.webAuthn.revoke(credential.id).subscribe({
        next: () => {
          this.credentials = this.credentials.filter((c) => c.id !== credential.id);
          this.toast.show(this.translate.instant('SECURITY.REVOKE_TOAST'), 'success');
        },
        error: (err) => {
          this.error = err?.error?.message || this.translate.instant('SECURITY.ERROR_GENERIC');
        }
      });
    });
  }

  // ---------- Push ----------

  private loadPushState(): void {
    this.webPush.currentSubscription().subscribe((subscription) => {
      this.pushEnabled = !!subscription;
    });
    this.webPush.getPreferences().subscribe({
      next: (preferences) => (this.preferences = preferences),
      error: () => undefined
    });
  }

  togglePush(): void {
    if (this.pushLoading) {
      return;
    }
    this.error = '';
    this.pushLoading = true;
    const operation = this.pushEnabled ? this.webPush.disable() : this.webPush.enable();
    operation.subscribe({
      next: () => {
        this.pushEnabled = !this.pushEnabled;
        this.pushLoading = false;
        const key = this.pushEnabled ? 'SECURITY.PUSH_ENABLED_TOAST' : 'SECURITY.PUSH_DISABLED_TOAST';
        this.toast.show(this.translate.instant(key), 'success');
      },
      error: (err) => {
        this.pushLoading = false;
        this.error = this.errorMessage(err, 'SECURITY.PUSH_ERROR');
      }
    });
  }

  savePreferences(): void {
    if (this.preferencesSaving) {
      return;
    }
    this.error = '';
    this.preferencesSaving = true;
    this.webPush.updatePreferences(this.preferences).subscribe({
      next: (preferences) => {
        this.preferences = preferences;
        this.preferencesSaving = false;
        this.toast.show(this.translate.instant('SECURITY.PREF_SAVED_TOAST'), 'success');
      },
      error: () => {
        this.preferencesSaving = false;
        this.error = this.translate.instant('SECURITY.ERROR_GENERIC');
      }
    });
  }

  // ---------- Helpers ----------

  /** Traduit les erreurs WebAuthn/Push (annulation navigateur, service worker absent...). */
  private errorMessage(err: unknown, fallbackKey: string): string {
    const name = (err as { name?: string })?.name;
    if (name === 'NotAllowedError') {
      return this.translate.instant('SECURITY.ACTION_CANCELLED');
    }
    if (name === 'SecurityError' && typeof window !== 'undefined' && window.isSecureContext) {
      // SecurityError sur contexte déjà HTTPS = RP ID refusé par le navigateur
      // (notamment accès par IP : un nom de domaine réel est exigé, pas le HTTPS).
      return this.translate.instant('SECURITY.ACTION_BLOCKED_DOMAIN');
    }
    if (name === 'NotSupportedError' || name === 'SecurityError') {
      return this.translate.instant('SECURITY.ACTION_BLOCKED');
    }
    const backendMessage = (err as { error?: { message?: string } })?.error?.message;
    return backendMessage || this.translate.instant(fallbackKey);
  }
}
