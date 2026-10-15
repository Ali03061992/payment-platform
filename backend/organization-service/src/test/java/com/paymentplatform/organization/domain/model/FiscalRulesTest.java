package com.paymentplatform.organization.domain.model;

import com.paymentplatform.organization.domain.valueobject.MatriculeFiscal;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationName;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.organization.domain.valueobject.VatRate;
import com.paymentplatform.shared.domain.exception.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Moteur fiscal tunisien : ordre légal remise → FODEC → base TVA → TVA par taux → timbre.
 * JUnit pur (aucun contexte Spring).
 */
@DisplayName("Fiscalité tunisienne : calculs TVA/FODEC/timbre et 5 cas sans TVA.")
class FiscalRulesTest {

    @Test
    @DisplayName("TVA 19% standard sur HT net")
    void computeLine_standard19() {
        var r = FiscalRules.computeLine(new BigDecimal("1000"), 1, BigDecimal.ZERO, new BigDecimal("19"), false, false);
        assertThat(r.netHt()).isEqualByComparingTo("1000.0000");
        assertThat(r.fodec()).isEqualByComparingTo("0");
        assertThat(r.base()).isEqualByComparingTo("1000.0000");
        assertThat(r.vat()).isEqualByComparingTo("190.0000");
        assertThat(r.ttc()).isEqualByComparingTo("1190.0000");
    }

    @Test
    @DisplayName("FODEC 1% intégré dans la base TVA (pas sur le HT seul)")
    void computeLine_fodecInVatBase() {
        var r = FiscalRules.computeLine(new BigDecimal("1000"), 1, BigDecimal.ZERO, new BigDecimal("19"), true, false);
        assertThat(r.fodec()).isEqualByComparingTo("10.0000");
        assertThat(r.base()).isEqualByComparingTo("1010.0000");
        assertThat(r.vat()).isEqualByComparingTo("191.9000");
        assertThat(r.ttc()).isEqualByComparingTo("1201.9000");
    }

    @Test
    @DisplayName("Remise ligne déduite AVANT TVA, sans remise globale (2000 - 200, TVA 19%)")
    void computeLine_lineDiscountBeforeVat() {
        var r = FiscalRules.computeLine(new BigDecimal("1000"), 2, new BigDecimal("200"),
                new BigDecimal("19"), false, false);
        assertThat(r.netHt()).isEqualByComparingTo("1800.0000");
        assertThat(r.vat()).isEqualByComparingTo("342.0000");
        assertThat(r.ttc()).isEqualByComparingTo("2142.0000");
    }

    @Test
    @DisplayName("Prix TTC : TVA déduite, rien ajouté (1190 TTC 19% → HT 1000)")
    void computeLine_priceTtc() {
        var r = FiscalRules.computeLine(new BigDecimal("1190"), 1, BigDecimal.ZERO, new BigDecimal("19"), false, true);
        assertThat(r.netHt()).isEqualByComparingTo("1000.0000");
        assertThat(r.vat()).isEqualByComparingTo("190.0000");
        assertThat(r.ttc()).isEqualByComparingTo("1190.0000");
        assertThat(r.htUnitPrice()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Prix TTC 7% : 107 → HT 100 + TVA 7")
    void computeLine_priceTtcReduced() {
        var r = FiscalRules.computeLine(new BigDecimal("107"), 2, BigDecimal.ZERO, new BigDecimal("7"), false, true);
        assertThat(r.netHt()).isEqualByComparingTo("200.0000");
        assertThat(r.vat()).isEqualByComparingTo("14.0000");
    }

    @Test
    @DisplayName("Taux réduits 7% et 13%")
    void computeLine_reducedRates() {
        var seven = FiscalRules.computeLine(new BigDecimal("500"), 1, BigDecimal.ZERO, new BigDecimal("7"), false, false);
        assertThat(seven.vat()).isEqualByComparingTo("35.0000");
        var thirteen = FiscalRules.computeLine(new BigDecimal("1000"), 1, BigDecimal.ZERO, new BigDecimal("13"), false, false);
        assertThat(thirteen.vat()).isEqualByComparingTo("130.0000");
    }

    @Test
    @DisplayName("Ventilation par taux : construction + relecture (roundtrip)")
    void vatBreakdown_roundtrip() {
        Map<BigDecimal, FiscalRules.VatLine> byRate = new LinkedHashMap<>();
        byRate = FiscalRules.accumulate(byRate, new BigDecimal("19"), new BigDecimal("1000"), new BigDecimal("190"));
        byRate = FiscalRules.accumulate(byRate, new BigDecimal("7"), new BigDecimal("500"), new BigDecimal("35"));
        byRate = FiscalRules.accumulate(byRate, new BigDecimal("19"), new BigDecimal("200"), new BigDecimal("38"));
        String canonical = FiscalRules.buildVatBreakdown(byRate);
        List<FiscalRules.VatLine> lines = FiscalRules.parseVatBreakdown(canonical);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0).rate()).isEqualByComparingTo("19");
        assertThat(lines.get(0).base()).isEqualByComparingTo("1200");
        assertThat(lines.get(0).amount()).isEqualByComparingTo("228");
        assertThat(lines.get(1).rate()).isEqualByComparingTo("7");
    }

