package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeNote;
import com.paymentplatform.organization.domain.model.DisputePriority;
import com.paymentplatform.organization.domain.repository.DisputeNoteRepository;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.security.ActorContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Actions de pilotage d'un litige : priorite/SLA, assignation et notes internes. */
@Service
public class ManageDisputeUseCase {

    private final DisputeRepository disputes;
    private final DisputeNoteRepository notes;

    public ManageDisputeUseCase(DisputeRepository disputes, DisputeNoteRepository notes) {
        this.disputes = disputes;
        this.notes = notes;
    }

    @Transactional
    public DisputeResponse updatePriority(UUID disputeId, ActorContext actor, String priority) {
        Dispute dispute = loadForAdmin(disputeId, actor);
        dispute.changePriority(DisputePriority.from(priority));
        return DisputeResponse.from(disputes.save(dispute));
    }

    @Transactional
    public DisputeResponse assign(UUID disputeId, ActorContext actor, UUID assigneeId) {
        Dispute dispute = loadForAdmin(disputeId, actor);
        if (!actor.isSystemAdmin() && !actor.hasAnyRole(RoleCode.SUPPLIER_ADMIN)) {
            throw new ForbiddenException("Seuls les administrateurs système et fournisseur peuvent assigner un litige");
        }
        UUID target = assigneeId != null ? assigneeId : actor.userId();
        if (target == null) {
            throw new ConflictException("Aucun administrateur à assigner");
        }
        dispute.assignTo(target);
        return DisputeResponse.from(disputes.save(dispute));
    }

    @Transactional
    public DisputeResponse addNote(UUID disputeId, ActorContext actor, String content) {
        Dispute dispute = loadForAdmin(disputeId, actor);
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isEmpty()) {
            throw new ConflictException("Le contenu de la note est requis");
        }
        if (trimmed.length() > 2000) {
            throw new ConflictException("La note ne doit pas dépasser 2000 caractères");
        }
        notes.save(DisputeNote.create(dispute.getId(), actor.userId(), trimmed));
        return DisputeResponse.from(dispute);
    }

    private Dispute loadForAdmin(UUID disputeId, ActorContext actor) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + disputeId));
        TenantAccess.requireParticipant(actor, dispute.getShopId(), dispute.getSupplierId(), null);
        if (!actor.isSystemAdmin()
                && !actor.hasAnyRole(RoleCode.SHOP_ADMIN, RoleCode.SUPPLIER_ADMIN)) {
            throw new ForbiddenException("Seuls les administrateurs peuvent piloter ce litige");
        }
        return dispute;
    }
}
