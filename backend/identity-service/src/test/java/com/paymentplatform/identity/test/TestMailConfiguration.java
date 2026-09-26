package com.paymentplatform.identity.test;

import com.paymentplatform.identity.infrastructure.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
/**
 * Tests de TestMailConfiguration.
 * Perimetre : classe d'aide pour les tests (configuration/stub, pas de test direct).
 * Moyens : JUnit pur (AssertJ).
 */

@Configuration
@Profile("test")
public class TestMailConfiguration {

    private static final Logger log = LoggerFactory.getLogger(TestMailConfiguration.class);

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost("localhost");
        mailSender.setPort(2525);
        return mailSender;
    }

    @Bean
    @Primary
    public EmailService emailService() {
        return new NoOpEmailService();
    }

    static class NoOpEmailService extends EmailService {
        private NoOpEmailService() {
            super(null);
        }

        @Override
        public void sendPasswordSetupEmail(String to, String firstName, String token) {
            log.info("[TEST] Would send password setup email to {} with token={}", to, token);
        }
    }
}
