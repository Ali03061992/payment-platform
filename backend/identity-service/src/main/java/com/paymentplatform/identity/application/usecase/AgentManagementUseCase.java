package com.paymentplatform.identity.application.usecase;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserCreatedEvent;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserDisabledEvent;
import com.paymentplatform.shared.domain.event.IdentityEvents.UserActivatedEvent;
import com.paymentplatform.identity.application.dto.AgentCreatedResponse;
import com.paymentplatform.identity.application.dto.AgentRequest;
import com.paymentplatform.identity.application.dto.UpdateAgentRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

/**
 * Gestion des agents d'une organisation (fournisseur ou boutique).
 * Périmètre strict : l'acteur ne gère que les agents de SA propre organisation.
 */
@Service
public class AgentManagementUseCase {

    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final OrganizationStatusPort organizationStatus;
    private final PasswordEncoder passwordEncoder;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;

    public AgentManagementUseCase(UserRepository users, OrganizationStatusPort organizationStatus,
                                  PasswordEncoder passwordEncoder, AuditRecorder audit,
                                  OutboxEventStore outbox) {
        this.users = users;
        this.organizationStatus = organizationStatus;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.outbox = outbox;
    }

    @Transactional
    public AgentCreatedResponse createAgent(long actorUserId, long organizationId, String expectedType,
                                            List<RoleCode> allowedRoles, AgentRequest request) {
        User actor = requireActor(actorUserId, organizationId);
        RoleCode role = RoleCode.from(request.role());
        if (!allowedRoles.contains(role)) {
            throw new ForbiddenException("Rôle " + request.role() + " non autorisé pour cette organisation");
        }

        var org = organizationStatus.getOrganizationStatus(organizationId);
        if (!expectedType.equals(org.type())) {
            throw new ConflictException("L'organisation " + organizationId + " n'est pas de type " + expectedType);
        }
        if (!org.isActive()) {
            throw new ConflictException("L'organisation " + organizationId + " est désactivée");
        }

        Username username = Username.of(request.username());
        Email email = Email.of(request.email());
        if (users.existsByUsername(username)) {
            throw new ConflictException("Nom d'utilisateur déjà utilisé");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("Adresse email déjà utilisée");
        }

        String rawPassword = request.password() != null && !request.password().isBlank()
                ? request.password()
                : generatePassword();
        PasswordHash hash = PasswordHash.of(passwordEncoder.encode(rawPassword));

        User user = User.create(new UserId(0), username, email, hash, request.firstName(), request.lastName(),
                PhoneNumber.of(request.phone()), OrganizationId.of(organizationId), role);
        User saved = users.save(user);

        audit.record(actorUserId, organizationId, AuditActions.USER_CREATED, saved.id().value(),
                "{\"username\":\"" + saved.username().value() + "\",\"role\":\"" + role + "\"}");
        outbox.append(new UserCreatedEvent(UUID.randomUUID(), java.time.Instant.now(), saved.id().value(),
                organizationId, List.of(role.name())), String.valueOf(saved.id().value()));

        return new AgentCreatedResponse(UserResponse.from(saved),
                request.password() == null || request.password().isBlank() ? rawPassword : null);
    }

    @Transactional
    public UserResponse updateAgent(long actorUserId, long organizationId, long agentId,
                                    UpdateAgentRequest request) {
        requireActor(actorUserId, organizationId);
        User agent = requireAgentOf(organizationId, agentId);
        agent.updateProfile(request.firstName(), request.lastName(), PhoneNumber.of(request.phone()));
        if (request.email() != null && !request.email().isBlank()) {
            Email email = Email.of(request.email());
            if (!agent.email().equals(email) && users.existsByEmail(email)) {
                throw new ConflictException("Adresse email déjà utilisée");
            }
            agent.updateEmail(email);
        }
        users.save(agent);
        audit.record(actorUserId, organizationId, AuditActions.USER_CREATED, agentId,
                "{\"action\":\"update\"}");
        return UserResponse.from(agent);
    }

    @Transactional
    public UserResponse disableAgent(long actorUserId, long organizationId, long agentId) {
        requireActor(actorUserId, organizationId);
        User agent = requireAgentOf(organizationId, agentId);
        agent.disable();
        users.save(agent);
        audit.record(actorUserId, organizationId, AuditActions.USER_DISABLED, agentId, "{\"by\":\"agent-manager\"}");
        outbox.append(new UserDisabledEvent(UUID.randomUUID(), java.time.Instant.now(), agentId,
                organizationId, "agent-manager"), String.valueOf(agentId));
        return UserResponse.from(agent);
    }

    @Transactional
    public UserResponse activateAgent(long actorUserId, long organizationId, long agentId) {
        requireActor(actorUserId, organizationId);
        User agent = requireAgentOf(organizationId, agentId);
        agent.activate();
        users.save(agent);
        audit.record(actorUserId, organizationId, AuditActions.USER_ENABLED, agentId, "{\"by\":\"agent-manager\"}");
        outbox.append(new UserActivatedEvent(UUID.randomUUID(), java.time.Instant.now(), agentId,
                organizationId), String.valueOf(agentId));
        return UserResponse.from(agent);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listAgents(long actorUserId, long organizationId) {
        requireActor(actorUserId, organizationId);
        return users.findByOrganizationId(OrganizationId.of(organizationId)).stream()
                .map(UserResponse::from)
                .toList();
    }

    private User requireActor(long actorUserId, long organizationId) {
        User actor = users.findById(UserId.of(actorUserId))
                .orElseThrow(() -> new NotFoundException("Utilisateur courant introuvable"));
        actor.assertCanManageOrganization(OrganizationId.of(organizationId));
        return actor;
    }

    private User requireAgentOf(long organizationId, long agentId) {
        User agent = users.findById(UserId.of(agentId))
                .orElseThrow(() -> new NotFoundException("Agent introuvable"));
        if (agent.organizationId() == null || agent.organizationId().value() != organizationId) {
            throw new ForbiddenException("L'agent n'appartient pas à l'organisation " + organizationId);
        }
        return agent;
    }

    private String generatePassword() {
        StringBuilder sb = new StringBuilder(14);
        for (int i = 0; i < 14; i++) {
            sb.append(CHARSET.charAt(RANDOM.nextInt(CHARSET.length())));
        }
        return sb.toString();
    }
}