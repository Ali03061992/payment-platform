package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.AddDisputeMessageRequest;
import com.paymentplatform.organization.application.dto.CreateDisputeRequest;
import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.application.dto.PageResponse;
import com.paymentplatform.organization.application.usecase.AddDisputeMessageUseCase;
import com.paymentplatform.organization.application.usecase.CreateDisputeUseCase;
import com.paymentplatform.organization.application.usecase.DisputeAttachmentService;
import com.paymentplatform.organization.application.usecase.ManageDisputeUseCase;
import com.paymentplatform.organization.application.usecase.ResolveDisputeUseCase;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeAttachment;
import com.paymentplatform.organization.domain.model.DisputeNote;
import com.paymentplatform.organization.domain.model.DisputeStatusHistory;
import com.paymentplatform.organization.domain.repository.DisputeAttachmentRepository;
import com.paymentplatform.organization.domain.repository.DisputeNoteRepository;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.organization.domain.repository.DisputeStatusHistoryRepository;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private static final String DISPUTE_ROLES =
            "'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN'";

    private final CreateDisputeUseCase createDispute;
    private final AddDisputeMessageUseCase addDisputeMessage;
    private final ResolveDisputeUseCase resolveDispute;
    private final ManageDisputeUseCase manageDispute;
    private final DisputeAttachmentService attachmentService;
    private final DisputeRepository disputeRepository;
    private final DisputeStatusHistoryRepository historyRepository;
    private final DisputeAttachmentRepository attachmentRepository;
    private final DisputeNoteRepository noteRepository;

    public DisputeController(CreateDisputeUseCase createDispute,
                             AddDisputeMessageUseCase addDisputeMessage,
                             ResolveDisputeUseCase resolveDispute,
                             ManageDisputeUseCase manageDispute,
                             DisputeAttachmentService attachmentService,
                             DisputeRepository disputeRepository,
                             DisputeStatusHistoryRepository historyRepository,
                             DisputeAttachmentRepository attachmentRepository,
                             DisputeNoteRepository noteRepository) {
        this.createDispute = createDispute;
        this.addDisputeMessage = addDisputeMessage;
        this.resolveDispute = resolveDispute;
        this.manageDispute = manageDispute;
        this.attachmentService = attachmentService;
        this.disputeRepository = disputeRepository;
        this.historyRepository = historyRepository;
        this.attachmentRepository = attachmentRepository;
        this.noteRepository = noteRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<DisputeResponse> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        var current = CurrentUser.get();
        DisputeResponse response = createDispute.execute(
                request.orderId(), current.actor(), request.reason(), request.priority());
        return ResponseEntity.created(URI.create("/api/disputes/" + response.id())).body(response);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<List<DisputeResponse>> getDisputesByOrder(@PathVariable UUID orderId) {
        var current = CurrentUser.get();
        var disputes = disputeRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            if (orgId == null) {
                return ResponseEntity.status(403).build();
            }
            boolean allowed = disputes.isEmpty() || disputes.stream().anyMatch(d ->
                    orgId.equals(d.getShopId()) || orgId.equals(d.getSupplierId()));
            if (!allowed) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(disputes.stream().map(DisputeResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable UUID id) {
        var current = CurrentUser.get();
        Dispute dispute = loadAuthorized(id);
        return ResponseEntity.ok(enriched(dispute, isAdmin(current)));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT')")
    public ResponseEntity<DisputeResponse> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddDisputeMessageRequest request) {
        var current = CurrentUser.get();
        DisputeResponse response = addDisputeMessage.execute(id, current.actor(), request.content());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        String targetStatus = body.getOrDefault("status", "RESOLVED");
        DisputeResponse response = resolveDispute.execute(id, current.actor(), targetStatus);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/transition")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> transition(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        DisputeResponse response = resolveDispute.execute(id, current.actor(),
                body.getOrDefault("status", "IN_PROGRESS"));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> updatePriority(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(manageDispute.updatePriority(id, current.actor(), body.get("priority")));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> assign(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        var current = CurrentUser.get();
        UUID assigneeId = null;
        if (body != null && body.get("assigneeId") != null && !body.get("assigneeId").isBlank()) {
            try {
                assigneeId = UUID.fromString(body.get("assigneeId"));
            } catch (IllegalArgumentException e) {
                throw new ConflictException("Identifiant assigné invalide");
            }
        }
        return ResponseEntity.ok(manageDispute.assign(id, current.actor(), assigneeId));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> addNote(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(manageDispute.addNote(id, current.actor(), body.get("content")));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<DisputeResponse> uploadAttachment(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        var current = CurrentUser.get();
        Dispute dispute = loadAuthorized(id);
        DisputeAttachment attachment;
        try (InputStream input = file.getInputStream()) {
            attachment = attachmentService.store(dispute.getId(), current.userId(),
                    file.getOriginalFilename(), file.getContentType(), file.getSize(), input);
        } catch (IOException e) {
            throw new ConflictException(
                    "Impossible de lire la pièce jointe");
        }
        return ResponseEntity.created(URI.create("/api/disputes/" + id + "/attachments/" + attachment.getId()))
                .body(enriched(dispute, isAdmin(current)));
    }

    @GetMapping("/{id}/attachments/{attachmentId}")
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {
        loadAuthorized(id);
        DisputeAttachmentService.AttachmentDownload download = attachmentService.load(id, attachmentId);
        String disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.sizeBytes())
                .body(new FileSystemResource(download.path()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<List<DisputeResponse>> listDisputes(
            @RequestParam(required = false) String status) {
        var current = CurrentUser.get();
        boolean hasStatus = status != null && !status.isBlank();
        List<Dispute> disputes;
        if (current.roles().contains("SYSTEM_ADMIN")) {
            disputes = hasStatus
                    ? disputeRepository.findByStatusOrderByCreatedAtDesc(status)
                    : disputeRepository.findAll();
        } else {
            UUID orgId = current.organizationId();
            if (orgId == null) {
                return ResponseEntity.status(403).build();
            }
            boolean supplierSide = current.roles().contains("SUPPLIER_ADMIN")
                    || current.roles().contains("SUPPLIER_AGENT");
            if (supplierSide) {
                disputes = hasStatus
                        ? disputeRepository.findBySupplierIdAndStatusOrderByCreatedAtDesc(orgId, status)
                        : disputeRepository.findBySupplierIdOrderByCreatedAtDesc(orgId);
            } else {
                disputes = hasStatus
                        ? disputeRepository.findByShopIdAndStatusOrderByCreatedAtDesc(orgId, status)
                        : disputeRepository.findByShopIdOrderByCreatedAtDesc(orgId);
            }
        }
        return ResponseEntity.ok(disputes.stream().map(DisputeResponse::from).toList());
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyAuthority(" + DISPUTE_ROLES + ")")
    public ResponseEntity<PageResponse<DisputeResponse>> pagedDisputes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var current = CurrentUser.get();
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Dispute> spec = scopeFor(current);
        if (status != null && !status.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.trim().toUpperCase()));
        }
        if (priority != null && !priority.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("priority"), priority.trim().toUpperCase()));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("reason")), pattern));
        }
        Page<DisputeResponse> result = disputeRepository.findAll(spec, pageable)
                .map(DisputeResponse::from);
        return ResponseEntity.ok(PageResponse.of(result));
    }

    private Specification<Dispute> scopeFor(AuthenticatedUser current) {
        if (current.roles().contains("SYSTEM_ADMIN")) {
            return (root, query, cb) -> cb.conjunction();
        }
        UUID orgId = current.organizationId();
        if (orgId == null) {
            throw new ForbiddenException("Aucune organisation rattachée à ce compte");
        }
        boolean supplierSide = current.roles().contains("SUPPLIER_ADMIN")
                || current.roles().contains("SUPPLIER_AGENT");
        String field = supplierSide ? "supplierId" : "shopId";
        return (root, query, cb) -> cb.equal(root.get(field), orgId);
    }

    private DisputeResponse enriched(Dispute dispute, boolean includeNotes) {
        List<DisputeStatusHistory> history =
                historyRepository.findByDisputeIdOrderByChangedAtAsc(dispute.getId());
        List<DisputeAttachment> attachments =
                attachmentRepository.findByDisputeIdOrderByCreatedAtAsc(dispute.getId());
        List<DisputeNote> notes = includeNotes
                ? noteRepository.findByDisputeIdOrderByCreatedAtAsc(dispute.getId())
                : List.of();
        return DisputeResponse.from(dispute, history, attachments, notes);
    }

    private Dispute loadAuthorized(UUID id) {
        var current = CurrentUser.get();
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + id));
        TenantAccess.requireParticipant(current.actor(), dispute.getShopId(), dispute.getSupplierId(), null);
        return dispute;
    }

    private boolean isAdmin(AuthenticatedUser current) {
        return current.roles().contains("SYSTEM_ADMIN")
                || current.roles().contains("SHOP_ADMIN")
                || current.roles().contains("SUPPLIER_ADMIN");
    }
}
