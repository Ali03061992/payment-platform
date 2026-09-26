package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.shared.infrastructure.audit.AuditLogEntity;
import com.paymentplatform.shared.infrastructure.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_VIEW_AUDIT')")
    public ResponseEntity<Page<AuditLogEntry>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {

        Specification<AuditLogEntity> spec = Specification.allOf();

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (action != null && !action.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (dateFrom != null) {
            Instant from = dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), from));
        }
        if (dateTo != null) {
            Instant to = dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("timestamp"), to));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLogEntity> result = auditLogRepository.findAll(spec, pageRequest);

        Page<AuditLogEntry> dtoPage = result.map(AuditLogEntry::from);
        return ResponseEntity.ok(dtoPage);
    }

    public record AuditLogEntry(
            UUID id,
            UUID userId,
            UUID organizationId,
            String action,
            UUID entityId,
            Instant timestamp,
            String details
    ) {
        public static AuditLogEntry from(AuditLogEntity entity) {
            return new AuditLogEntry(
                    entity.getId(),
                    entity.getUserId(),
                    entity.getOrganizationId(),
                    entity.getAction(),
                    entity.getEntityId(),
                    entity.getTimestamp(),
                    entity.getDetails()
            );
        }
    }
}
