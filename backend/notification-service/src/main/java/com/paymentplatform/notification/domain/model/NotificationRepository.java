package com.paymentplatform.notification.domain.model;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    List<Notification> findByRecipientOrganizationIdOrderByCreatedAtDesc(UUID recipientOrganizationId);

    List<Notification> findByRecipientUserIdAndReadStatusOrderByCreatedAtDesc(UUID recipientUserId, String readStatus);

    List<Notification> findByRecipientOrganizationIdAndReadStatusOrderByCreatedAtDesc(UUID recipientOrganizationId, String readStatus);

    long countByRecipientUserIdAndReadStatus(UUID recipientUserId, String readStatus);

    long countByRecipientOrganizationIdAndReadStatus(UUID recipientOrganizationId, String readStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readStatus = 'READ' " +
           "WHERE n.recipientUserId = :userId AND n.readStatus = 'UNREAD'")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readStatus = 'READ' " +
           "WHERE n.recipientOrganizationId = :orgId AND n.readStatus = 'UNREAD'")
    int markAllAsReadByOrgId(@Param("orgId") UUID orgId);
}
