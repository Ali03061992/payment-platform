package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeAttachment;
import com.paymentplatform.organization.domain.model.DisputeMessage;
import com.paymentplatform.organization.domain.model.DisputeNote;
import com.paymentplatform.organization.domain.model.DisputeStatusHistory;

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
        String priority,
        Instant dueAt,
        UUID assignedTo,
        List<DisputeMessageResponse> messages,
        List<DisputeStatusHistoryResponse> history,
        List<DisputeAttachmentResponse> attachments,
        List<DisputeNoteResponse> notes,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public static DisputeResponse from(Dispute dispute) {
        return from(dispute, List.of(), List.of(), List.of());
    }

    public static DisputeResponse from(Dispute dispute,
                                       List<DisputeStatusHistory> history,
                                       List<DisputeAttachment> attachments,
                                       List<DisputeNote> notes) {
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
                dispute.getPriority(),
                dispute.getDueAt(),
                dispute.getAssignedTo(),
                msgs,
                history.stream().map(DisputeStatusHistoryResponse::from).toList(),
                attachments.stream().map(DisputeAttachmentResponse::from).toList(),
                notes.stream().map(DisputeNoteResponse::from).toList(),
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

    public record DisputeStatusHistoryResponse(
            UUID id,
            String fromStatus,
            String toStatus,
            UUID changedBy,
            String note,
            Instant changedAt
    ) {
        public static DisputeStatusHistoryResponse from(DisputeStatusHistory history) {
            return new DisputeStatusHistoryResponse(
                    history.getId(),
                    history.getFromStatus(),
                    history.getToStatus(),
                    history.getChangedBy(),
                    history.getNote(),
                    history.getChangedAt()
            );
        }
    }

    public record DisputeAttachmentResponse(
            UUID id,
            UUID messageId,
            String fileName,
            String contentType,
            long sizeBytes,
            UUID uploadedBy,
            Instant createdAt
    ) {
        public static DisputeAttachmentResponse from(DisputeAttachment attachment) {
            return new DisputeAttachmentResponse(
                    attachment.getId(),
                    attachment.getMessageId(),
                    attachment.getFileName(),
                    attachment.getContentType(),
                    attachment.getSizeBytes(),
                    attachment.getUploadedBy(),
                    attachment.getCreatedAt()
            );
        }
    }

    public record DisputeNoteResponse(
            UUID id,
            UUID authorId,
            String content,
            Instant createdAt
    ) {
        public static DisputeNoteResponse from(DisputeNote note) {
            return new DisputeNoteResponse(
                    note.getId(),
                    note.getAuthorId(),
                    note.getContent(),
                    note.getCreatedAt()
            );
        }
    }
}
