package com.paymentplatform.notification.domain.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    List<Notification> findByRecipientOrganizationIdOrderByCreatedAtDesc(UUID recipientOrganizationId);

    List<Notification> findByRecipientUserIdAndReadStatusOrderByCreatedAtDesc(UUID recipientUserId, String readStatus);

    List<Notification> findByRecipientOrganizationIdAndReadStatusOrderByCreatedAtDesc(UUID recipientOrganizationId, String readStatus);

    long countByRecipientUserIdAndReadStatus(UUID recipientUserId, String readStatus);

    long countByRecipientOrganizationIdAndReadStatus(UUID recipientOrganizationId, String readStatus);

    Page<Notification> findByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Page<Notification> findByRecipientOrganizationIdOrderByCreatedAtDesc(UUID recipientOrganizationId, Pageable pageable);

    Page<Notification> findByRecipientUserIdAndTypeOrderByCreatedAtDesc(UUID recipientUserId, String type, Pageable pageable);

    Page<Notification> findByRecipientOrganizationIdAndTypeOrderByCreatedAtDesc(UUID recipientOrganizationId, String type, Pageable pageable);

    long countByRecipientUserIdAndType(UUID recipientUserId, String type);

    long countByRecipientOrganizationIdAndType(UUID recipientOrganizationId, String type);

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
