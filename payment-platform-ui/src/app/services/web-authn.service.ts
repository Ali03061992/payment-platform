import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, defer, from } from 'rxjs';
import { firstValueFrom } from 'rxjs';
import { LoginResponse } from '../models/user.model';
import {
  PublicKeyCredentialCreationOptionsJSON,
  PublicKeyCredentialRequestOptionsJSON,
  WebAuthnCredential,
  WebAuthnCredentialPayload
} from '../models/passkey.model';

/** base64url (sans padding) -> ArrayBuffer, pour alimenter les API WebAuthn. */
function base64UrlToBuffer(value: string): ArrayBuffer {
  const padding = '='.repeat((4 - (value.length % 4)) % 4);
  const base64 = (value + padding).replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/** ArrayBuffer -> base64url (sans padding), format attendu par le backend. */
function bufferToBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  bytes.forEach((b) => (binary += String.fromCharCode(b)));
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** Convertit les options JSON du backend en PublicKeyCredentialCreationOptions utilisable par le navigateur. */
function toCreationPublicKey(raw: PublicKeyCredentialCreationOptionsJSON): CredentialCreationOptions['publicKey'] {
  return {
    ...raw,
    challenge: base64UrlToBuffer(raw.challenge),
    user: { ...raw.user, id: base64UrlToBuffer(raw.user.id) },
    excludeCredentials: raw.excludeCredentials?.map((cred) => ({
      ...cred,
      id: base64UrlToBuffer(cred.id)
    }))
  } as unknown as CredentialCreationOptions['publicKey'];
}

/** Convertit les options JSON du backend en PublicKeyCredentialRequestOptions utilisable par le navigateur. */
function toRequestPublicKey(raw: PublicKeyCredentialRequestOptionsJSON): CredentialRequestOptions['publicKey'] {
  return {
    ...raw,
    challenge: base64UrlToBuffer(raw.challenge),
    allowCredentials: raw.allowCredentials?.map((cred) => ({
      ...cred,
      id: base64UrlToBuffer(cred.id)
    }))
  } as unknown as CredentialRequestOptions['publicKey'];
}

/** Re-encode la réponse d'attestation (navigator.credentials.create) pour le backend. */
function toAttestationPayload(credential: PublicKeyCredential): WebAuthnCredentialPayload {
  const response = credential.response as AuthenticatorAttestationResponse;
  const transports = typeof response.getTransports === 'function' ? response.getTransports() : [];
  const payload: WebAuthnCredentialPayload = {
    id: credential.id,
    rawId: bufferToBase64Url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: bufferToBase64Url(response.clientDataJSON),
      attestationObject: bufferToBase64Url(response.attestationObject),
      transports
    },
    clientExtensionResults: credential.getClientExtensionResults() as Record<string, unknown>
  };
  if (credential.authenticatorAttachment) {
    payload.authenticatorAttachment = credential.authenticatorAttachment;
  }
  return payload;
}

/** Re-encode la réponse d'assertion (navigator.credentials.get) pour le backend. */
function toAssertionPayload(credential: PublicKeyCredential): WebAuthnCredentialPayload {
  const response = credential.response as AuthenticatorAssertionResponse;
  return {
    id: credential.id,
    rawId: bufferToBase64Url(credential.rawId),
    type: credential.type,
    response: {
      clientDataJSON: bufferToBase64Url(response.clientDataJSON),
      authenticatorData: bufferToBase64Url(response.authenticatorData),
      signature: bufferToBase64Url(response.signature),
      userHandle: response.userHandle ? bufferToBase64Url(response.userHandle) : null
    },
    clientExtensionResults: credential.getClientExtensionResults() as Record<string, unknown>
  };
}

/**
 * Passkeys / WebAuthn — enregistrement, connexion et gestion des appareils.
 * Les routes /login* sont publiques (rate-limitées au gateway) ; les autres exigent un JWT.
 */
@Injectable({ providedIn: 'root' })
export class WebAuthnService {

  private apiUrl = '/api/auth/webauthn';

  constructor(private http: HttpClient) {}

  /** True si le navigateur implémente WebAuthn (navigator.credentials). */
  static isSupported(): boolean {
    return typeof window !== 'undefined'
      && typeof window.PublicKeyCredential !== 'undefined'
      && !!navigator.credentials;
  }

  /** Démarre l'enregistrement : renvoie les options à passer à navigator.credentials.create. */
  registerStart(): Observable<PublicKeyCredentialCreationOptionsJSON> {
    return this.http.post<PublicKeyCredentialCreationOptionsJSON>(`${this.apiUrl}/register/options`, {});
  }

  /** Confirme l'enregistrement avec le credential créé et un label d'appareil. */
  registerFinish(credential: WebAuthnCredentialPayload, label: string): Observable<WebAuthnCredential> {
    return this.http.post<WebAuthnCredential>(`${this.apiUrl}/register`, { credential, label });
  }

  /** Liste les appareils de l'utilisateur connecté. */
  list(): Observable<WebAuthnCredential[]> {
    return this.http.get<WebAuthnCredential[]>(`${this.apiUrl}/credentials`);
  }

  /** Renomme un appareil. */
  rename(id: string, label: string): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/credentials/${id}`, { label });
  }

  /** Révoque (supprime) un appareil. */
  revoke(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/credentials/${id}`);
  }

  /** Demande les options de connexion (username-first ; sans username = discoverable). */
  loginOptions(username?: string): Observable<PublicKeyCredentialRequestOptionsJSON> {
    const body = username && username.trim() ? { username: username.trim() } : {};
    return this.http.post<PublicKeyCredentialRequestOptionsJSON>(`${this.apiUrl}/login/options`, body);
  }

  /** Envoie l'assertion et échange contre les tokens JWT. */
  finishLogin(credential: WebAuthnCredentialPayload): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, { credential });
  }

  /** Enchaîne start -> navigator.credentials.get -> finish, en une seule observable. */
  login(username?: string): Observable<LoginResponse> {
    return defer(() =>
      from((async () => {
        const options = await firstValueFrom(this.loginOptions(username));
        const credential = await navigator.credentials.get({
          publicKey: toRequestPublicKey(options)
        }) as PublicKeyCredential | null;
        if (!credential) {
          throw new DOMException('Aborted', 'NotAllowedError');
        }
        return firstValueFrom(this.finishLogin(toAssertionPayload(credential)));
      })())
    );
  }

  /** Enchaîne start -> navigator.credentials.create -> finish, en une seule observable. */
  register(label: string): Observable<WebAuthnCredential> {
    return defer(() =>
      from((async () => {
        const options = await firstValueFrom(this.registerStart());
        const credential = await navigator.credentials.create({
          publicKey: toCreationPublicKey(options)
        }) as PublicKeyCredential | null;
        if (!credential) {
          throw new DOMException('Aborted', 'NotAllowedError');
        }
        return firstValueFrom(this.registerFinish(toAttestationPayload(credential), label));
      })())
    );
  }
}
