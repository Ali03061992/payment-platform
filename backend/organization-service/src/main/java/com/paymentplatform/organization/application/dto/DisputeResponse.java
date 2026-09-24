package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID orderId,
        UUID shopId,
        UUID supplierId,
        UUID openedBy,
        String status,
        String reason,
        List<DisputeMessageResponse> messages,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static DisputeResponse from(Dispute dispute) {
        List<DisputeMessageResponse> msgs = dispute.getMessages().stream()
                .map(DisputeMessageResponse::from)
                .toList();
        return new DisputeResponse(
                dispute.getId(),
                dispute.getOrderId(),
                dispute.getShopId(),
                dispute.getSupplierId(),
                dispute.getOpenedBy(),
                dispute.getStatus(),
                dispute.getReason(),
                msgs,
                dispute.getVersion(),
                dispute.getCreatedAt(),
                dispute.getUpdatedAt()
        );
    }

    public record DisputeMessageResponse(
            UUID id,
            UUID senderId,
            String senderRole,
            String content,
            Instant timestamp
    ) {
        public static DisputeMessageResponse from(DisputeMessage msg) {
            return new DisputeMessageResponse(
                    msg.getId(),
                    msg.getSenderId(),
                    msg.getSenderRole(),
                    msg.getContent(),
                    msg.getTimestamp()
            );
        }
    }
}
