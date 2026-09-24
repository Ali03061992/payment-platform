package com.paymentplatform.notification.infrastructure.email;

import org.springframework.stereotype.Service;

@Service
public class EmailTemplateService {

    public String buildOrderConfirmation(String recipientName, String orderReference, String orderDetails) {
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
                        .order-box { background: #f8f9fa; border: 1px solid #dee2e6; border-radius: 6px; padding: 16px; margin: 15px 0; }
                        .status-badge { display: inline-block; background: #28a745; color: white; padding: 4px 12px; border-radius: 12px; font-size: 13px; font-weight: bold; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Platform</h1>
                        </div>
                        <div class="content">
                            <h2>Bonjour %s,</h2>
                            <p>Votre commande a &eacute;t&eacute; confirm&eacute;e avec succ&egrave;s.</p>
                            <div class="order-box">
                                <p><strong>R&eacute;f&eacute;rence :</strong> %s</p>
                                <p><span class="status-badge">CONFIRM&Eacute;E</span></p>
                                %s
                            </div>
                            <p>Vous recevrez une notification d&egrave;s que la commande passera en pr&eacute;paration.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Payment Platform. Tous droits r&eacute;serv&eacute;s.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, orderReference, orderDetails);
    }

    public String buildDeliveryNotification(String recipientName, String orderReference, String deliveryInfo) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                        .header { background: linear-gradient(135deg, #11998e 0%%, #38ef7d 100%%); padding: 30px; text-align: center; }
                        .header h1 { color: white; margin: 0; font-size: 24px; }
                        .content { padding: 30px; color: #333; line-height: 1.6; }
                        .delivery-box { background: #fff3cd; border: 1px solid #ffc107; border-radius: 6px; padding: 16px; margin: 15px 0; }
                        .status-badge { display: inline-block; background: #ffc107; color: #333; padding: 4px 12px; border-radius: 12px; font-size: 13px; font-weight: bold; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Platform</h1>
                        </div>
                        <div class="content">
                            <h2>Bonjour %s,</h2>
                            <p>Votre commande est pr&ecirc;te pour livraison.</p>
                            <div class="delivery-box">
                                <p><strong>R&eacute;f&eacute;rence :</strong> %s</p>
                                <p><span class="status-badge">EN LIVRAISON</span></p>
                                %s
                            </div>
                            <p>Un livreur a &eacute;t&eacute; assign&eacute;. Vous recevrez une confirmation d&egrave;s que la livraison sera termin&eacute;e.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Payment Platform. Tous droits r&eacute;serv&eacute;s.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, orderReference, deliveryInfo);
    }

    public String buildPaymentReceipt(String recipientName, String paymentReference, String amount, String currency, String orderReference) {
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
                        .receipt-box { background: #d4edda; border: 1px solid #28a745; border-radius: 6px; padding: 16px; margin: 15px 0; }
                        .amount { font-size: 28px; font-weight: bold; color: #28a745; text-align: center; margin: 10px 0; }
                        .receipt-detail { display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e9ecef; }
                        .receipt-detail:last-child { border-bottom: none; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Platform</h1>
                        </div>
                        <div class="content">
                            <h2>Bonjour %s,</h2>
                            <p>Votre paiement a &eacute;t&eacute; confirm&eacute;. Voici votre re&ccedil;u.</p>
                            <div class="receipt-box">
                                <div class="amount">%s %s</div>
                                <div class="receipt-detail"><span>R&eacute;f&eacute;rence paiement</span><span>%s</span></div>
                                <div class="receipt-detail"><span>Commande associ&eacute;e</span><span>%s</span></div>
                                <div class="receipt-detail"><span>Statut</span><span>Confirm&eacute;</span></div>
                            </div>
                            <p>Ce re&ccedil;u atteste du paiement valid&eacute;. Conservez-le pour vos archives.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Payment Platform. Tous droits r&eacute;serv&eacute;s.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, amount, currency, paymentReference, orderReference);
    }

    public String buildAgentInvitation(String recipientName, String organizationName, String invitationLink) {
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
                        .warning { background: #fff3cd; border: 1px solid #ffc107; border-radius: 4px; padding: 12px; margin: 15px 0; color: #856404; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Platform</h1>
                        </div>
                        <div class="content">
                            <h2>Bonjour %s,</h2>
                            <p>Vous &ecirc;tes invit&eacute; &agrave; rejoindre <strong>%s</strong> en tant qu'agent livreur sur Payment Platform.</p>
                            <p style="text-align: center;">
                                <a href="%s" class="button">Accepter l'invitation</a>
                            </p>
                            <div class="warning">
                                <strong>Ce lien expire dans 72 heures.</strong>
                            </div>
                            <p>Si vous n'avez pas demand&eacute; cette invitation, veuillez ignorer cet email.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 Payment Platform. Tous droits r&eacute;serv&eacute;s.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(recipientName, organizationName, invitationLink);
    }
}
