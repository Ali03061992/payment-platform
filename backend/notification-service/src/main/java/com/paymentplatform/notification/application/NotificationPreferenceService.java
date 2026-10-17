package com.paymentplatform.notification.application;

import com.paymentplatform.notification.domain.model.NotificationPreference;
import com.paymentplatform.notification.domain.model.NotificationPreferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Lecture/mise à jour des préférences de notification par utilisateur. */
@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;

    public NotificationPreferenceService(NotificationPreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public NotificationPreference getOrCreate(UUID userId) {
        return repository.findByUserId(userId)
                .orElseGet(() -> repository.save(new NotificationPreference(userId)));
    }

    @Transactional
    public NotificationPreference update(UUID userId, boolean pushEnabled, boolean emailEnabled) {
        NotificationPreference pref = getOrCreate(userId);
        pref.update(pushEnabled, emailEnabled);
        return repository.save(pref);
    }

    @Transactional(readOnly = true)
    public boolean isPushEnabled(UUID userId) {
        return repository.findByUserId(userId).map(NotificationPreference::pushEnabled).orElse(true);
    }
}
