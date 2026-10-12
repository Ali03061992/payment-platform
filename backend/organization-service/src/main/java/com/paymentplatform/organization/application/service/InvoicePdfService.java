package com.paymentplatform.organization.application.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.paymentplatform.organization.domain.model.FiscalRules;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Facture PDF conforme au droit tunisien (Code TVA art. 18 §II et art. 11).
 * Mentions : MF fournisseur et client (sauf particuliers), adresse des parties,
 * TVA ventilée PAR TAUX de ligne, FODEC, timbre fiscal sur ligne distincte,
 * mention de suspension/export le cas échéant, numérotation du document.
 */
@Service
public class InvoicePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(15, 52, 96));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 52, 96));
    private static final Font MENTION_FONT = new Font(Font.HELVETICA, 9, Font.BOLDITALIC, new Color(120, 30, 30));

    /**
     * Lignes de remplissage du tableau articles : la facture garde l'allure d'un
     * document même à une seule ligne, tout en tenant avec totaux, mentions et
     * signatures sur UNE page (les totaux restent insécables via setKeepTogether).
     */
    private static final int MIN_ITEM_ROWS = 5;

    /** Partie à la facture (fournisseur ou client) avec son profil fiscal. */
    public record InvoiceParty(String name, String address, String matriculeFiscal, String patente,
                               String fiscalRegime, String attestationNumber, LocalDate attestationDate) {
        public static InvoiceParty named(String name) {
            return new InvoiceParty(name, null, null, null, null, null, null);
        }
    }

    /** Compatibilité : facture avec seuls les noms (mentions fiscales réduites). */
    public byte[] generateInvoicePdf(Order order, List<OrderItem> items, String supplierName, String shopName) {
        return generateInvoicePdf(order, items, InvoiceParty.named(supplierName), InvoiceParty.named(shopName));
    }

    public byte[] generateInvoicePdf(Order order, List<OrderItem> items,
                                     InvoiceParty supplier, InvoiceParty buyer) {
        Document document = new Document(PageSize.A4, 36, 36, 44, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new FooterEvent());
            document.open();

            addHeader(document, order, supplier, buyer);
            addItemsTable(document, items, order);
            addTotals(document, order);
            addFiscalMention(document, order);
            addLegalMentions(document, order);
            addSignatures(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }

        return out.toByteArray();
    }

    /** Pied de page sur chaque page : plateforme + pagination. */
    private static class FooterEvent extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer, Document document) {
            try {
                com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
                Phrase footer = new Phrase("Payment Platform - Facture " + writer.getPageNumber(),
                        SMALL_FONT);
                com.lowagie.text.pdf.ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                        (document.right() + document.left()) / 2, document.bottom() - 18, 0);
            } catch (Exception ignored) {
                // Le pied de page ne doit jamais faire échouer la facture.
            }
        }
    }

    private void addHeader(Document document, Order order, InvoiceParty supplier, InvoiceParty buyer)
            throws DocumentException {
        Paragraph title = new Paragraph("FACTURE", TITLE_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        document.add(new Paragraph(" "));

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Facture n°: " + order.getReference(), BOLD_FONT));
        leftCell.addElement(new Paragraph("Date d'opération: " + formatDate(order.getCreatedAt()), NORMAL_FONT));
        if (order.getDeliveredAt() != null) {
            leftCell.addElement(new Paragraph("Date de livraison: " + formatDate(order.getDeliveredAt()), NORMAL_FONT));
        }
        leftCell.addElement(new Paragraph("Devise: " + order.getCurrency(), NORMAL_FONT));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        addElectronicReference(rightCell, order);
        rightCell.addElement(new Paragraph("Fournisseur:", BOLD_FONT));
        addPartyLines(rightCell, supplier);
        rightCell.addElement(new Paragraph(" ", NORMAL_FONT));
        rightCell.addElement(new Paragraph("Client:", BOLD_FONT));
        addPartyLines(rightCell, buyer);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    /**
     * Référence électronique externe (TTN/El Fatoora, TEJ) avec QR en haut à droite,
     * comme les outils tunisiens (référence unique + QR de validation).
     */
    private void addElectronicReference(PdfPCell cell, Order order) {
        if (order.getExternalReference() == null || order.getExternalReference().isBlank()) {
            return;
        }
        try {
            com.google.zxing.common.BitMatrix matrix = new com.google.zxing.qrcode.QRCodeWriter().encode(
                    "FACTURE:" + order.getExternalReference() + "|DOC:" + order.getReference(),
                    com.google.zxing.BarcodeFormat.QR_CODE, 90, 90);
            java.awt.image.BufferedImage image =
                    new java.awt.image.BufferedImage(90, 90, java.awt.image.BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 90; x++) {
                for (int y = 0; y < 90; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            com.lowagie.text.Image qr = com.lowagie.text.Image.getInstance(image, null);
            qr.setAlignment(Element.ALIGN_RIGHT);
            cell.addElement(qr);
        } catch (Exception ignored) {
            // Le QR ne doit jamais faire échouer la facture.
        }
        Paragraph ref = new Paragraph("Réf. électronique: " + order.getExternalReference(), BOLD_FONT);
        ref.setAlignment(Element.ALIGN_RIGHT);
        cell.addElement(ref);
    }

    private void addPartyLines(PdfPCell cell, InvoiceParty party) {
        cell.addElement(new Paragraph(party.name() != null ? party.name() : "-", NORMAL_FONT));
        if (party.address() != null && !party.address().isBlank()) {
            cell.addElement(new Paragraph(party.address(), NORMAL_FONT));
        }
        cell.addElement(new Paragraph("MF: " + (party.matriculeFiscal() != null ? party.matriculeFiscal() : "—"),
                party.matriculeFiscal() != null ? NORMAL_FONT : SMALL_FONT));
        if (party.patente() != null && !party.patente().isBlank()) {
            cell.addElement(new Paragraph("Patente: " + party.patente(), NORMAL_FONT));
        }
        if (party.attestationNumber() != null && !party.attestationNumber().isBlank()) {
            String date = party.attestationDate() != null
                    ? party.attestationDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "...";
            cell.addElement(new Paragraph("Attestation suspension n°" + party.attestationNumber() + " du " + date,
                    SMALL_FONT));
        }
    }

    private void addItemsTable(Document document, List<OrderItem> items, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{32, 8, 17, 12, 11, 20});
        table.setSpacingBefore(10f);
        table.setHeaderRows(1);
        table.setSplitLate(true);

        addTableHeader(table, "Produit");
        addTableHeader(table, "Qté");
        addTableHeader(table, "PU HT");
        addTableHeader(table, "Remise");
        addTableHeader(table, "TVA");
        addTableHeader(table, "Total HT");

        for (OrderItem item : items) {
            addBodyCell(table, item.getProductName(), Element.ALIGN_LEFT, NORMAL_FONT);
            addBodyCell(table, String.valueOf(item.getQuantity()), Element.ALIGN_CENTER, NORMAL_FONT);
            addBodyCell(table,
                    formatAmount(item.getUnitPrice()) + (item.isPriceTtc() ? " TTC" : ""),
                    Element.ALIGN_RIGHT, NORMAL_FONT);
            addBodyCell(table,
                    item.getDiscount() != null && item.getDiscount().signum() > 0
                            ? formatAmount(item.getDiscount()) : "-",
                    Element.ALIGN_CENTER, NORMAL_FONT);
            addBodyCell(table, formatRate(item.getVatRate()), Element.ALIGN_CENTER, NORMAL_FONT);
            addBodyCell(table, formatAmount(item.getLineTotal()), Element.ALIGN_RIGHT, BOLD_FONT);
        }

        // Remplissage : la facture occupe toujours toute la page, même à une seule ligne.
        int missing = MIN_ITEM_ROWS - items.size();
        for (int i = 0; i < missing; i++) {
            for (int c = 0; c < 6; c++) {
                addBodyCell(table, " ", c == 0 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER, NORMAL_FONT);
            }
        }

        document.add(table);
        if (order.getGlobalDiscount() != null && order.getGlobalDiscount().signum() > 0) {
            Paragraph discount = new Paragraph("Remise globale : " + formatRate(order.getGlobalDiscount()),
                    SMALL_FONT);
            discount.setAlignment(Element.ALIGN_RIGHT);
            document.add(discount);
        }
    }

    private void addBodyCell(PdfPTable table, String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        cell.setBorderWidth(0.5f);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String text) {
        PdfPCell header = new PdfPCell(new Phrase(text, HEADER_FONT));
        header.setBackgroundColor(new Color(15, 52, 96));
        header.setPadding(6f);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }

    private void addTotals(Document document, Order order) throws DocumentException {
        document.add(new Paragraph(" "));

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(55);
        totalsTable.setWidths(new float[]{60, 40});
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        // Bloc insécable : les totaux basculent ensemble sur la page suivante,
        // jamais déchirés entre deux pages (timbre + net avec le TTC).
        totalsTable.setKeepTogether(true);

        addTotalRow(totalsTable, "Sous-total HT:", formatAmount(order.getSubtotal()) + " " + order.getCurrency(), false);
        if (order.getFodecTotal() != null && order.getFodecTotal().signum() > 0) {
            addTotalRow(totalsTable, "FODEC (1%):",
                    formatAmount(order.getFodecTotal()) + " " + order.getCurrency(), false);
        }
        List<FiscalRules.VatLine> lines = FiscalRules.parseVatBreakdown(order.getVatBreakdown());
        if (lines.isEmpty()) {
            addTotalRow(totalsTable, "TVA (" + formatRate(order.getTaxRate()) + "):",
                    formatAmount(order.getTaxAmount()) + " " + order.getCurrency(), false);
        } else {
            for (FiscalRules.VatLine line : lines) {
                addTotalRow(totalsTable, "TVA " + formatRate(line.rate()) + " (base " + formatAmount(line.base()) + "):",
                        formatAmount(line.amount()) + " " + order.getCurrency(), false);
            }
        }
        addTotalRow(totalsTable, "Total TTC:", formatAmount(order.getTotal()) + " " + order.getCurrency(), false);
        addTotalRow(totalsTable, "Timbre fiscal:",
                formatAmount(order.getStampAmount()) + " " + order.getCurrency(), false);
        addTotalRow(totalsTable, "NET À PAYER:",
                formatAmount(order.getNetPayable()) + " " + order.getCurrency(), true);

        document.add(totalsTable);
    }

    private void addFiscalMention(Document document, Order order) throws DocumentException {
        if (order.getFiscalMention() == null || order.getFiscalMention().isBlank()) {
            return;
        }
        document.add(new Paragraph(" "));
        Paragraph mention = new Paragraph(order.getFiscalMention(), MENTION_FONT);
        mention.setAlignment(Element.ALIGN_JUSTIFIED);
        mention.setSpacingBefore(6f);
        document.add(mention);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean isBold) {
        Font font = isBold ? TOTAL_FONT : NORMAL_FONT;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(4f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4f);
        if (isBold) {
            valueCell.setBorderWidthTop(1f);
            valueCell.setBorderColorTop(new Color(15, 52, 96));
        }
        table.addCell(valueCell);
    }

    private void addLegalMentions(Document document, Order order) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph legal = new Paragraph("Mentions légales", BOLD_FONT);
        legal.setAlignment(Element.ALIGN_LEFT);
        document.add(legal);

        StringBuilder legalText = new StringBuilder(
                "Facture établie conformément à l'article 18 du Code de la TVA : taux et montants de TVA "
                + "ventilés par ligne, droit de timbre fiscal sur ligne distincte. "
                + "TVA calculée sur HT net de remise, FODEC inclus dans la base ; "
                + "les prix marqués TTC incluent la TVA (déduite, non ajoutée). ");
        if (order.getFiscalMention() != null && !order.getFiscalMention().isBlank()) {
            legalText.append("Opération sous régime spécial : ").append(order.getFiscalMention()).append(". ");
        }
        legalText.append("Numérotation du document : ").append(order.getReference()).append(". "
                + "En cas de question, veuillez contacter votre fournisseur. "
                + "Les montants sont exprimés en ").append(order.getCurrency()).append(" (millimes).");

        Paragraph legalContent = new Paragraph(legalText.toString(), SMALL_FONT);
        legalContent.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(legalContent);
    }

    private void addSignatures(Document document) throws DocumentException {
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        table.setSpacingBefore(10f);
        table.setKeepTogether(true);

        PdfPCell left = signatureCell("Signature du fournisseur", "Nom, qualité, cachet et date");
        PdfPCell right = signatureCell("Signature du client", "Nom, qualité, cachet et date");
        table.addCell(left);
        table.addCell(right);
        document.add(table);
    }

    private PdfPCell signatureCell(String title, String hint) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5f);
        cell.addElement(new Paragraph(title, BOLD_FONT));
        cell.addElement(new Paragraph(" ", NORMAL_FONT));
        cell.addElement(new Paragraph(" ", NORMAL_FONT));
        cell.addElement(new Paragraph(hint, SMALL_FONT));
        cell.setMinimumHeight(62f);
        return cell;
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "-";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0,000";
        return String.format(java.util.Locale.FRENCH, "%,.3f", amount).replace(" ", " ");
    }

    private String formatRate(BigDecimal rate) {
        if (rate == null) return "-";
        return rate.stripTrailingZeros().toPlainString() + " %";
    }
}
