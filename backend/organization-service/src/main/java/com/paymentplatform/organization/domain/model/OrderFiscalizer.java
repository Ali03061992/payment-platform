package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.ConflictException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Applique le régime fiscal de l'acheteur à une commande (décision centralisée).
 *
 * <ul>
 *   <li>SUSPENDU (attestation valide) / EXPORT → lignes à 0%, sans FODEC,
 *       mention légale art. 11, timbre 0 à l'export.</li>
 *   <li>ASSUJETTI / FORFAITAIRE / NON_ASSUJETTI / ETAT_PUBLIC → TVA des lignes
 *       conservée (le fournisseur collecte même vers un non-assujetti).</li>
 * </ul>
 *
 * La TVA par ligne et le flag FODEC doivent être snapshotés depuis la fiche
 * produit AVANT cet appel. La remise globale, le FODEC dans la base TVA et la
 * ventilation par taux sont calculés par {@link Order#recalculateTotals(List)}.
 */
public final class OrderFiscalizer {

    private OrderFiscalizer() {}

    public static void apply(Order order, List<OrderItem> items, Organization buyer) {
        FiscalRegime regime = buyer != null ? buyer.fiscalRegime() : FiscalRegime.ASSUJETTI;

        if (regime == FiscalRegime.SUSPENDU && (buyer == null || !buyer.hasValidSuspension())) {
            throw new ConflictException("Vente en suspension refusée : attestation manquante ou expirée "
                    + "(art. 11 du Code TVA). Régularisez le profil fiscal de l'acheteur.");
        }

        if (regime.isVatExemptSale()) {
            for (OrderItem item : items) {
                item.setVatRate(BigDecimal.ZERO);
                item.setFodecApplicable(false);
            }
        }

        order.setBuyerRegime(regime.name());
        order.setStampAmount(FiscalRules.stampFor(regime));
        String attNumber = buyer != null ? buyer.suspensionAttestationNumber() : null;
        var attDate = buyer != null ? buyer.suspensionAttestationDate() : null;
        order.setFiscalMention(FiscalRules.exemptionMention(regime, attNumber, attDate));
        order.setExemptionReason(regime.isVatExemptSale()
                ? (regime == FiscalRegime.SUSPENDU ? "SUSPENDU:" + attNumber : "EXPORT")
                : null);

        order.recalculateTotals(items);
    }
}
