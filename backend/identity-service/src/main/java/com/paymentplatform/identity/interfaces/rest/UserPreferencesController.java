package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.UpdatePreferencesRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.UserPreferencesUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserPreferencesController {

    private final UserPreferencesUseCase preferences;

    public UserPreferencesController(UserPreferencesUseCase preferences) {
        this.preferences = preferences;
    }

    @PatchMapping("/me/preferences")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponse> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(preferences.updatePreferences(request));
    }
}
