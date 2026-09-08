package com.paymentplatform.notification.domain.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientUserIdOrderByCreatedAtDesc(Long recipientUserId);

    List<Notification> findByRecipientOrganizationIdOrderByCreatedAtDesc(Long recipientOrganizationId);

    List<Notification> findByRecipientUserIdAndReadStatusOrderByCreatedAtDesc(Long recipientUserId, String readStatus);

    List<Notification> findByRecipientOrganizationIdAndReadStatusOrderByCreatedAtDesc(Long recipientOrganizationId, String readStatus);

    long countByRecipientUserIdAndReadStatus(Long recipientUserId, String readStatus);

    long countByRecipientOrganizationIdAndReadStatus(Long recipientOrganizationId, String readStatus);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readStatus = 'READ' " +
           "WHERE n.recipientUserId = :userId AND n.readStatus = 'UNREAD'")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.readStatus = 'READ' " +
           "WHERE n.recipientOrganizationId = :orgId AND n.readStatus = 'UNREAD'")
    int markAllAsReadByOrgId(@Param("orgId") Long orgId);
}
