package com.paymentplatform.identity.application.dto;

import java.util.List;

/**
 * B5 : enveloppe de liste paginée (même contrat que les autres services :
 * {@code items}, {@code totalElements}, {@code totalPages}, {@code number}).
 */
public record PageResponse<T>(
        List<T> items,
        long totalElements,
        int totalPages,
        int number
) {
}
