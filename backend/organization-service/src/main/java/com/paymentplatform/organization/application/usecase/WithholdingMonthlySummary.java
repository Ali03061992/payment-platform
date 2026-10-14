package com.paymentplatform.organization.application.usecase;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Synthèse mensuelle des retenues d'un débiteur : sert à réconcilier la déclaration TEJ.
 * Annulés exclus ; montants en dinars.
 */
public record WithholdingMonthlySummary(
        String month,
        int certificateCount,
        BigDecimal grossTotal,
        BigDecimal withholdingTotal,
        Map<String, BigDecimal> byNature) {}
