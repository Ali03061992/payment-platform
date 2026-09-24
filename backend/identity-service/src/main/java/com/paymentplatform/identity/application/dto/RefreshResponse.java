package com.paymentplatform.identity.application.dto;

public record RefreshResponse(String accessToken, String tokenType, long expiresIn,
                              String refreshToken, long refreshExpiresIn) {

    public static RefreshResponse of(String accessToken, long expiresIn,
                                     String refreshToken, long refreshExpiresIn) {
        return new RefreshResponse(accessToken, "Bearer", expiresIn, refreshToken, refreshExpiresIn);
    }
}
