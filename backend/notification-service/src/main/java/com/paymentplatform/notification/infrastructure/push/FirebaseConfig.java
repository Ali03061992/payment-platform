package com.paymentplatform.notification.infrastructure.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.push.enabled:false}")
    private boolean pushEnabled;

    @Value("${app.push.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void init() {
        if (!pushEnabled) {
            log.info("Push notifications disabled, skipping Firebase initialization");
            return;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount;
                if (!credentialsPath.isEmpty() && Files.exists(Path.of(credentialsPath))) {
                    serviceAccount = Files.newInputStream(Path.of(credentialsPath));
                } else {
                    serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                }

                if (serviceAccount == null) {
                    log.warn("Firebase service account not found, push notifications will be disabled");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!pushEnabled || FirebaseApp.getApps().isEmpty()) {
            return null;
        }
        return FirebaseMessaging.getInstance();
    }
}
