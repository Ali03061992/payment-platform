package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.application.dto.WithholdingCertificateResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Fichier d'échange des certificats de retenue (dépôt TEJ), rattachés aux
 * FACTURES DE COMMANDES. Structure documentée : mentions art. 55 IRPP/IS +
 * arrêté du 10 mai 2024 (réf/date/nature de l'opération, exercice).
 * Le schéma XSD officiel du ministère des Finances reste la référence en cas
 * d'écart : valider le premier dépôt en environnement de test TEJ.
 */
public final class WithholdingXmlService {

    private WithholdingXmlService() {}

    public static String buildBatch(List<WithholdingCertificateResponse> certificates) {
        return buildBatch(certificates, null);
    }

    /**
     * Lot mensuel avec commentaire d'identification du déposant (ignoré par les
     * validateurs XSD, utile pour tracer le compte TEJ utilisé au dépôt).
     */
    public static String buildBatch(List<WithholdingCertificateResponse> certificates, String depositorTejId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (depositorTejId != null && !depositorTejId.isBlank()) {
            sb.append("<!-- deposant TEJ: ").append(esc(depositorTejId.strip())).append(" -->\n");
        }
        sb.append("<certificatsRetenue version=\"1.0\" xmlns=\"urn:tn:finances:tej:retenue:v1\">\n");
        for (WithholdingCertificateResponse c : certificates) {
            sb.append(buildCertificate(c));
        }
        sb.append("</certificatsRetenue>\n");
        return sb.toString();
    }

    public static String buildCertificate(WithholdingCertificateResponse c) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <certificat>\n");
        sb.append("    <numero>").append(esc(c.number())).append("</numero>\n");
        sb.append("    <debiteur>\n");
        sb.append("      <matriculeFiscal>").append(esc(orEmpty(c.debtorMatricule()))).append("</matriculeFiscal>\n");
        sb.append("      <raisonSociale>").append(esc(orEmpty(c.debtorName()))).append("</raisonSociale>\n");
        sb.append("      <adresse>").append(esc(orEmpty(c.debtorAddress()))).append("</adresse>\n");
        sb.append("    </debiteur>\n");
        sb.append("    <beneficiaire>\n");
        sb.append("      <matriculeFiscal>").append(esc(orEmpty(c.beneficiaryMatricule()))).append("</matriculeFiscal>\n");
        sb.append("      <raisonSociale>").append(esc(orEmpty(c.beneficiaryName()))).append("</raisonSociale>\n");
        sb.append("      <adresse>").append(esc(orEmpty(c.beneficiaryAddress()))).append("</adresse>\n");
        sb.append("    </beneficiaire>\n");
        sb.append("    <facture>\n");
        sb.append("      <reference>").append(esc(c.orderReference())).append("</reference>\n");
        sb.append("      <date>").append(c.orderDate()).append("</date>\n");
        sb.append("      <nature>").append(esc(c.nature())).append("</nature>\n");
        sb.append("      <exerciceFacturation>").append(esc(orEmpty(c.billingPeriod()))).append("</exerciceFacturation>\n");
        sb.append("    </facture>\n");
        sb.append("    <montants>\n");
        sb.append("      <brut>").append(fmt(c.grossAmount())).append("</brut>\n");
        sb.append("      <taux>").append(fmt(c.rate())).append("</taux>\n");
        sb.append("      <retenue>").append(fmt(c.withholdingAmount())).append("</retenue>\n");
        sb.append("      <net>").append(fmt(c.netAmount())).append("</net>\n");
        sb.append("      <devise>").append(esc(orEmpty(c.currency()))).append("</devise>\n");
        sb.append("    </montants>\n");
        sb.append("    <echeance>").append(c.dueDate()).append("</echeance>\n");
        if (c.tejReference() != null && !c.tejReference().isBlank()) {
            sb.append("    <referenceTej>").append(esc(c.tejReference())).append("</referenceTej>\n");
        }
        sb.append("  </certificat>\n");
        return sb.toString();
    }

    private static String fmt(BigDecimal v) {
        if (v == null) return "0.000";
        return String.format(java.util.Locale.ROOT, "%.3f", v);
    }

    private static String orEmpty(String v) {
        return v != null ? v : "";
    }

    private static String esc(String v) {
        if (v == null) return "";
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
