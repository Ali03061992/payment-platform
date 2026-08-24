package com.paymentplatform.organization.domain.repository;

import com.paymentplatform.organization.domain.model.BalanceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BalanceRepository extends JpaRepository<BalanceEntry, Long> {

    List<BalanceEntry> findBySupplierIdAndShopIdOrderByCreatedAtDesc(Long supplierId, Long shopId);

    List<BalanceEntry> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);

    List<BalanceEntry> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<BalanceEntry> findFirstBySupplierIdAndShopIdOrderByCreatedAtDesc(Long supplierId, Long shopId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM BalanceEntry e WHERE e.supplierId = :supplierId AND e.shopId = :shopId")
    BigDecimal sumBalance(@Param("supplierId") Long supplierId, @Param("shopId") Long shopId);
}
