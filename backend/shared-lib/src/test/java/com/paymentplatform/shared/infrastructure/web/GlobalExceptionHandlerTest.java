package com.paymentplatform.shared.infrastructure.web;

import com.paymentplatform.shared.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest("GET", "/api/test");
    }

    @Test
    void notFound_returns404() {
        ResponseEntity<ApiError> response = handler.notFound(new NotFoundException("Not found"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Not found");
    }

    @Test
    void conflict_returns409() {
        ResponseEntity<ApiError> response = handler.conflict(new ConflictException("Conflict"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().error()).isEqualTo("CONFLICT");
    }

    @Test
    void forbidden_returns403() {
        ResponseEntity<ApiError> response = handler.forbidden(new ForbiddenException("Forbidden"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().error()).isEqualTo("FORBIDDEN");
    }

    @Test
    void unauthorized_returns401() {
        ResponseEntity<ApiError> response = handler.unauthorized(new UnauthorizedException("Unauthorized"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().error()).isEqualTo("UNAUTHORIZED");
    }

    @Test
    void unprocessable_returns422() {
        ResponseEntity<ApiError> response = handler.unprocessable(new UnprocessableEntityException("Unprocessable"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().error()).isEqualTo("UNPROCESSABLE_ENTITY");
    }

    @Test
    void accessDenied_returns403() {
        ResponseEntity<ApiError> response = handler.accessDenied(new AccessDeniedException("Denied"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().message()).isEqualTo("Permission insuffisante");
    }

    @Test
    void validation_returns400WithFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        FieldError fieldError = new FieldError("request", "email", "must not be blank");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ApiError> response = handler.validation(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().message()).contains("email");
    }

    @Test
    void domain_returns400() {
        ResponseEntity<ApiError> response = handler.domain(new DomainException("Domain error"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("DOMAIN_ERROR");
    }

    @Test
    void unexpected_returns500() {
        ResponseEntity<ApiError> response = handler.unexpected(new RuntimeException("Oops"), request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void optimisticLock_returns409() {
        org.springframework.orm.ObjectOptimisticLockingFailureException ex =
                mock(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        ResponseEntity<ApiError> response = handler.optimisticLock(ex, request);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("concurrence");
    }
}
