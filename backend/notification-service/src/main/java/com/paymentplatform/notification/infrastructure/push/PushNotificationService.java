package com.paymentplatform.notification.infrastructure.push;

import com.google.firebase.messaging.*;
import com.paymentplatform.notification.domain.model.FcmToken;
import com.paymentplatform.notification.domain.model.FcmTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final FcmTokenRepository fcmTokenRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    @Value("${app.push.enabled:false}")
    private boolean pushEnabled;

    public PushNotificationService(FcmTokenRepository fcmTokenRepository,
                                    ObjectProvider<FirebaseMessaging> firebaseMessagingProvider) {
        this.fcmTokenRepository = fcmTokenRepository;
        this.firebaseMessagingProvider = firebaseMessagingProvider;
    }

    @Async
    public void sendToUser(UUID userId, String title, String body, String tag, String url) {
        if (!pushEnabled) {
            log.debug("Push notifications disabled, skipping for user {}", userId);
            return;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.debug("FirebaseMessaging unavailable, skipping push for user {}", userId);
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No FCM tokens found for user {}", userId);
            return;
        }

        for (FcmToken fcmToken : tokens) {
            sendToDevice(firebaseMessaging, fcmToken.token(), title, body, tag, url);
        }
    }

    private void sendToDevice(FirebaseMessaging firebaseMessaging, String token, String title, String body, String tag, String url) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("tag", tag != null ? tag : "payment-notification")
                    .putData("url", url != null ? url : "/")
                    .setAndroidConfig(AndroidConfig.builder()
                            .setTtl(3600000)
                            .setNotification(AndroidNotification.builder()
                                    .setTag(tag != null ? tag : "payment-notification")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setBadge(1)
                                    .setSound("default")
                                    .build())
                            .build())
                    .build();

            String messageId = firebaseMessaging.send(message);
            log.debug("Push notification sent to token {}: messageId={}", token.substring(0, 10) + "...", messageId);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send push notification to token {}: {}", token.substring(0, 10) + "...", e.getMessage());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                fcmTokenRepository.deleteByToken(token);
                log.info("Removed invalid FCM token: {}", token.substring(0, 10) + "...");
            }
        }
    }
}
