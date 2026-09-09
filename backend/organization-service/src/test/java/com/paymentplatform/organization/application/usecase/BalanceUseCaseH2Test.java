package com.paymentplatform.organization.application.usecase;

import java.util.UUID;

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
        var entry = balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(entry.getType()).isEqualTo(BalanceEntry.ORDER_CREDIT);
    }

    @Test
    void debitBalance_decreasesBalance() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("200.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var entry = balanceUseCase.debitBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50.00"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(entry.getType()).isEqualTo(BalanceEntry.PAYMENT_DEBIT);
    }

    @Test
    void adjustBalance_addsAmount() {
        var entry = balanceUseCase.adjustBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("25.00"), "Manual adjustment", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(entry.getType()).isEqualTo(BalanceEntry.ADJUSTMENT);
    }

    @Test
    void adjustBalance_negativeAmount_subtracts() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var entry = balanceUseCase.adjustBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("-30.00"), "Correction", UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    void getBalance_returnsCurrentBalance() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        balanceUseCase.debitBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("40.00"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        BigDecimal balance = balanceUseCase.getBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(balance).isEqualByComparingTo(new BigDecimal("60.00"));
    }

    @Test
    void getBalance_noEntries_returnsZero() {
        BigDecimal balance = balanceUseCase.getBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getBalanceHistory_returnsAll() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        balanceUseCase.debitBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50.00"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var history = balanceUseCase.getBalanceHistory(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(history).hasSize(2);
    }

    @Test
    void getSupplierBalances_returnsEntries() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000003"), new BigDecimal("50.00"), UUID.fromString("00000000-0000-0000-0000-000000000011"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var result = balanceUseCase.getSupplierBalances(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(result).hasSize(2);
    }

    @Test
    void getShopBalances_returnsEntries() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        var result = balanceUseCase.getShopBalances(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(result).hasSize(1);
    }

    @Test
    void multipleTransactions_correctRunningBalance() {
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        balanceUseCase.creditBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("50.00"), UUID.fromString("00000000-0000-0000-0000-000000000011"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        balanceUseCase.debitBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"), new BigDecimal("30.00"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        BigDecimal balance = balanceUseCase.getBalance(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000002"));
        assertThat(balance).isEqualByComparingTo(new BigDecimal("120.00"));
    }
}
