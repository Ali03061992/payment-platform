package com.paymentplatform.shared.domain.model;

import java.util.List;

/**
 * B5 : page de résultats agnostique de l'infrastructure (pas de dépendance
 * Spring Data dans le domaine). {@code items} = contenu borné de la page,
 * {@code totalElements} = total en base pour construire les enveloppes HTTP.
 */
public record PageResult<T>(List<T> items, long totalElements) {

    /**
     * Construit une page vide (aucun élément, total nul).
     *
     * @param <T> type des éléments
     * @return page vide
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0);
    }
}
