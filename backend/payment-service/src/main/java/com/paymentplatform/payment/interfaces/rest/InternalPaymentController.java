package com.paymentplatform.payment.interfaces.rest;

import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.application.dto.SupplierPaymentSummaryResponse;
import com.paymentplatform.payment.application.usecase.CreatePaymentUseCase;
import com.paymentplatform.payment.application.usecase.ListPaymentsUseCase;
import com.paymentplatform.payment.infrastructure.http.PaymentInternalAuthGuard;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint interne (secret partagé) pour la création de paiements par les
 * autres microservices — ex. paiement auto à réception de commande confirmée.
 * Le header {@code X-Internal-Token} est obligatoire (401 unifié, cf. B3) et
 * {@code X-Actor-User-Id} désigne l'utilisateur à l'origine de l'action.
 */
@RestController
@RequestMapping("/api/internal/payments")
public class InternalPaymentController {

    private final CreatePaymentUseCase createPayment;
    private final ListPaymentsUseCase listPayments;
    private final PaymentInternalAuthGuard guard;

    public InternalPaymentController(CreatePaymentUseCase createPayment,
                                     ListPaymentsUseCase listPayments,
                                     PaymentInternalAuthGuard guard) {
        this.createPayment = createPayment;
        this.listPayments = listPayments;
        this.guard = guard;
    }

    /**
     * Crée un paiement automatique pour une commande à réception confirmée
     * (appel inter-services).
     *
     * @param token secret interne partagé
     * @param actorUserId identifiant de l'utilisateur à l'origine de l'action
     * @param request requête de création validée
     * @return paiement créé (201)
     */
    @PostMapping("/auto")
    public ResponseEntity<PaymentResponse> auto(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestHeader(value = "X-Actor-User-Id", required = false) String actorUserId,
            @Valid @RequestBody CreatePaymentRequest request) {
        if (!guard.isValid(token)) {
            throw new UnauthorizedException("Secret interne invalide ou manquant");
        }
        UUID actor;
        try {
            actor = UUID.fromString(actorUserId);
        } catch (Exception e) {
            throw new UnauthorizedException("Acteur interne invalide ou manquant");
        }
        // Idempotence portée par la commande : un rejeu (livraison + accept-asap)
        // retourne le paiement existant au lieu d'un doublon (cf. B1).
        String idempotencyKey = request.orderId() != null ? "asap-" + request.orderId() : null;
        return ResponseEntity.status(201).body(
                createPayment.execute(request, actor, request.shopId(), idempotencyKey));
    }

    /**
     * Synthèse des totaux par statut pour un fournisseur (rapport financier
     * organization-service). Même secret partagé que {@link #auto}.
     *
     * @param token secret interne partagé
     * @param supplierId fournisseur concerné
     * @return totaux et compteurs par statut
     */
    @GetMapping("/supplier-summary")
    public ResponseEntity<SupplierPaymentSummaryResponse> supplierSummary(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestParam UUID supplierId) {
        if (!guard.isValid(token)) {
            throw new UnauthorizedException("Secret interne invalide ou manquant");
        }
        return ResponseEntity.ok(listPayments.summarizeBySupplier(supplierId));
    }
}
