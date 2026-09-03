package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.BalanceResponse;
import com.paymentplatform.organization.application.usecase.BalanceUseCase;
import com.paymentplatform.organization.domain.model.BalanceEntry;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/balances")
public class BalanceController {

    private final BalanceUseCase balanceUseCase;

    public BalanceController(BalanceUseCase balanceUseCase) {
        this.balanceUseCase = balanceUseCase;
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("hasAuthority('SUPPLIER_ADMIN') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<List<BalanceResponse>> listSupplierBalances(@PathVariable Long supplierId) {
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN") && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(balanceUseCase.getSupplierBalanceSummaries(supplierId));
    }

    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasAuthority('SHOP_ADMIN') or hasAuthority('SHOP_MANAGER') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<List<BalanceResponse>> listShopBalances(@PathVariable Long shopId) {
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN") && !current.organizationId().equals(shopId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(balanceUseCase.getShopBalanceSummaries(shopId));
    }

    @GetMapping("/supplier/{supplierId}/shop/{shopId}")
    @PreAuthorize("hasAuthority('SUPPLIER_ADMIN') or hasAuthority('SHOP_ADMIN') or hasAuthority('SHOP_MANAGER') or hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<List<BalanceEntry>> getBalanceHistory(
            @PathVariable Long supplierId,
            @PathVariable Long shopId) {
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            Long orgId = current.organizationId();
            if (orgId == null) return ResponseEntity.status(403).build();
            boolean isSupplier = orgId.equals(supplierId);
            boolean isShop = orgId.equals(shopId);
            if (!isSupplier && !isShop) return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(balanceUseCase.getBalanceHistory(supplierId, shopId));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<BalanceEntry> adjustBalance(@Valid @RequestBody AdjustBalanceRequest request) {
        var current = CurrentUser.get();
        BalanceEntry entry = balanceUseCase.adjustBalance(
                request.supplierId(), request.shopId(), request.amount(),
                request.reason(), current.userId());
        return ResponseEntity.ok(entry);
    }

    public record AdjustBalanceRequest(
            @NotNull Long supplierId,
            @NotNull Long shopId,
            @NotNull BigDecimal amount,
            String reason
    ) {}
}
