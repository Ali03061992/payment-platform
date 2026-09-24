package com.paymentplatform.payment.application.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static com.paymentplatform.payment.domain.model.PaymentStatus.*;

@Service
public class PaymentInvoicePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(15, 52, 96));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 14, Font.BOLD, new Color(15, 52, 96));

    public byte[] generateInvoicePdf(PaymentResponse payment) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, payment);
            addPaymentDetails(document, payment);
            addAmountSection(document, payment);
            addLegalMentions(document);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, PaymentResponse payment) throws DocumentException {
        Paragraph title = new Paragraph("FACTURE DE PAIEMENT", TITLE_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        document.add(title);

        document.add(new Paragraph(" "));

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("Facture n°: " + payment.reference(), BOLD_FONT));
        leftCell.addElement(new Paragraph("Date: " + formatDate(payment.createdAt()), NORMAL_FONT));
        leftCell.addElement(new Paragraph("Statut: " + translateStatus(payment.status()), NORMAL_FONT));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(new Paragraph("Fournisseur:", BOLD_FONT));
        rightCell.addElement(new Paragraph(payment.supplierName() != null ? payment.supplierName() : "-", NORMAL_FONT));
        rightCell.addElement(new Paragraph(" ", NORMAL_FONT));
        rightCell.addElement(new Paragraph("Boutique:", BOLD_FONT));
        rightCell.addElement(new Paragraph(payment.shopName() != null ? payment.shopName() : "-", NORMAL_FONT));
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);
        document.add(new Paragraph(" "));
    }

    private void addPaymentDetails(Document document, PaymentResponse payment) throws DocumentException {
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{50, 50});
        detailsTable.setSpacingBefore(6f);

        addDetailCell(detailsTable, "Référence", payment.reference());
        addDetailCell(detailsTable, "Devise", payment.currency());
        addDetailCell(detailsTable, "Créé par", payment.createdByName() != null ? payment.createdByName() : "-");
        if (payment.status() == CONFIRMED && payment.confirmedByName() != null) {
            addDetailCell(detailsTable, "Confirmé par", payment.confirmedByName());
        }
        if (payment.status() == REJECTED) {
            if (payment.rejectedByName() != null) {
                addDetailCell(detailsTable, "Rejeté par", payment.rejectedByName());
            }
            if (payment.rejectionReason() != null) {
                addDetailCell(detailsTable, "Motif", payment.rejectionReason());
            }
        }
        if (payment.status() == CANCELLED && payment.cancelledByName() != null) {
            addDetailCell(detailsTable, "Annulé par", payment.cancelledByName());
        }

        document.add(detailsTable);
        document.add(new Paragraph(" "));
    }

    private void addDetailCell(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label + ":", SMALL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(4f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, BOLD_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(4f);
        table.addCell(valueCell);
    }

    private void addAmountSection(Document document, PaymentResponse payment) throws DocumentException {
        document.add(new Paragraph(" "));

        PdfPTable amountTable = new PdfPTable(2);
        amountTable.setWidthPercentage(50);
        amountTable.setWidths(new float[]{50, 50});
        amountTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPCell labelCell = new PdfPCell(new Phrase("MONTANT TOTAL:", TOTAL_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(8f);
        amountTable.addCell(labelCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(
                String.format("%.2f %s", payment.amount(), payment.currency()), TOTAL_FONT));
        amountCell.setBorder(Rectangle.NO_BORDER);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        amountCell.setPadding(8f);
        amountCell.setBorderWidthTop(2f);
        amountCell.setBorderColorTop(new Color(15, 52, 96));
        amountTable.addCell(amountCell);

        document.add(amountTable);
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

    private String translateStatus(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "En attente";
            case CONFIRMED -> "Confirmé";
            case REJECTED -> "Rejeté";
            case CANCELLED -> "Annulé";
            default -> status.toString();
        };
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "-";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
}
