package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.domain.model.BalanceEntry;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceUseCaseH2Test {

    @Autowired private BalanceUseCase balanceUseCase;

    @Test
    void creditBalance_increasesBalance() {
        var entry = balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(entry.getType()).isEqualTo(BalanceEntry.ORDER_CREDIT);
    }

    @Test
    void debitBalance_decreasesBalance() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("200.00"), 10L, 1L);
        var entry = balanceUseCase.debitBalance(1L, 2L, new BigDecimal("50.00"), 20L, 1L);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(entry.getType()).isEqualTo(BalanceEntry.PAYMENT_DEBIT);
    }

    @Test
    void adjustBalance_addsAmount() {
        var entry = balanceUseCase.adjustBalance(1L, 2L, new BigDecimal("25.00"), "Manual adjustment", 1L);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(entry.getType()).isEqualTo(BalanceEntry.ADJUSTMENT);
    }

    @Test
    void adjustBalance_negativeAmount_subtracts() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        var entry = balanceUseCase.adjustBalance(1L, 2L, new BigDecimal("-30.00"), "Correction", 1L);
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void getBalance_returnsCurrentBalance() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        balanceUseCase.debitBalance(1L, 2L, new BigDecimal("40.00"), 20L, 1L);
        BigDecimal balance = balanceUseCase.getBalance(1L, 2L);
        assertThat(balance).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void getBalance_noEntries_returnsZero() {
        BigDecimal balance = balanceUseCase.getBalance(1L, 2L);
        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getBalanceHistory_returnsAll() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        balanceUseCase.debitBalance(1L, 2L, new BigDecimal("50.00"), 20L, 1L);
        var history = balanceUseCase.getBalanceHistory(1L, 2L);
        assertThat(history).hasSize(2);
    }

    @Test
    void getSupplierBalances_returnsEntries() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        balanceUseCase.creditBalance(1L, 3L, new BigDecimal("50.00"), 11L, 1L);
        var result = balanceUseCase.getSupplierBalances(1L);
        assertThat(result).hasSize(2);
    }

    @Test
    void getShopBalances_returnsEntries() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        var result = balanceUseCase.getShopBalances(2L);
        assertThat(result).hasSize(1);
    }

    @Test
    void multipleTransactions_correctRunningBalance() {
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("100.00"), 10L, 1L);
        balanceUseCase.creditBalance(1L, 2L, new BigDecimal("50.00"), 11L, 1L);
        balanceUseCase.debitBalance(1L, 2L, new BigDecimal("30.00"), 20L, 1L);
        BigDecimal balance = balanceUseCase.getBalance(1L, 2L);
        assertThat(balance).isEqualByComparingTo(new BigDecimal("120.00"));
    }
}
