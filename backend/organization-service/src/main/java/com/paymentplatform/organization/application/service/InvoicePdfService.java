package com.paymentplatform.organization.application.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoicePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(15, 52, 96));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 52, 96));

    public byte[] generateInvoicePdf(Order order, List<OrderItem> items, String supplierName, String shopName) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, order, supplierName, shopName);
            addOrderInfo(document, order);
            addItemsTable(document, items, order);
            addTotals(document, order);
            addLegalMentions(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, Order order, String supplierName, String shopName) throws DocumentException {
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
        leftCell.addElement(new Paragraph("Date: " + formatDate(order.getCreatedAt()), NORMAL_FONT));
        leftCell.addElement(new Paragraph("Statut: " + order.getStatus(), NORMAL_FONT));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(new Paragraph("Fournisseur:", BOLD_FONT));
        rightCell.addElement(new Paragraph(supplierName != null ? supplierName : "-", NORMAL_FONT));
        rightCell.addElement(new Paragraph(" ", NORMAL_FONT));
        rightCell.addElement(new Paragraph("Boutique:", BOLD_FONT));
        rightCell.addElement(new Paragraph(shopName != null ? shopName : "-", NORMAL_FONT));
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addOrderInfo(Document document, Order order) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(3);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{33, 33, 34});
        infoTable.setSpacingBefore(6f);
        infoTable.setSpacingAfter(6f);

        addInfoCell(infoTable, "Devise", order.getCurrency());
        addInfoCell(infoTable, "TVA", order.getTaxRate() + "%");
        addInfoCell(infoTable, "Créé le", formatDate(order.getCreatedAt()));

        document.add(infoTable);
        document.add(new Paragraph(" "));
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6f);
        cell.addElement(new Paragraph(label + ":", SMALL_FONT));
        cell.addElement(new Paragraph(value, BOLD_FONT));
        table.addCell(cell);
    }

    private void addItemsTable(Document document, List<OrderItem> items, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{35, 10, 20, 15, 20});
        table.setSpacingBefore(10f);

        addTableHeader(table, "Produit");
        addTableHeader(table, "Qté");
        addTableHeader(table, "Prix unitaire");
        addTableHeader(table, "Remise");
        addTableHeader(table, "Total");

        for (OrderItem item : items) {
            PdfPCell nameCell = new PdfPCell(new Phrase(item.getProductName(), NORMAL_FONT));
            nameCell.setPadding(5f);
            nameCell.setBorderWidth(0.5f);
            nameCell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(nameCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), NORMAL_FONT));
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setPadding(5f);
            qtyCell.setBorderWidth(0.5f);
            qtyCell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(qtyCell);

            PdfPCell priceCell = new PdfPCell(new Phrase(formatAmount(item.getUnitPrice()) + " " + order.getCurrency(), NORMAL_FONT));
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            priceCell.setPadding(5f);
            priceCell.setBorderWidth(0.5f);
            priceCell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(priceCell);

            PdfPCell discountCell = new PdfPCell(new Phrase(
                    item.getDiscount() != null && item.getDiscount().signum() > 0 ? item.getDiscount() + "%" : "-",
                    NORMAL_FONT));
            discountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            discountCell.setPadding(5f);
            discountCell.setBorderWidth(0.5f);
            discountCell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(discountCell);

            PdfPCell totalCell = new PdfPCell(new Phrase(formatAmount(item.getLineTotal()) + " " + order.getCurrency(), BOLD_FONT));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setPadding(5f);
            totalCell.setBorderWidth(0.5f);
            totalCell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(totalCell);
        }

        document.add(table);
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
        totalsTable.setWidthPercentage(40);
        totalsTable.setWidths(new float[]{60, 40});
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTotalRow(totalsTable, "Sous-total:", formatAmount(order.getSubtotal()) + " " + order.getCurrency(), false);
        addTotalRow(totalsTable, "TVA (" + order.getTaxRate() + "%):", formatAmount(order.getTaxAmount()) + " " + order.getCurrency(), false);
        addTotalRow(totalsTable, "TOTAL:", formatAmount(order.getTotal()) + " " + order.getCurrency(), true);

        document.add(totalsTable);
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

    private void addLegalMentions(Document document) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph legal = new Paragraph("Mentions légales", BOLD_FONT);
        legal.setAlignment(Element.ALIGN_LEFT);
        document.add(legal);

        String legalText = "Facture générée automatiquement par la plateforme Payment Platform. " +
                "Ce document fait foi de la transaction entre les parties. " +
                "En cas de question, veuillez contacter votre fournisseur ou la boutique concernée. " +
                "Les montants indiqués sont exprimés dans la devise spécifiée sur cette facture.";

        Paragraph legalContent = new Paragraph(legalText, SMALL_FONT);
        legalContent.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(legalContent);

        Paragraph footer = new Paragraph("Payment Platform - Plateforme de gestion des commandes et paiements", SMALL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(20f);
        document.add(footer);
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "-";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    private String formatAmount(java.math.BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%.2f", amount);
    }
}
