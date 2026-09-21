package com.paymentplatform.identity.infrastructure.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@payment-platform.local}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendPasswordSetupEmail(String to, String firstName, String token) {
        String link = frontendUrl + "/setup-password?token=" + token;
        String subject = "Payment Platform - Configuration de votre mot de passe";
        String htmlBody = buildPasswordSetupHtml(firstName, link);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email de configuration mot de passe envoyé à {}", to);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email à {}: {}", to, e.getMessage());
        }
    }

    private String buildPasswordSetupHtml(String firstName, String link) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; }
                        .header h1 { color: white; margin: 0; font-size: 24px; }
                        .content { padding: 30px; color: #333; line-height: 1.6; }
                        .button { display: inline-block; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; text-decoration: none; padding: 14px 32px; border-radius: 6px; font-weight: bold; margin: 20px 0; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                        .warning { background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; padding: 12px; margin: 15px 0; color: #856404; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Platform</h1>
                        </div>
                        <div class="content">
                            <h2>Bonjour %s,</h2>
                            <p>Votre compte a été créé avec succès. Veuillez configurer votre mot de passe en cliquant sur le bouton ci-dessous :</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Configurer mon mot de passe</a>
                            </p>
                            <div class="warning">
                                <strong>⚠️ Ce lien expire dans 24 heures.</strong>
                            </div>
                            <p>Si vous n'avez pas demandé la création de ce compte, veuillez ignorer cet email.</p>
                        </div>
                        <div class="footer">
                            <p>© 2026 Payment Platform. Tous droits réservés.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, link);
    }
}