    @Test
    @DisplayName("Timbre : 1 DT sauf export à 0")
    void stamp_exportZeroOtherwiseOne() {
        assertThat(FiscalRules.stampFor(FiscalRegime.EXPORT)).isEqualByComparingTo("0");
        assertThat(FiscalRules.stampFor(FiscalRegime.ASSUJETTI)).isEqualByComparingTo("1.000");
        assertThat(FiscalRules.stampFor(FiscalRegime.SUSPENDU)).isEqualByComparingTo("1.000");
    }

    @Test
    @DisplayName("Mention suspension porte le n° d'attestation (art. 11)")
    void exemptionMention_suspension() {
        String mention = FiscalRules.exemptionMention(FiscalRegime.SUSPENDU, "ATT-2026-001",
                LocalDate.of(2026, 3, 15));
        assertThat(mention).contains("suspension").contains("ATT-2026-001").contains("art. 11");
        assertThat(FiscalRules.exemptionMention(FiscalRegime.EXPORT, null, null))
                .contains("exportation");
        assertThat(FiscalRules.exemptionMention(FiscalRegime.ASSUJETTI, null, null)).isNull();
    }

    @Test
    @DisplayName("VatRate : 0/7/13/19 légaux, 20 rejeté, null → 19")
    void vatRate_validation() {
        assertThat(VatRate.of(new BigDecimal("7")).value()).isEqualByComparingTo("7");
        assertThat(VatRate.of(null).value()).isEqualByComparingTo("19");
        assertThat(VatRate.exempt().isExempt()).isTrue();
        assertThatThrownBy(() -> VatRate.of(new BigDecimal("20")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Matricule fiscal : format canonique, code N hors TVA")
    void matriculeFiscal_codes() {
        var mf = MatriculeFiscal.of("1234567A/A/M/000");
        assertThat(mf.isCanonical()).isTrue();
        assertThat(mf.vatCode()).isEqualTo("A");
        assertThat(mf.isOutsideVatScope()).isFalse();
        var n = MatriculeFiscal.of("7654321B/N/M/000");
        assertThat(n.isOutsideVatScope()).isTrue();
        assertThatThrownBy(() -> MatriculeFiscal.of("  ")).isInstanceOf(DomainException.class);
    }

    private Organization buyer(FiscalRegime regime) {
        Organization org = Organization.create(OrganizationId.of(UUID.randomUUID()),
                OrganizationName.of("Acheteur Test"), OrganizationType.SHOP);
        if (regime == FiscalRegime.SUSPENDU) {
            org.updateFiscalProfile("1234567A/A/M/000", null, "Tunis", regime,
                    "ATT-2026-001", LocalDate.now().minusDays(1), LocalDate.now().plusYears(1));
        } else {
            org.updateFiscalProfile("1234567A/A/M/000", null, "Tunis", regime, null, null, null);
        }
        return org;
    }

    private OrderItem item(String name, String price, int qty, String vatRate, boolean fodec) {
        OrderItem item = new OrderItem();
        item.setOrderId(UUID.randomUUID());
        item.setProductId(UUID.randomUUID());
        item.setProductName(name);
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal(price));
        item.setDiscount(BigDecimal.ZERO);
        item.setVatRate(new BigDecimal(vatRate));
        item.setFodecApplicable(fodec);
        return item;
    }

    private Order order() {
        return Order.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "SHOP_ADMIN", "SHOP", false, "TND", null);
    }

    @Test
    @DisplayName("Commande mixte 19% + 7% avec FODEC : totaux recomposés au millime")
    void order_mixedRates_withFodec() {
        Order order = order();
        List<OrderItem> items = List.of(
                item("Conseil", "1000", 1, "19", false),
                item("Alimentaire", "500", 1, "7", false),
                item("Matériel", "5000", 1, "19", true));
        OrderFiscalizer.apply(order, items, buyer(FiscalRegime.ASSUJETTI));

        // HT net = 1000 + 500 + 5000 = 6500 ; FODEC = 50 ; TVA = 190 + 35 + 959.5 = 1184.5
        assertThat(order.getSubtotal()).isEqualByComparingTo("6500.0000");
        assertThat(order.getFodecTotal()).isEqualByComparingTo("50.0000");
        assertThat(order.getTaxAmount()).isEqualByComparingTo("1184.5000");
        assertThat(order.getTotal()).isEqualByComparingTo("7734.5000");
        assertThat(order.getStampAmount()).isEqualByComparingTo("1.000");
        assertThat(order.getNetPayable()).isEqualByComparingTo("7735.5000");
        assertThat(order.getVatBreakdown()).contains("19|").contains("7|");
    }

    @Test
    @DisplayName("Suspension sans attestation valide → refus (art. 11)")
    void fiscalizer_suspendedWithoutAttestation_throws() {
        Order order = order();
        Organization noAtt = Organization.create(OrganizationId.of(UUID.randomUUID()),
                OrganizationName.of("Sans Attestation"), OrganizationType.SHOP);
        // Force le régime sans passer par la validation (cas base corrompue) :
        // on attend le refus du fiscaliseur.
        org.springframework.test.util.ReflectionTestUtils.setField(noAtt, "fiscalRegime", FiscalRegime.SUSPENDU);
        assertThatThrownBy(() -> OrderFiscalizer.apply(order, List.of(item("X", "100", 1, "19", false)), noAtt))
                .hasMessageContaining("suspension");
    }

    @Test
    @DisplayName("Suspension valide → 0%, sans FODEC, mention art. 11")
    void fiscalizer_suspended_zeroVatWithMention() {
        Order order = order();
        List<OrderItem> items = List.of(item("Matériel", "1000", 1, "19", true));
        OrderFiscalizer.apply(order, items, buyer(FiscalRegime.SUSPENDU));
        assertThat(order.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(order.getFodecTotal()).isEqualByComparingTo("0");
        assertThat(order.getTotal()).isEqualByComparingTo("1000.0000");
        assertThat(order.getFiscalMention()).contains("ATT-2026-001");
        assertThat(order.getExemptionReason()).isEqualTo("SUSPENDU:ATT-2026-001");
    }

    @Test
    @DisplayName("Export → 0% et timbre 0")
    void fiscalizer_export_zeroVatZeroStamp() {
        Order order = order();
        OrderFiscalizer.apply(order, List.of(item("X", "1000", 1, "19", false)), buyer(FiscalRegime.EXPORT));
        assertThat(order.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(order.getStampAmount()).isEqualByComparingTo("0");
        assertThat(order.getNetPayable()).isEqualByComparingTo("1000.0000");
        assertThat(order.getFiscalMention()).contains("exportation");
    }

    @Test
    @DisplayName("Non-assujetti et forfaitaire : TVA du fournisseur reste due")
    void fiscalizer_nonAssujetti_collectsVat() {
        for (FiscalRegime regime : List.of(FiscalRegime.NON_ASSUJETTI, FiscalRegime.FORFAITAIRE,
                FiscalRegime.ETAT_PUBLIC)) {
            Order order = order();
            OrderFiscalizer.apply(order, List.of(item("X", "1000", 1, "19", false)), buyer(regime));
            assertThat(order.getTaxAmount()).isEqualByComparingTo("190.0000");
        }
    }
}
