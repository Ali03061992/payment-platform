package com.paymentplatform.shared.infrastructure.web;

import java.time.Instant;

/** Corps d'erreur uniforme de l'API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {
}