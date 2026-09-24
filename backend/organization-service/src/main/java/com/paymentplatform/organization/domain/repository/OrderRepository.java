package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByReference(String reference);

    List<Order> findBySupplierId(UUID supplierId);

    List<Order> findByShopId(UUID shopId);

    List<Order> findBySupplierIdAndStatus(UUID supplierId, String status);

    List<Order> findByShopIdAndStatus(UUID shopId, String status);

    List<Order> findByDeliveryAgentId(UUID deliveryAgentId);

    Page<Order> findBySupplierId(UUID supplierId, Pageable pageable);

    Page<Order> findByShopId(UUID shopId, Pageable pageable);

    Page<Order> findBySupplierIdAndStatus(UUID supplierId, String status, Pageable pageable);

    Page<Order> findByShopIdAndStatus(UUID shopId, String status, Pageable pageable);

    UUID countBySupplierId(UUID supplierId);

    long countBySupplierIdAndStatus(UUID supplierId, String status);

    UUID countByShopId(UUID shopId);

    long countByShopIdAndStatus(UUID shopId, String status);

    @Query(value = "SELECT o.* FROM orders o LEFT JOIN organizations org ON o.shop_id = org.id " +
            "WHERE LOWER(o.reference) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(org.name) LIKE LOWER(CONCAT('%', :query, '%'))",
            countQuery = "SELECT COUNT(*) FROM orders o LEFT JOIN organizations org ON o.shop_id = org.id " +
            "WHERE LOWER(o.reference) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(org.name) LIKE LOWER(CONCAT('%', :query, '%'))",
            nativeQuery = true)
    Page<Order> search(@Param("query") String query, Pageable pageable);

    @Query(value = "SELECT o.* FROM orders o LEFT JOIN organizations org ON o.shop_id = org.id " +
            "WHERE (LOWER(o.reference) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(org.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND o.supplier_id = :supplierId",
            countQuery = "SELECT COUNT(*) FROM orders o LEFT JOIN organizations org ON o.shop_id = org.id " +
            "WHERE (LOWER(o.reference) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(org.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND o.supplier_id = :supplierId",
            nativeQuery = true)
    Page<Order> searchBySupplierId(@Param("query") String query, @Param("supplierId") UUID supplierId, Pageable pageable);

    @Query(value = "SELECT o.* FROM orders o LEFT JOIN organizations org ON o.shop_id = org.id " +
            "WHERE (LOWER(o.reference) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(org.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND o.shop_id = :shopId",
            countQuery = "SELECT COUNT(*) FROM orders o LEFT JOIN organizations org ON o.shop_id = org.id " +
            "WHERE (LOWER(o.reference) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(org.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND o.shop_id = :shopId",
            nativeQuery = true)
    Page<Order> searchByShopId(@Param("query") String query, @Param("shopId") UUID shopId, Pageable pageable);

    List<Order> findTopNByShopIdOrderByCreatedAtDesc(UUID shopId, org.springframework.data.domain.Pageable pageable);

    List<Order> findTopNBySupplierIdOrderByCreatedAtDesc(UUID supplierId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :cutoff")
    List<Order> findByStatusAndCreatedAtBefore(@Param("status") String status, @Param("cutoff") Instant cutoff);

    @Query(value = "SELECT DATE_FORMAT(o.created_at, '%Y-%m-01') AS month, " +
            "COUNT(*) AS order_count, " +
            "COALESCE(SUM(o.total), 0) AS revenue " +
            "FROM orders o " +
            "WHERE o.supplier_id = :supplierId " +
            "AND o.created_at >= :since " +
            "AND o.status NOT IN ('CANCELLED', 'REJECTED') " +
            "GROUP BY DATE_FORMAT(o.created_at, '%Y-%m-01') " +
            "ORDER BY month ASC",
            nativeQuery = true)
    List<Object[]> monthlyRevenueBySupplier(@Param("supplierId") UUID supplierId, @Param("since") Instant since);

    @Query(value = "SELECT o.status, COUNT(*) AS cnt " +
            "FROM orders o " +
            "WHERE o.supplier_id = :supplierId " +
            "GROUP BY o.status",
            nativeQuery = true)
    List<Object[]> orderCountByStatusForSupplier(@Param("supplierId") UUID supplierId);

    @Query(value = "SELECT oi.product_name, SUM(oi.quantity) AS total_qty, SUM(oi.line_total) AS total_revenue " +
            "FROM order_items oi " +
            "JOIN orders o ON oi.order_id = o.id " +
            "WHERE o.supplier_id = :supplierId " +
            "AND o.status NOT IN ('CANCELLED', 'REJECTED') " +
            "GROUP BY oi.product_name " +
            "ORDER BY total_qty DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Object[]> topSoldProductsForSupplier(@Param("supplierId") UUID supplierId, @Param("limit") int limit);
}
