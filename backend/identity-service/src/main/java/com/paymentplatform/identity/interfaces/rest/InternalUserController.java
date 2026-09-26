package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.InternalUserCreationUseCase;
import com.paymentplatform.identity.application.usecase.UserQueryUseCase;
import com.paymentplatform.identity.infrastructure.http.InternalAuthGuard;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Endpoint interne (secret partagé) utilisé par Organization Service. */
@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final InternalUserCreationUseCase useCase;
    private final UserQueryUseCase query;
    private final InternalAuthGuard guard;

    public InternalUserController(InternalUserCreationUseCase useCase,
                                   UserQueryUseCase query,
                                   InternalAuthGuard guard) {
        this.useCase = useCase;
        this.query = query;
        this.guard = guard;
    }

    /**
     * Crée un utilisateur interne pour le compte d'un autre microservice.
     *
     * @param token secret interne partagé
     * @param request données du compte à créer
     * @return utilisateur créé (201)
     */
    @PostMapping
    public ResponseEntity<UserResponse> create(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @Valid @RequestBody CreateInternalUserRequest request) {
        // B3 : 401 unifié (manquant OU invalide), comme InternalOrganizationController.
        if (!guard.isValid(token)) {
            throw new UnauthorizedException("Secret interne invalide ou manquant");
        }
        return ResponseEntity.status(201).body(useCase.createInternalUser(request));
    }

    /**
     * Récupère un utilisateur par identifiant pour un appel inter-services.
     *
     * @param token secret interne partagé
     * @param id identifiant de l'utilisateur
     * @return profil de l'utilisateur
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable UUID id) {
        // B3 : 401 unifié (manquant OU invalide), comme InternalOrganizationController.
        if (!guard.isValid(token)) {
            throw new UnauthorizedException("Secret interne invalide ou manquant");
        }
        return ResponseEntity.ok(query.findByIdInternal(id));
    }

    /**
     * Liste les utilisateurs d'une organisation pour un appel inter-services.
     *
     * @param token secret interne partagé
     * @param organizationId organisation filtrée
     * @return utilisateurs de l'organisation
     */
    @GetMapping
    public ResponseEntity<java.util.List<UserResponse>> listByOrganization(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestParam UUID organizationId) {
        if (!guard.isValid(token)) {
            throw new UnauthorizedException("Secret interne invalide ou manquant");
        }
        return ResponseEntity.ok(query.listByOrganizationInternal(organizationId));
    }
}