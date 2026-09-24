package com.paymentplatform.notification.infrastructure.email;

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
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateService templateService;

    @Value("${app.mail.from:noreply@payment-platform.local}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:8080}")
    private String frontendUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public EmailNotificationService(JavaMailSender mailSender, EmailTemplateService templateService) {
        this.mailSender = mailSender;
        this.templateService = templateService;
    }

    @Async
    public void sendOrderConfirmation(String to, String recipientName, String orderReference, String orderDetails) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping order confirmation to {}", to);
            return;
        }
        String subject = "Payment Platform - Commande " + orderReference + " confirmée";
        String htmlBody = templateService.buildOrderConfirmation(recipientName, orderReference, orderDetails);
        sendHtmlEmail(to, subject, htmlBody);
    }

    @Async
    public void sendDeliveryNotification(String to, String recipientName, String orderReference, String deliveryInfo) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping delivery notification to {}", to);
            return;
        }
        String subject = "Payment Platform - Commande " + orderReference + " en livraison";
        String htmlBody = templateService.buildDeliveryNotification(recipientName, orderReference, deliveryInfo);
        sendHtmlEmail(to, subject, htmlBody);
    }

    @Async
    public void sendPaymentReceipt(String to, String recipientName, String paymentReference, String amount, String currency, String orderReference) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping payment receipt to {}", to);
            return;
        }
        String subject = "Payment Platform - Reçu de paiement " + paymentReference;
        String htmlBody = templateService.buildPaymentReceipt(recipientName, paymentReference, amount, currency, orderReference);
        sendHtmlEmail(to, subject, htmlBody);
    }

    @Async
    public void sendAgentInvitation(String to, String recipientName, String organizationName) {
        if (!mailEnabled) {
            log.debug("Mail disabled, skipping agent invitation to {}", to);
            return;
        }
        String invitationLink = frontendUrl + "/accept-invitation?email=" + to;
        String subject = "Payment Platform - Invitation en tant qu'agent livreur";
        String htmlBody = templateService.buildAgentInvitation(recipientName, organizationName, invitationLink);
        sendHtmlEmail(to, subject, htmlBody);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email envoyé à {} : {}", to, subject);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email à {} : {}", to, e.getMessage());
        }
    }
}
