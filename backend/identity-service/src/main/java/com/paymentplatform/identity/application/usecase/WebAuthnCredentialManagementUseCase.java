package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.WebAuthnCredentialResponse;
import com.paymentplatform.identity.domain.repository.WebAuthnCredentialRepository;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Gestion des passkeys enregistrées : liste, renommage, révocation (ownership strict). */
@Service
public class WebAuthnCredentialManagementUseCase {

    private final WebAuthnCredentialRepository credentials;
    private final AuditRecorder audit;

    public WebAuthnCredentialManagementUseCase(WebAuthnCredentialRepository credentials, AuditRecorder audit) {
        this.credentials = credentials;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<WebAuthnCredentialResponse> list(UUID userId) {
        return credentials.findByUserId(userId).stream().map(WebAuthnCredentialResponse::from).toList();
    }

    @Transactional
    public void rename(UUID userId, UUID credentialId, String label) {
        if (!credentials.rename(credentialId, userId, label)) {
            throw new NotFoundException("Passkey introuvable");
        }
        audit.record(userId, null, AuditActions.WEBAUTHN_CREDENTIAL_RENAMED, credentialId, null);
    }

    @Transactional
    public void revoke(UUID userId, UUID credentialId) {
        if (!credentials.revoke(credentialId, userId)) {
            throw new NotFoundException("Passkey introuvable");
        }
        audit.record(userId, null, AuditActions.WEBAUTHN_CREDENTIAL_REVOKED, credentialId, null);
    }
}
