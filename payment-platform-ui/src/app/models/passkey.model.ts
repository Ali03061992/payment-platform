/** DTO WebAuthn / Passkeys — miroir des contrats identity-service. */

export interface WebAuthnCredential {
  id: string;
  label: string;
  credentialId: string;
  status: 'ACTIVE' | 'REVOKED';
  transports: string[];
  createdAt: string;
  lastUsedAt: string | null;
}

export interface WebAuthnCredentialDescriptorJSON {
  type: 'public-key';
  id: string;
  transports?: string[];
}

/** Options d'enregistrement (reçues de POST /api/auth/webauthn/register/options). */
export interface PublicKeyCredentialCreationOptionsJSON {
  rp: { id: string; name: string };
  user: { id: string; name: string; displayName: string };
  challenge: string;
  pubKeyCredParams: { type: 'public-key'; alg: number }[];
  timeout?: number;
  excludeCredentials?: WebAuthnCredentialDescriptorJSON[];
  authenticatorSelection?: {
    authenticatorAttachment?: 'platform' | 'cross-platform';
    residentKey?: 'required' | 'preferred' | 'discouraged';
    requireResidentKey?: boolean;
    userVerification?: 'required' | 'preferred' | 'discouraged';
  };
  attestation?: 'none' | 'indirect' | 'direct' | 'enterprise';
}

/** Options de connexion (reçues de POST /api/auth/webauthn/login/options). */
export interface PublicKeyCredentialRequestOptionsJSON {
  challenge: string;
  timeout?: number;
  rpId?: string;
  allowCredentials?: WebAuthnCredentialDescriptorJSON[];
  userVerification?: 'required' | 'preferred' | 'discouraged';
}

/** Payload re-encodé (base64url) renvoyé à POST /register ou /login. */
export interface WebAuthnCredentialPayload {
  id: string;
  rawId: string;
  type: string;
  response: {
    clientDataJSON: string;
    attestationObject?: string;
    authenticatorData?: string;
    signature?: string;
    userHandle?: string | null;
    transports?: string[];
  };
  clientExtensionResults?: Record<string, unknown>;
  authenticatorAttachment?: string;
}

/** Abonnement PushManager (format ngsw / Push API). */
export interface PushSubscriptionJSON {
  endpoint: string;
  keys?: { p256dh?: string; auth?: string };
}

/** Préférences de notification (GET/PUT /api/push/preferences). */
export interface NotificationPreferences {
  pushEnabled: boolean;
  emailEnabled: boolean;
}
