package com.paymentplatform.identity.application.dto;

import java.util.List;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

    public static LoginResponse of(String accessToken, long expiresIn, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, user);
    }
}