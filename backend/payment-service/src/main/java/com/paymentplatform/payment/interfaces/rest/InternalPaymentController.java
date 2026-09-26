package com.paymentplatform.payment.interfaces.rest;

import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.application.usecase.CreatePaymentUseCase;
import com.paymentplatform.payment.infrastructure.http.PaymentInternalAuthGuard;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Endpoint interne (secret partagé) pour la création de paiements par les
 * autres microservices — ex. paiement auto des commandes ASAP à la livraison.
 * Le header {@code X-Internal-Token} est obligatoire (401 unifié, cf. B3) et
 * {@code X-Actor-User-Id} désigne l'utilisateur à l'origine de l'action.
 */
@RestController
@RequestMapping("/api/internal/payments")
public class InternalPaymentController {

    private final CreatePaymentUseCase createPayment;
    private final PaymentInternalAuthGuard guard;

    public InternalPaymentController(CreatePaymentUseCase createPayment,
                                      PaymentInternalAuthGuard guard) {
        this.createPayment = createPayment;
        this.guard = guard;
    }

    /**
     * Crée un paiement automatique ASAP pour une commande livrée (appel inter-services).
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
}
