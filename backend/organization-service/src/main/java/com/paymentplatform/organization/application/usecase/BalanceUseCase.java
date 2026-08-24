package com.paymentplatform.organization.application.usecase;

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

    public BalanceEntry creditBalance(Long supplierId, Long shopId, BigDecimal amount, Long orderId, Long createdBy) {
        BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
        BigDecimal newBalance = currentBalance.add(amount);

        BalanceEntry entry = BalanceEntry.create(
                supplierId, shopId, BalanceEntry.ORDER_CREDIT, amount,
                newBalance, orderId, null, null, null, createdBy);
        return balanceRepository.save(entry);
    }

    public BalanceEntry debitBalance(Long supplierId, Long shopId, BigDecimal amount, Long paymentId, Long createdBy) {
        BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
        BigDecimal newBalance = currentBalance.subtract(amount);

        BalanceEntry entry = BalanceEntry.create(
                supplierId, shopId, BalanceEntry.PAYMENT_DEBIT, amount.negate(),
                newBalance, null, paymentId, null, null, createdBy);
        return balanceRepository.save(entry);
    }

    public BalanceEntry adjustBalance(Long supplierId, Long shopId, BigDecimal amount, String reason, Long createdBy) {
        BigDecimal currentBalance = getCurrentBalance(supplierId, shopId);
        BigDecimal newBalance = currentBalance.add(amount);

        BalanceEntry entry = BalanceEntry.create(
                supplierId, shopId, BalanceEntry.ADJUSTMENT, amount,
                newBalance, null, null, null, reason, createdBy);
        return balanceRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long supplierId, Long shopId) {
        return getCurrentBalance(supplierId, shopId);
    }

    @Transactional(readOnly = true)
    public List<BalanceEntry> getBalanceHistory(Long supplierId, Long shopId) {
        return balanceRepository.findBySupplierIdAndShopIdOrderByCreatedAtDesc(supplierId, shopId);
    }

    @Transactional(readOnly = true)
    public List<BalanceEntry> getSupplierBalances(Long supplierId) {
        return balanceRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    }

    @Transactional(readOnly = true)
    public List<BalanceEntry> getShopBalances(Long shopId) {
        return balanceRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }

    private BigDecimal getCurrentBalance(Long supplierId, Long shopId) {
        return balanceRepository.findFirstBySupplierIdAndShopIdOrderByCreatedAtDesc(supplierId, shopId)
                .map(BalanceEntry::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
    }
}
