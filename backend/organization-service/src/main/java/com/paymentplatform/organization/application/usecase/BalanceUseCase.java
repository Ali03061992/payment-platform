package com.paymentplatform.organization.application.usecase;

import java.util.UUID;

import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.organization.application.dto.BalanceResponse;
import com.paymentplatform.organization.domain.model.BalanceEntry;
import com.paymentplatform.organization.domain.repository.BalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class BalanceUseCase {

    private final BalanceRepository balanceRepository;

    public BalanceUseCase(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
    }

    public BalanceEntry creditBalance(UUID supplierId, UUID shopId, BigDecimal amount, UUID orderId, UUID createdBy) {
        BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
        BigDecimal newBalance = currentBalance.add(amount);

        BalanceEntry entry = BalanceEntry.create(
                supplierId, shopId, BalanceEntry.ORDER_CREDIT, amount,
                newBalance, orderId, null, null, null, createdBy);
        return balanceRepository.save(entry);
    }

    public BalanceEntry debitBalance(UUID supplierId, UUID shopId, BigDecimal amount, UUID paymentId, UUID createdBy) {
        BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
        BigDecimal newBalance = currentBalance.subtract(amount);

        BalanceEntry entry = BalanceEntry.create(
                supplierId, shopId, BalanceEntry.PAYMENT_DEBIT, amount.negate(),
                newBalance, null, paymentId, null, null, createdBy);
        return balanceRepository.save(entry);
    }

    public BalanceEntry adjustBalance(UUID supplierId, UUID shopId, BigDecimal amount, String reason, UUID createdBy) {
        BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
        BigDecimal newBalance = currentBalance.add(amount);

        BalanceEntry entry = BalanceEntry.create(
                supplierId, shopId, BalanceEntry.ADJUSTMENT, amount,
                newBalance, null, null, null, reason, createdBy);
        return balanceRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(UUID supplierId, UUID shopId) {
        return getCurrentBalance(supplierId, shopId);
    }

    @Transactional(readOnly = true)
    public List<BalanceEntry> getBalanceHistory(UUID supplierId, UUID shopId) {
        return balanceRepository.findBySupplierIdAndShopIdOrderByCreatedAtDesc(supplierId, shopId);
    }

    @Transactional(readOnly = true)
    public List<BalanceEntry> getSupplierBalances(UUID supplierId) {
        return balanceRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    }

    @Transactional(readOnly = true)
    public List<BalanceEntry> getShopBalances(UUID shopId) {
        return balanceRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getShopBalanceSummaries(UUID shopId) {
        List<UUID> supplierIds = balanceRepository.findDistinctSupplierIdsByShopId(shopId);
        return supplierIds.stream().map(supplierId -> {
            BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
            BigDecimal totalOrders = balanceRepository.sumOrdersBySupplierAndShop(supplierId, shopId);
            BigDecimal totalPayments = balanceRepository.sumPaymentsBySupplierAndShop(supplierId, shopId);
            java.time.Instant lastTransaction = balanceRepository.findLatestBySupplierIdAndShopId(supplierId, shopId)
                    .map(BalanceEntry::getCreatedAt).orElse(null);
            return new BalanceResponse(
                    supplierId, shopId,
                    "Supplier " + supplierId, "Shop " + shopId,
                    currentBalance, totalOrders, totalPayments,
                    currentBalance, lastTransaction
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getSupplierBalanceSummaries(UUID supplierId) {
        List<UUID> shopIds = balanceRepository.findDistinctShopIdsBySupplierId(supplierId);
        return shopIds.stream().map(shopId -> {
            BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
            BigDecimal totalOrders = balanceRepository.sumOrdersBySupplierAndShop(supplierId, shopId);
            BigDecimal totalPayments = balanceRepository.sumPaymentsBySupplierAndShop(supplierId, shopId);
            java.time.Instant lastTransaction = balanceRepository.findLatestBySupplierIdAndShopId(supplierId, shopId)
                    .map(BalanceEntry::getCreatedAt).orElse(null);
            return new BalanceResponse(
                    supplierId, shopId,
                    "Supplier " + supplierId, "Shop " + shopId,
                    currentBalance, totalOrders, totalPayments,
                    currentBalance, lastTransaction
            );
        }).toList();
    }

    private BigDecimal getCurrentBalance(UUID supplierId, UUID shopId) {
        return balanceRepository.findFirstBySupplierIdAndShopIdOrderByCreatedAtDesc(supplierId, shopId)
                .map(BalanceEntry::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
    }
}
