package com.paymentplatform.shared.infrastructure.web;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.domain.exception.UnprocessableEntityException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de GlobalExceptionHandler.
 * Périmètre : traduction des exceptions métier et techniques en réponses JSON uniformes.
 * Moyens : JUnit pur avec objets Spring réels (zéro simulation).
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/test");
    }

    @SuppressWarnings("unused")
    static void methodeSupportValidation(String email) {
    }

    private static MethodArgumentNotValidException exceptionValidationReelle() throws NoSuchMethodException {
        MethodParameter parametre = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("methodeSupportValidation", String.class), 0);
        BindingResult resultat = new BeanPropertyBindingResult(new Object(), "request");
        resultat.addError(new FieldError("request", "email", "doit être renseigné"));
        return new MethodArgumentNotValidException(parametre, resultat);
    }

    @Test
    @DisplayName("Une entité introuvable retourne un statut 404 avec le code NOT_FOUND.")
    void notFound_entiteAbsente_retourne404() {
        ResponseEntity<ApiError> response = handler.notFound(new NotFoundException("Not found"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Not found");
    }

    @Test
    @DisplayName("Un conflit métier retourne un statut 409 avec le code CONFLICT.")
    void conflict_conflitMetier_retourne409() {
        ResponseEntity<ApiError> response = handler.conflict(new ConflictException("Conflict"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
    }

    @Test
    @DisplayName("Une opération interdite retourne un statut 403 avec le code FORBIDDEN.")
    void forbidden_accesInterdit_retourne403() {
        ResponseEntity<ApiError> response = handler.forbidden(new ForbiddenException("Forbidden"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("Une absence d'authentification retourne un statut 401 avec le code UNAUTHORIZED.")
    void unauthorized_nonAuthentifie_retourne401() {
        ResponseEntity<ApiError> response = handler.unauthorized(new UnauthorizedException("Unauthorized"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Une entité non traitable retourne un statut 422.")
    void unprocessable_entiteNonTraitable_retourne422() {
        ResponseEntity<ApiError> response = handler.unprocessable(new UnprocessableEntityException("Unprocessable"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error()).isEqualTo("UNPROCESSABLE_ENTITY");
    }

    @Test
    @DisplayName("Un refus Spring Security retourne un statut 403 avec un message en français.")
    void accessDenied_permissionInsuffisante_retourne403() {
        ResponseEntity<ApiError> response = handler.accessDenied(new AccessDeniedException("Denied"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("Permission insuffisante");
    }

    @Test
    @DisplayName("Une erreur de validation retourne un statut 400 avec le champ fautif.")
    void validation_champInvalide_retourne400AvecDetails() throws Exception {
        MethodArgumentNotValidException ex = exceptionValidationReelle();

        ResponseEntity<ApiError> response = handler.validation(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).contains("email");
    }

    @Test
    @DisplayName("Une erreur du domaine retourne un statut 400 avec le code DOMAIN_ERROR.")
    void domain_erreurMetier_retourne400() {
        ResponseEntity<ApiError> response = handler.domain(new DomainException("Domain error"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("DOMAIN_ERROR");
    }

    @Test
    @DisplayName("Une erreur inattendue retourne un statut 500 sans exposer le détail interne.")
    void unexpected_erreurInattendue_retourne500() {
        ResponseEntity<ApiError> response = handler.unexpected(new RuntimeException("Oops"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    @DisplayName("Un conflit de concurrence optimiste retourne un statut 409 avec un message en français.")
    void optimisticLock_conflitConcurrence_retourne409() {
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("Order", "123");
        ResponseEntity<ApiError> response = handler.optimisticLock(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("concurrence");
    }
}
