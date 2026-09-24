package com.paymentplatform.identity.application.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user,
                              String refreshToken, long refreshExpiresIn) {

    public static LoginResponse of(String accessToken, long expiresIn, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, user, null, 0);
    }

    public static LoginResponse of(String accessToken, long expiresIn, UserResponse user,
                                   String refreshToken, long refreshExpiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn, user, refreshToken, refreshExpiresIn);
    }
}