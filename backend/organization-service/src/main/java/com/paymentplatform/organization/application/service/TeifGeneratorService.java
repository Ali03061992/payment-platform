package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.domain.model.FiscalRules;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Génère la facture électronique au format TEIF v1.8.8 (XML TTN/El Fatoora).
 * Référence : `docs/SPEC_EFACTURE_TTN.md` §4/§9 (XSD facture_INVOIC, codes I-*).
 * Conventions appliquées : MF sur 13 caractères sans séparateur, dates ddMMyy,
 * montants 3 décimales, TVA ventilée par taux, timbre séparé des taxes,
 * RefTtnVal remplie si la référence externe est connue (sinon vide, TTN la remplit).
 * PAS de bloc RAS (v1.8.8 : pas de I-1604) ni de ds:Signature (certificat client requis).
 */
public final class TeifGeneratorService {

    private TeifGeneratorService() {}

    private static final DateTimeFormatter TEIF_DATE =
            DateTimeFormatter.ofPattern("ddMMyy").withZone(ZoneId.systemDefault());

    /** Normalise un MF au format TEIF : 13 caractères, sans séparateur. */
    public static String normalizeMatricule(String mf) {
        if (mf == null) {
            return "";
        }
        String clean = mf.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (!clean.matches("[0-9]{7}[A-Z]{3}[0-9]{3}")) {
            throw new IllegalArgumentException("Matricule fiscal non convertible au format TEIF (13 caractères) : " + mf);
        }
        return clean;
    }

    /** Partie facture (fournisseur I-62 / acheteur I-64) pour le TEIF. */
    public record TeifParty(String name, String matriculeFiscal, String address) {}

    public static String buildInvoice(Order order, List<OrderItem> items,
                                      TeifParty supplier, TeifParty buyer) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<TEIF version=\"1.8.8\" controlingAgency=\"TTN\">\n");
        sb.append("  <InvoiceHeader>\n");
        sb.append("    <MessageSenderIdentifier type=\"I-01\">")
                .append(esc(normalizeMatricule(supplier.matriculeFiscal()))).append("</MessageSenderIdentifier>\n");
        sb.append("    <MessageRecieverIdentifier type=\"I-01\">")
                .append(esc(normalizeMatricule(buyer.matriculeFiscal()))).append("</MessageRecieverIdentifier>\n");
        sb.append("  </InvoiceHeader>\n");
        sb.append("  <InvoiceBody>\n");
        sb.append("    <Bgm>\n");
        sb.append("      <DocumentIdentifier>").append(esc(order.getReference())).append("</DocumentIdentifier>\n");
        sb.append("      <DocumentType code=\"I-11\">Facture</DocumentType>\n");
        sb.append("    </Bgm>\n");
        sb.append("    <Dtm>\n");
        sb.append("      <DateText format=\"ddMMyy\">").append(TEIF_DATE.format(order.getCreatedAt())).append("</DateText>\n");
        sb.append("    </Dtm>\n");
        sb.append("    <PartnerSection>\n");
        appendPartner(sb, "I-62", supplier);
        appendPartner(sb, "I-64", buyer);
        sb.append("    </PartnerSection>\n");
        sb.append("    <LinSection>\n");
        for (OrderItem item : items) {
            appendLine(sb, item, order.getCurrency());
        }
        sb.append("    </LinSection>\n");
        sb.append("    <InvoiceMoa>\n");
        sb.append("      <Moa amountTypeCode=\"I-176\" currencyIdentifier=\"").append(esc(order.getCurrency())).append("\">")
                .append(fmt(order.getSubtotal())).append("</Moa>\n");
        sb.append("      <Moa amountTypeCode=\"I-181\" currencyIdentifier=\"").append(esc(order.getCurrency())).append("\">")
                .append(fmt(order.getTaxAmount())).append("</Moa>\n");
        sb.append("      <Moa amountTypeCode=\"I-180\" currencyIdentifier=\"").append(esc(order.getCurrency())).append("\">")
                .append(fmt(order.getTotal())).append("</Moa>\n");
        sb.append("    </InvoiceMoa>\n");
        sb.append("    <TimbreFiscal currencyIdentifier=\"").append(esc(order.getCurrency())).append("\">")
                .append(fmt(order.getStampAmount())).append("</TimbreFiscal>\n");
        sb.append("    <InvoiceTax>\n");
        for (FiscalRules.VatLine line : FiscalRules.parseVatBreakdown(order.getVatBreakdown())) {
            sb.append("      <TaxDetail rate=\"").append(strip(line.rate())).append("\">\n");
            sb.append("        <BaseAmount>").append(fmt(line.base())).append("</BaseAmount>\n");
            sb.append("        <TaxAmount>").append(fmt(line.amount())).append("</TaxAmount>\n");
            sb.append("      </TaxDetail>\n");
        }
        sb.append("    </InvoiceTax>\n");
        sb.append("    <RefTtnVal>").append(esc(orEmpty(order.getExternalReference()))).append("</RefTtnVal>\n");
        sb.append("  </InvoiceBody>\n");
        sb.append("</TEIF>\n");
        return sb.toString();
    }

    private static void appendPartner(StringBuilder sb, String function, TeifParty org) {
        sb.append("      <Partner function=\"").append(function).append("\">\n");
        sb.append("        <Name>").append(esc(org.name())).append("</Name>\n");
        sb.append("        <Identifier>").append(esc(normalizeMatricule(org.matriculeFiscal()))).append("</Identifier>\n");
        sb.append("        <Address>").append(esc(orEmpty(org.address()))).append("</Address>\n");
        sb.append("      </Partner>\n");
    }

    private static void appendLine(StringBuilder sb, OrderItem item, String currency) {
        sb.append("      <Lin>\n");
        sb.append("        <ItemId>").append(esc(orEmpty(item.getProductRef()))).append("</ItemId>\n");
        sb.append("        <Imd>").append(esc(orEmpty(item.getProductName()))).append("</Imd>\n");
        sb.append("        <Quantity>").append(item.getQuantity()).append("</Quantity>\n");
        sb.append("        <Tax code=\"I-1602\" rate=\"").append(strip(item.getVatRate())).append("\"/>\n");
        sb.append("        <Moa>\n");
        sb.append("          <UnitPrice code=\"I-183\" currencyIdentifier=\"").append(esc(currency)).append("\">")
                .append(fmt(item.getHtUnitPrice() != null ? item.getHtUnitPrice() : item.getUnitPrice()))
                .append("</UnitPrice>\n");
        sb.append("          <LineTotal>").append(fmt(item.getLineTotal())).append("</LineTotal>\n");
        sb.append("        </Moa>\n");
        sb.append("      </Lin>\n");
    }

    private static String fmt(BigDecimal v) {
        if (v == null) {
            return "0.000";
        }
        return String.format(java.util.Locale.ROOT, "%.3f", v);
    }

    private static String strip(BigDecimal v) {
        if (v == null) {
            return "0";
        }
        return v.stripTrailingZeros().toPlainString();
    }

    private static String orEmpty(String v) {
        return v != null ? v : "";
    }

    private static String esc(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
