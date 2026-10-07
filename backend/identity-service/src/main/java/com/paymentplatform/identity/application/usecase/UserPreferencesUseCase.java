package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.identity.application.dto.UpdatePreferencesRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferencesUseCase {

    private final UserRepository users;

    public UserPreferencesUseCase(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public UserResponse updatePreferences(UpdatePreferencesRequest request) {
        User user = users.findById(UserId.of(CurrentUser.id()))
                .orElseThrow(() -> new UnauthorizedException("Utilisateur non trouvé"));
        if (request.preferredLang() != null || request.accentColor1() != null || request.accentColor2() != null) {
            user.updatePreferences(
                    request.preferredLang() != null ? request.preferredLang() : user.preferredLang(),
                    request.accentColor1() != null ? request.accentColor1() : user.accentColor1(),
                    request.accentColor2() != null ? request.accentColor2() : user.accentColor2());
        }
        if (Boolean.TRUE.equals(request.tourSeen())) {
            user.markTourSeen();
        }
        return UserResponse.from(users.save(user));
    }
}
