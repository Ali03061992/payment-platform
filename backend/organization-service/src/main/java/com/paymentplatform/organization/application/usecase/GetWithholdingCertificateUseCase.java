package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.WithholdingCertificateResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetWithholdingCertificateUseCase {

    private final OrderRepository orders;
    private final WithholdingCertificateRepository certificates;

    public GetWithholdingCertificateUseCase(OrderRepository orders,
                                           WithholdingCertificateRepository certificates) {
        this.orders = orders;
        this.certificates = certificates;
    }

    @Transactional(readOnly = true)
    public WithholdingCertificateResponse execute(UUID orderId, UUID organizationId, boolean systemAdmin) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        if (!systemAdmin) {
            if (organizationId == null || (!organizationId.equals(order.getSupplierId())
                    && !organizationId.equals(order.getShopId()))) {
                throw new ForbiddenException("Accès refusé au certificat de retenue");
            }
        }
        return certificates.findByOrderId(orderId)
                .map(WithholdingCertificateResponse::from)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun certificat de retenue pour la commande : " + orderId));
    }

    @Transactional(readOnly = true)
    public WithholdingCertificateResponse executeByNumber(String number, UUID organizationId,
                                                          boolean systemAdmin) {
        var certificate = certificates.findByNumber(number)
                .orElseThrow(() -> new NotFoundException(
                        "Aucun certificat de retenue avec le numéro : " + number));
        Order order = orders.findById(certificate.orderId())
                .orElseThrow(() -> new NotFoundException(
                        "Commande non trouvée : " + certificate.orderId()));
        if (!systemAdmin) {
            if (organizationId == null || (!organizationId.equals(order.getSupplierId())
                    && !organizationId.equals(order.getShopId()))) {
                throw new ForbiddenException("Accès refusé au certificat de retenue");
            }
        }
        return WithholdingCertificateResponse.from(certificate);
    }
}
