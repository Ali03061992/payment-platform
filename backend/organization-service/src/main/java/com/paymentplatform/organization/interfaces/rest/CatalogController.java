package com.paymentplatform.organization.interfaces.rest;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.ProductCategory;
import com.paymentplatform.organization.domain.model.ProductFamily;
import com.paymentplatform.organization.domain.repository.ProductCategoryRepository;
import com.paymentplatform.organization.domain.repository.ProductFamilyRepository;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/supplier/catalog")
public class CatalogController {

    private final ProductCategoryRepository categoryRepository;
    private final ProductFamilyRepository familyRepository;

    public CatalogController(ProductCategoryRepository categoryRepository,
                             ProductFamilyRepository familyRepository) {
        this.categoryRepository = categoryRepository;
        this.familyRepository = familyRepository;
    }

    // --- Categories ---

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<List<ProductCategory>> listCategories(@RequestParam UUID supplierId) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(categoryRepository.findBySupplierId(supplierId));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<ProductCategory> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        var current = CurrentUser.get();
        UUID supplierId = request.supplierId();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        if (categoryRepository.existsBySupplierIdAndCode(supplierId, request.code())) {
            return ResponseEntity.badRequest().build();
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
        if (!categoryRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        categoryRepository.deleteById(id);
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
            return ResponseEntity.ok(familyRepository.findBySupplierId(supplierId));
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
            Set<ProductCategory> cats = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
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
        return familyRepository.findById(id).map(family -> {
            family.setName(request.name());
            family.setCode(request.code());
            if (request.categoryIds() != null) {
                Set<ProductCategory> cats = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
                family.setCategories(cats);
            }
            return ResponseEntity.ok(familyRepository.save(family));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/families/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteFamily(@PathVariable UUID id) {
        if (!familyRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        familyRepository.deleteById(id);
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
