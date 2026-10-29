package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.CreateQuoteRequestRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.dto.QuoteRequestResponse;
import com.paymentplatform.organization.application.dto.SendQuoteRequest;
import com.paymentplatform.organization.application.dto.SupplierQuoteResponse;
import com.paymentplatform.organization.application.service.InvoicePdfService;
import com.paymentplatform.organization.application.service.QuotePdfService;
import com.paymentplatform.organization.application.usecase.AcceptQuoteUseCase;
import com.paymentplatform.organization.application.usecase.CancelQuoteRequestUseCase;
import com.paymentplatform.organization.application.usecase.CreateQuoteRequestUseCase;
import com.paymentplatform.organization.application.usecase.ListQuoteRequestsUseCase;
import com.paymentplatform.organization.application.usecase.SendRequestQuoteUseCase;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import com.paymentplatform.organization.domain.repository.SupplierQuoteRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API REST des demandes de devis (découplées des commandes) : la boutique
 * demande, le fournisseur chiffre via l'application, la boutique accepte
 * (crée la commande) et télécharge chaque devis.
 */
@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    private final CreateQuoteRequestUseCase createRequest;
    private final ListQuoteRequestsUseCase listRequests;
    private final SendRequestQuoteUseCase sendQuote;
    private final AcceptQuoteUseCase acceptQuote;
    private final CancelQuoteRequestUseCase cancelRequest;
    private final QuoteRequestRepository requestRepository;
    private final SupplierQuoteRepository quoteRepository;
    private final OrganizationRepository organizationRepository;
    private final QuotePdfService quotePdfService;

    public QuoteController(CreateQuoteRequestUseCase createRequest,
                           ListQuoteRequestsUseCase listRequests,
                           SendRequestQuoteUseCase sendQuote,
                           AcceptQuoteUseCase acceptQuote,
                           CancelQuoteRequestUseCase cancelRequest,
                           QuoteRequestRepository requestRepository,
                           SupplierQuoteRepository quoteRepository,
                           OrganizationRepository organizationRepository,
                           QuotePdfService quotePdfService) {
        this.createRequest = createRequest;
        this.listRequests = listRequests;
        this.sendQuote = sendQuote;
        this.acceptQuote = acceptQuote;
        this.cancelRequest = cancelRequest;
        this.requestRepository = requestRepository;
        this.quoteRepository = quoteRepository;
        this.organizationRepository = organizationRepository;
        this.quotePdfService = quotePdfService;
    }

    private boolean isAdmin() {
        return CurrentUser.get().roles().contains("SYSTEM_ADMIN");
    }

    private boolean isSupplier() {
        var roles = CurrentUser.get().roles();
        return roles.contains("SUPPLIER_ADMIN") || roles.contains("SUPPLIER_AGENT");
    }

    /**
     * Crée une demande de devis (boutique, sans remise).
     */
    @PostMapping("/requests")
    @PreAuthorize("hasAnyAuthority('SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<QuoteRequestResponse> createDemand(
            @Valid @RequestBody CreateQuoteRequestRequest request) {
        var current = CurrentUser.get();
        UUID shopId = isAdmin() && current.organizationId() == null
                ? null : current.organizationId();
        if (shopId == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(createRequest.execute(request, shopId, current.userId()));
    }

    /**
     * Demandes visibles : les siennes (boutique), celles reçues (fournisseur).
     */
    @GetMapping("/requests")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<QuoteRequestResponse>> myRequests() {
        var current = CurrentUser.get();
        if (isAdmin()) {
            return ResponseEntity.ok(List.of());
        }
        UUID orgId = current.organizationId();
        if (orgId == null) {
            return ResponseEntity.status(403).build();
        }
        if (isSupplier()) {
            return ResponseEntity.ok(listRequests.forSupplier(orgId));
        }
        if (current.roles().contains("SHOP_ADMIN") || current.roles().contains("SHOP_AGENT")) {
            return ResponseEntity.ok(listRequests.forShop(orgId));
        }
        return ResponseEntity.status(403).build();
    }

    /**
     * Détail d'une demande + ses devis (périmètre boutique/fournisseur concernés).
     */
    @GetMapping("/requests/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<QuoteRequestResponse> getRequest(@PathVariable UUID id) {
        var request = requestRepository.findById(id);
        if (request.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var r = request.get();
        var current = CurrentUser.get();
        UUID orgId = current.organizationId();
        if (!isAdmin() && (orgId == null
                || (!orgId.equals(r.shopId()) && !orgId.equals(r.supplierId())))) {
            return ResponseEntity.status(403).build();
        }
        var quotes = quoteRepository.findByRequestId(id).stream()
                .map(SupplierQuoteResponse::from).toList();
        return ResponseEntity.ok(QuoteRequestResponse.from(r, quotes));
    }

    /**
     * Chiffre et envoie un devis versionné (fournisseur, via l'application).
     */
    @PostMapping("/requests/{id}/quotes")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<SupplierQuoteResponse> sendRequestQuote(
            @PathVariable UUID id, @Valid @RequestBody SendQuoteRequest request) {
        var current = CurrentUser.get();
        UUID supplierId = isAdmin()
                ? requestRepository.findById(id).map(q -> q.supplierId()).orElse(null)
                : current.organizationId();
        return ResponseEntity.ok(sendQuote.execute(id, request, supplierId, current.userId()));
    }

    /**
     * Accepte un devis (boutique) : crée la commande CONFIRMÉE aux conditions figées.
     */
    @PostMapping("/quotes/{quoteId}/accept")
    @PreAuthorize("hasAnyAuthority('SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<OrderResponse> accept(
            @PathVariable UUID quoteId) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(acceptQuote.execute(quoteId, current.organizationId(), current.userId()));
    }

    /**
     * Annule une demande (boutique demanderesse).
     */
    @PatchMapping("/requests/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<QuoteRequestResponse> cancel(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(cancelRequest.execute(id, current.organizationId()));
    }

    /**
     * Télécharge un devis en PDF (mise en page facture, intitulé DEVIS).
     */
    @GetMapping("/requests/{requestId}/quotes/{version}/pdf")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public void downloadQuotePdf(@PathVariable UUID requestId, @PathVariable int version,
                                 HttpServletResponse response) throws Exception {
        var request = requestRepository.findById(requestId);
        if (request.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Demande non trouvée");
            return;
        }
        var r = request.get();
        var current = CurrentUser.get();
        UUID orgId = current.organizationId();
        if (!isAdmin() && (orgId == null
                || (!orgId.equals(r.shopId()) && !orgId.equals(r.supplierId())))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
            return;
        }
        var quote = quoteRepository.findByRequestId(requestId).stream()
                .filter(q -> q.version() == version).findFirst();
        if (quote.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Devis non trouvé");
            return;
        }
        var supplier = toParty(r.supplierId());
        var buyer = toParty(r.shopId());
        byte[] pdf = quotePdfService.generateQuotePdf(quote.get(), supplier, buyer,
                r.shopId(), "Demande", r.reference());
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"devis-" + r.reference() + "-v" + version + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }

    private InvoicePdfService.InvoiceParty toParty(UUID orgId) {
        if (orgId == null) {
            return InvoicePdfService.InvoiceParty.named(null);
        }
        return organizationRepository.findById(OrganizationId.of(orgId))
                .map(org -> new InvoicePdfService.InvoiceParty(
                        org.name().value(),
                        org.address(),
                        org.matriculeFiscal(),
                        org.patente(),
                        org.fiscalRegime() != null ? org.fiscalRegime().name() : null,
                        org.suspensionAttestationNumber(),
                        org.suspensionAttestationDate()))
                .orElse(InvoicePdfService.InvoiceParty.named(null));
    }
}
