package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.BalanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface BalanceRepository extends JpaRepository<BalanceEntry, Long> {

    List<BalanceEntry> findBySupplierIdAndShopIdOrderByCreatedAtDesc(Long supplierId, Long shopId);

    List<BalanceEntry> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    List<BalanceEntry> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<BalanceEntry> findFirstBySupplierIdAndShopIdOrderByCreatedAtDesc(Long supplierId, Long shopId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId")
    BigDecimal sumBalance(@Param("supplierId") Long supplierId, @Param("shopId") Long shopId);

    @Query("SELECT e.supplierId FROM BalanceEntry e WHERE e.shopId = :shopId GROUP BY e.supplierId")
    List<Long> findDistinctSupplierIdsByShopId(@Param("shopId") Long shopId);

    @Query("SELECT e.shopId FROM BalanceEntry e WHERE e.supplierId = :supplierId GROUP BY e.shopId")
    List<Long> findDistinctShopIdsBySupplierId(@Param("supplierId") Long supplierId);

    @Query("SELECT e FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId ORDER BY e.createdAt DESC LIMIT 1")
    Optional<BalanceEntry> findLatestBySupplierIdAndShopId(@Param("supplierId") Long supplierId, @Param("shopId") Long shopId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId AND e.type = 'ORDER_CREDIT'")
    BigDecimal sumOrdersBySupplierAndShop(@Param("supplierId") Long supplierId, @Param("shopId") Long shopId);

    @Query("SELECT COALESCE(SUM(ABS(e.amount)), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId AND e.type = 'PAYMENT_DEBIT'")
    BigDecimal sumPaymentsBySupplierAndShop(@Param("supplierId") Long supplierId, @Param("shopId") Long shopId);
}
