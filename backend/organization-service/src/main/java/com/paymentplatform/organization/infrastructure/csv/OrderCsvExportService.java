package com.paymentplatform.organization.infrastructure.csv;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.infrastructure.http.IdentityClient;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderCsvExportService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrganizationRepository organizationRepository;
    private final IdentityClient identityClient;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    public OrderCsvExportService(OrderRepository orderRepository,
                                  OrderItemRepository orderItemRepository,
                                  OrganizationRepository organizationRepository,
                                  IdentityClient identityClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.organizationRepository = organizationRepository;
        this.identityClient = identityClient;
    }

    public String generateOrdersCsv(String status, Instant from, Instant to,
                                     UUID supplierId, UUID shopId) {
        List<Order> allOrders = orderRepository.findAll();

        List<Order> filtered = allOrders.stream()
                .filter(o -> status == null || status.isBlank() || status.equals(o.getStatus()))
                .filter(o -> from == null || !o.getCreatedAt().isBefore(from))
                .filter(o -> to == null || !o.getCreatedAt().isAfter(to))
                .filter(o -> supplierId == null || supplierId.equals(o.getSupplierId()))
                .filter(o -> shopId == null || shopId.equals(o.getShopId()))
                .toList();

        StringWriter sw = new StringWriter();
        sw.write("Reference,Supplier,Shop,CreatedBy,Source,Status,Subtotal,TaxRate,TaxAmount,Total,Currency,"
                + "DeliveryAgent,PlannedDeliveryDate,ConfirmedDeliveryDate,AsapPayment,DeliveredAt,ReceivedBy,"
                + "ReceivedAt,DeliveryRejectionReason,Notes,CreatedAt,UpdatedAt\n");

        for (Order o : filtered) {
            String supplierName = resolveOrgName(o.getSupplierId());
            String shopName = resolveOrgName(o.getShopId());
            String deliveryAgentName = identityClient.resolveUserName(o.getDeliveryAgentId());
            String receivedByName = identityClient.resolveUserName(o.getReceivedBy());
            String createdByName = identityClient.resolveUserName(o.getCreatedBy());

            sw.write(String.format("%s,\"%s\",\"%s\",\"%s\",%s,%s,%s,%s,%s,%s,%s,\"%s\",%s,%s,%s,%s,\"%s\",%s,\"%s\",\"%s\",%s,%s\n",
                    o.getReference(),
                    esc(supplierName),
                    esc(shopName),
                    esc(createdByName),
                    o.getSource(),
                    o.getStatus(),
                    o.getSubtotal().toPlainString(),
                    o.getTaxRate().toPlainString(),
                    o.getTaxAmount().toPlainString(),
                    o.getTotal().toPlainString(),
                    o.getCurrency(),
                    esc(deliveryAgentName),
                    o.getPlannedDeliveryDate() != null ? o.getPlannedDeliveryDate().toString() : "",
                    o.getConfirmedDeliveryDate() != null ? o.getConfirmedDeliveryDate().toString() : "",
                    o.isAsapPayment(),
                    o.getDeliveredAt() != null ? FORMATTER.format(o.getDeliveredAt()) : "",
                    esc(receivedByName),
                    o.getReceivedAt() != null ? FORMATTER.format(o.getReceivedAt()) : "",
                    esc(o.getDeliveryRejectionReason() != null ? o.getDeliveryRejectionReason() : ""),
                    esc(o.getNotes() != null ? o.getNotes() : ""),
                    FORMATTER.format(o.getCreatedAt()),
                    FORMATTER.format(o.getUpdatedAt())
            ));
        }

        return sw.toString();
    }

    private String resolveOrgName(UUID orgId) {
        if (orgId == null) return "";
        return organizationRepository.findById(OrganizationId.of(orgId))
                .map(org -> org.name().value())
                .orElse("Organization " + orgId);
    }

    private String esc(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
