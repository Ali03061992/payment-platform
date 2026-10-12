package com.paymentplatform.organization.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Règles de calcul fiscal tunisiennes (ordre légal impératif).
 *
 * <ol>
 *   <li>Brut = quantité × prix unitaire HT</li>
 *   <li>Remise ligne (montant) → total ligne HT</li>
 *   <li>Remise globale (%) → HT net (la remise s'impute AVANT la TVA)</li>
 *   <li>FODEC 1% du HT net, lignes concernées uniquement</li>
 *   <li>Base TVA = HT net + FODEC (le FODEC entre dans la base TVA)</li>
 *   <li>TVA par taux (19 / 13 / 7 / 0 avec motif)</li>
 *   <li>TTC = base + TVA ; timbre +1,000 DT ajouté APRÈS, hors toute base</li>
 * </ol>
 *
 * Sources : Code TVA (taux, art. 11 suspension, art. 18 mentions),
 * LF 2023 (timbre 1 DT), LF 2026 art. 20 (barème grandes surfaces, hors scope P0).
 */
public final class FiscalRules {

    private FiscalRules() {}

    public static final BigDecimal FODEC_RATE_PCT = new BigDecimal("1");
    public static final BigDecimal STAMP_AMOUNT = new BigDecimal("1.000");
    public static final BigDecimal STAMP_EXPORT = BigDecimal.ZERO;
    public static final int SCALE = 4;

    /** Résultat fiscal d'une ligne après imputation de la remise globale. */
    public record LineResult(BigDecimal netHt, BigDecimal fodec, BigDecimal base, BigDecimal vat, BigDecimal ttc,
                             BigDecimal htUnitPrice) {}

    /** Ventilation TVA par taux pour le pied de facture. */
    public record VatLine(BigDecimal rate, BigDecimal base, BigDecimal amount) {}

    public static LineResult computeLine(BigDecimal unitPrice, int quantity, BigDecimal lineDiscount,
                                         BigDecimal globalDiscountPct, BigDecimal vatRate,
                                         boolean fodecApplicable, boolean priceTtc) {
        BigDecimal price = unitPrice != null ? unitPrice : BigDecimal.ZERO;
        BigDecimal discount = lineDiscount != null ? lineDiscount : BigDecimal.ZERO;
        BigDecimal globalPct = globalDiscountPct != null ? globalDiscountPct : BigDecimal.ZERO;
        BigDecimal rate = vatRate != null ? vatRate : new BigDecimal("19");

        // Prix saisi (HT ou TTC selon la fiche produit) après remise ligne.
        BigDecimal lineGross = price.multiply(BigDecimal.valueOf(quantity)).subtract(discount);
        if (lineGross.signum() < 0) {
            lineGross = BigDecimal.ZERO;
        }
        BigDecimal netEntered = applyPct(lineGross, BigDecimal.valueOf(100).subtract(globalPct));
        // Prix TTC : la TVA est INCLUSE, on la déduit au lieu de l'ajouter.
        BigDecimal netHt = priceTtc && rate.signum() > 0
                ? netEntered.divide(BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), SCALE + 2,
                        RoundingMode.HALF_UP)), SCALE, RoundingMode.HALF_UP)
                : netEntered;
        BigDecimal htUnitPrice = BigDecimal.ZERO;
        if (quantity > 0) {
            htUnitPrice = priceTtc && rate.signum() > 0
                    ? price.divide(BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), SCALE + 2,
                            RoundingMode.HALF_UP)), 2, RoundingMode.HALF_UP)
                    : price;
        }
        BigDecimal fodec = fodecApplicable ? applyPct(netHt, FODEC_RATE_PCT) : BigDecimal.ZERO;
        BigDecimal base = netHt.add(fodec);
        BigDecimal vat = applyPct(base, rate);
        return new LineResult(scale(netHt), scale(fodec), scale(base), scale(vat), scale(base.add(vat)),
                htUnitPrice.setScale(2, RoundingMode.HALF_UP));
    }

    /** Timbre fiscal du pied de facture : 0 pour l'export, 1,000 DT sinon (P0). */
    public static BigDecimal stampFor(FiscalRegime buyerRegime) {
        if (buyerRegime == FiscalRegime.EXPORT) {
            return STAMP_EXPORT;
        }
        return STAMP_AMOUNT;
    }

    /** Mention légale d'exonération à porter sur la facture (art. 11). */
    public static String exemptionMention(FiscalRegime buyerRegime, String attestationNumber,
                                          LocalDate attestationDate) {
        if (buyerRegime == FiscalRegime.SUSPENDU) {
            String date = attestationDate != null
                    ? attestationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "...";
            return "Vente en suspension de la taxe sur la valeur ajoutée suivant attestation n°"
                    + (attestationNumber != null ? attestationNumber : "...") + " du " + date
                    + " (art. 11 du Code TVA)";
        }
        if (buyerRegime == FiscalRegime.EXPORT) {
            return "Vente à l'exportation (art. 11 du Code TVA)";
        }
        return null;
    }

    /**
     * Ventilation TVA par taux au format canonique « taux|base|montant;... »,
     * taux triés par ordre décroissant (19, 13, 7, 0).
     */
    public static String buildVatBreakdown(Map<BigDecimal, VatLine> byRate) {
        List<BigDecimal> rates = new ArrayList<>(byRate.keySet());
        rates.sort((a, b) -> b.compareTo(a));
        StringBuilder sb = new StringBuilder();
        for (BigDecimal rate : rates) {
            VatLine line = byRate.get(rate);
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(strip(rate)).append('|')
                    .append(strip(line.base())).append('|')
                    .append(strip(line.amount()));
        }
        return sb.toString();
    }

    public static List<VatLine> parseVatBreakdown(String canonical) {
        List<VatLine> lines = new ArrayList<>();
        if (canonical == null || canonical.isBlank()) {
            return lines;
        }
        for (String part : canonical.split(";")) {
            String[] seg = part.split("\\|");
            if (seg.length == 3) {
                try {
                    lines.add(new VatLine(new BigDecimal(seg[0]), new BigDecimal(seg[1]), new BigDecimal(seg[2])));
                } catch (NumberFormatException ignored) {
                    // Segment corrompu : ignoré, la facture reste affichable.
                }
            }
        }
        return lines;
    }

    public static Map<BigDecimal, VatLine> accumulate(Map<BigDecimal, VatLine> byRate, BigDecimal rate,
                                                      BigDecimal base, BigDecimal amount) {
        Map<BigDecimal, VatLine> copy = new LinkedHashMap<>(byRate);
        VatLine prev = copy.get(rate);
        if (prev == null) {
            copy.put(rate, new VatLine(rate, scale(base), scale(amount)));
        } else {
            copy.put(rate, new VatLine(rate, scale(prev.base().add(base)), scale(prev.amount().add(amount))));
        }
        return copy;
    }

    private static BigDecimal applyPct(BigDecimal base, BigDecimal pct) {
        return base.multiply(pct).divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static String strip(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}
