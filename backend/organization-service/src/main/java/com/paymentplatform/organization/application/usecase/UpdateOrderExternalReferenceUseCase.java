package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enregistre la référence électronique externe d'une facture (plateforme
 * TTN/El Fatoora pour les e-factures, ou TEJ) : affichée avec QR en haut à
 * droite, comme les outils tunisiens (référence unique + QR de validation).
 */
@Service
public class UpdateOrderExternalReferenceUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;

    public UpdateOrderExternalReferenceUseCase(OrderRepository orders,
                                               OrderItemRepository orderItems) {
        this.orders = orders;
        this.orderItems = orderItems;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, String reference) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        if (reference == null || reference.isBlank()) {
            throw new ConflictException("La référence électronique est obligatoire");
        }
        order.setExternalReference(reference.trim());
        orders.save(order);
        return OrderResponse.from(order, orderItems.findByOrderId(orderId));
    }
}
