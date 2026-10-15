package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.QuoteRequestResponse;
import com.paymentplatform.organization.domain.model.QuoteRequest;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Annulation d'une demande de devis par la boutique demanderesse.
 */
@Service
public class CancelQuoteRequestUseCase {

    private final QuoteRequestRepository requests;

    public CancelQuoteRequestUseCase(QuoteRequestRepository requests) {
        this.requests = requests;
    }

    @Transactional
    public QuoteRequestResponse execute(UUID requestId, UUID shopId) {
        QuoteRequest request = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Demande non trouvée : " + requestId));
        if (shopId == null || !shopId.equals(request.shopId())) {
            throw new ForbiddenException("Seule la boutique demanderesse peut annuler");
        }
        request.markCancelled();
        requests.save(request);
        return QuoteRequestResponse.from(request, List.of());
    }
}
