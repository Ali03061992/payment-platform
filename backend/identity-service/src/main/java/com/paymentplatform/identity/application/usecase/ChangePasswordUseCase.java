package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.ChangePasswordRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangePasswordUseCase {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder audit;

    public ChangePasswordUseCase(UserRepository users, PasswordEncoder passwordEncoder, AuditRecorder audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        var current = CurrentUser.get();
        User user = users.findById(UserId.of(current.userId()))
                .orElseThrow(() -> new UnauthorizedException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.currentPassword(), user.password().value())) {
            throw new UnauthorizedException("Mot de passe actuel incorrect");
        }

        String encodedNew = passwordEncoder.encode(request.newPassword());
        user.changePassword(PasswordHash.of(encodedNew));
        users.save(user);

        audit.record(user.id().value(),
                user.organizationId() == null ? null : user.organizationId().value(),
                AuditActions.USER_PASSWORD_CHANGE, user.id().value(), null);
    }
}
