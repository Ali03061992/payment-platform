package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.application.dto.*;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.StockMovement;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.StockMovementRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StockService {

    private final ProductRepository products;
    private final StockMovementRepository movements;

    public StockService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }

    public List<ProductResponse> listProducts(UUID supplierId, String status) {
        List<Product> list = (status != null && !status.isBlank())
                ? products.findBySupplierIdAndStatus(supplierId, status)
                : products.findBySupplierId(supplierId);
        return list.stream().map(ProductResponse::from).collect(Collectors.toList());
    }

    public ProductResponse getProduct(UUID supplierId, UUID productId) {
        Product product = products.findById(productId)
                .filter(p -> p.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse createProduct(UUID supplierId, ProductCreateRequest request) {
        if (products.existsBySupplierIdAndSku(supplierId, request.sku())) {
            throw new ConflictException("Un produit avec ce SKU existe déjà pour ce fournisseur");
        }
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setUnitPrice(request.unitPrice());
        product.setCurrency(request.currency());
        product.setQuantity(request.quantity());
        product.setMinQuantity(request.minQuantity());
        products.save(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(UUID supplierId, UUID productId, ProductUpdateRequest request) {
        Product product = products.findById(productId)
                .filter(p -> p.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.unitPrice() != null) product.setUnitPrice(request.unitPrice());
        if (request.quantity() != null) product.setQuantity(request.quantity());
        if (request.minQuantity() != null) product.setMinQuantity(request.minQuantity());
        if (request.status() != null) product.setStatus(request.status());
        products.save(product);
        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse deleteProduct(UUID supplierId, UUID productId) {
        Product product = products.findById(productId)
                .filter(p -> p.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));
        if (product.getReservedQty() != null && product.getReservedQty() > 0) {
            throw new ConflictException("Impossible de désactiver un produit avec des réservations en cours");
        }
        product.setStatus("INACTIVE");
        products.save(product);
        return ProductResponse.from(product);
    }

    public List<StockMovementResponse> listMovements(UUID supplierId, UUID productId) {
        List<StockMovement> list = (productId != null)
                ? movements.findByProductIdOrderByCreatedAtDesc(productId)
                : movements.findBySupplierIdOrderByCreatedAtDesc(supplierId);
        return list.stream()
                .map(m -> {
                    String productName = products.findById(m.getProductId())
                            .map(Product::getName)
                            .orElse("Inconnu");
                    return StockMovementResponse.from(m, productName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public StockMovementResponse createMovement(UUID supplierId, StockMovementRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ConflictException("La quantité doit être supérieure à 0");
        }
        Product product = products.findByIdForUpdate(request.productId())
                .filter(p -> p.getSupplierId().equals(supplierId))
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));

        int currentQty = product.getQuantity();
        int delta = request.quantity();
        switch (request.type()) {
            case "IN" -> product.setQuantity(currentQty + delta);
            case "OUT" -> {
                if (currentQty < delta) throw new ConflictException("Stock insuffisant");
                product.setQuantity(currentQty - delta);
            }
            case "ADJUSTMENT" -> product.setQuantity(delta);
            default -> throw new ConflictException("Type de mouvement inconnu : " + request.type());
        }
        products.save(product);

        StockMovement movement = new StockMovement();
        movement.setProductId(request.productId());
        movement.setSupplierId(supplierId);
        movement.setType(request.type());
        movement.setQuantity(request.quantity());
        movement.setReference(request.reference());
        movement.setNotes(request.notes());
        movements.save(movement);

        return StockMovementResponse.from(movement, product.getName());
    }
}
