package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.domain.model.ProductCategory;
import com.paymentplatform.organization.domain.model.ProductFamily;
import com.paymentplatform.organization.domain.repository.ProductCategoryRepository;
import com.paymentplatform.organization.domain.repository.ProductFamilyRepository;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/supplier/catalog")
public class CatalogController {

    private final ProductCategoryRepository categoryRepository;
    private final ProductFamilyRepository familyRepository;
    private final AuditRecorder audit;

    public CatalogController(ProductCategoryRepository categoryRepository,
                             ProductFamilyRepository familyRepository,
                             AuditRecorder audit) {
        this.categoryRepository = categoryRepository;
        this.familyRepository = familyRepository;
        this.audit = audit;
    }

    // --- Categories ---

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<List<ProductCategory>> listCategories(@RequestParam UUID supplierId) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(categoryRepository.findBySupplierIdAndDeletedAtIsNull(supplierId));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<?> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        var current = CurrentUser.get();
        UUID supplierId = request.supplierId();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        // M3 : l'unicité ne porte que sur les lignes actives — un code
        // soft-deleté peut être recréé (l'historique reste sur l'ancienne ligne).
        if (categoryRepository.existsBySupplierIdAndCodeAndDeletedAtIsNull(supplierId, request.code())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "UNIQUE_CONSTRAINT_VIOLATION",
                "message", "Une catégorie avec le code '" + request.code() + "' existe déjà pour ce fournisseur"
            ));
        }
        ProductCategory category = new ProductCategory();
        category.setSupplierId(supplierId);
        category.setName(request.name());
        category.setCode(request.code());
        ProductCategory saved = categoryRepository.save(category);
        return ResponseEntity.created(URI.create("/api/supplier/catalog/categories/" + saved.getId())).body(saved);
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        var current = CurrentUser.get();
        var category = categoryRepository.findById(id).filter(c -> !c.isDeleted());
        if (category.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // M3 : soft-delete — la ligne survit pour l'historique (produits, commandes).
        ProductCategory toDelete = category.get();
        toDelete.setDeletedAt(Instant.now());
        categoryRepository.save(toDelete);
        audit.record(current.userId(), current.organizationId(), AuditActions.CATEGORY_DELETED,
                id, "{\"code\":\"" + toDelete.getCode() + "\"}");
        return ResponseEntity.noContent().build();
    }

    // --- Families ---

    @GetMapping("/families")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<List<ProductFamily>> listFamilies(
            @RequestParam(required = false) UUID supplierId,
            @RequestParam(required = false) UUID categoryId) {
        var current = CurrentUser.get();
        if (supplierId != null && current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        if (categoryId != null) {
            return ResponseEntity.ok(familyRepository.findByCategoryId(categoryId));
        }
        if (supplierId != null) {
            return ResponseEntity.ok(familyRepository.findBySupplierIdAndDeletedAtIsNull(supplierId));
        }
        return ResponseEntity.badRequest().build();
    }

    @PostMapping("/families")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<ProductFamily> createFamily(
            @Valid @RequestBody FamilyRequest request) {
        var current = CurrentUser.get();
        UUID supplierId = request.supplierId();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        ProductFamily family = new ProductFamily();
        family.setSupplierId(supplierId);
        family.setName(request.name());
        family.setCode(request.code());

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            // M3 : on ne rattache jamais une catégorie soft-deletée.
            Set<ProductCategory> cats = new HashSet<>(categoryRepository.findAllById(request.categoryIds())
                    .stream().filter(c -> !c.isDeleted()).toList());
            family.setCategories(cats);
        }

        ProductFamily saved = familyRepository.save(family);
        return ResponseEntity.created(URI.create("/api/supplier/catalog/families/" + saved.getId())).body(saved);
    }

    @PutMapping("/families/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<ProductFamily> updateFamily(
            @PathVariable UUID id,
            @Valid @RequestBody FamilyRequest request) {
        var current = CurrentUser.get();
        UUID supplierId = request.supplierId();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return familyRepository.findById(id).filter(f -> !f.isDeleted()).map(family -> {
            family.setName(request.name());
            family.setCode(request.code());
            if (request.categoryIds() != null) {
                Set<ProductCategory> cats = new HashSet<>(categoryRepository.findAllById(request.categoryIds())
                        .stream().filter(c -> !c.isDeleted()).toList());
                family.setCategories(cats);
            }
            return ResponseEntity.ok(familyRepository.save(family));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/families/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteFamily(@PathVariable UUID id) {
        var current = CurrentUser.get();
        var family = familyRepository.findById(id).filter(f -> !f.isDeleted());
        if (family.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // M3 : soft-delete — la ligne survit pour l'historique (produits, commandes).
        ProductFamily toDelete = family.get();
        toDelete.setDeletedAt(Instant.now());
        familyRepository.save(toDelete);
        audit.record(current.userId(), current.organizationId(), AuditActions.FAMILY_DELETED,
                id, "{\"code\":\"" + toDelete.getCode() + "\"}");
        return ResponseEntity.noContent().build();
    }

    // --- Request DTOs ---

    public record CategoryRequest(
            @NotNull UUID supplierId,
            @NotBlank String name,
            @NotBlank String code
    ) {}

    public record FamilyRequest(
            @NotNull UUID supplierId,
            @NotBlank String name,
            @NotBlank String code,
            Set<UUID> categoryIds
    ) {}
}
