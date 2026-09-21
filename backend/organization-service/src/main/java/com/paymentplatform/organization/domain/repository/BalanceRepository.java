package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.BalanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<BalanceEntry, UUID> {

    List<BalanceEntry> findBySupplierIdAndShopIdOrderByCreatedAtDesc(UUID supplierId, UUID shopId);

    List<BalanceEntry> findBySupplierIdOrderByCreatedAtDesc(UUID supplierId);

    List<BalanceEntry> findByShopIdOrderByCreatedAtDesc(UUID shopId);

    Optional<BalanceEntry> findFirstBySupplierIdAndShopIdOrderByCreatedAtDesc(UUID supplierId, UUID shopId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId")
    BigDecimal sumBalance(@Param("supplierId") UUID supplierId, @Param("shopId") UUID shopId);

    @Query("SELECT e.supplierId FROM BalanceEntry e WHERE e.shopId = :shopId GROUP BY e.supplierId")
    List<UUID> findDistinctSupplierIdsByShopId(@Param("shopId") UUID shopId);

    @Query("SELECT e.shopId FROM BalanceEntry e WHERE e.supplierId = :supplierId GROUP BY e.shopId")
    List<UUID> findDistinctShopIdsBySupplierId(@Param("supplierId") UUID supplierId);

    @Query("SELECT e FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId ORDER BY e.createdAt DESC LIMIT 1")
    Optional<BalanceEntry> findLatestBySupplierIdAndShopId(@Param("supplierId") UUID supplierId, @Param("shopId") UUID shopId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId AND e.type = 'ORDER_CREDIT'")
    BigDecimal sumOrdersBySupplierAndShop(@Param("supplierId") UUID supplierId, @Param("shopId") UUID shopId);

    @Query("SELECT COALESCE(SUM(ABS(e.amount)), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId AND e.type = 'PAYMENT_DEBIT'")
    BigDecimal sumPaymentsBySupplierAndShop(@Param("supplierId") UUID supplierId, @Param("shopId") UUID shopId);
}
