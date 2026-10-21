package com.paymentplatform.organization.application.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.paymentplatform.organization.application.dto.WithholdingCertificateResponse;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * Certificat de retenue à la source (PDF), rattaché à la FACTURE DE COMMANDE :
 * mentions art. 55 IRPP/IS + arrêté du 10 mai 2024, QR code et référence
 * (interne en phase 1, TEJ en phase 2).
 */
@Service
public class WithholdingCertificatePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, new Color(15, 52, 96));
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, Color.WHITE);
    private static final Font NORMAL_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font BOLD_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SMALL_FONT = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
    private static final Font TOTAL_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, new Color(15, 52, 96));

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generateCertificatePdf(WithholdingCertificateResponse cert) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, cert);
            addParties(document, cert);
            addOperation(document, cert);
            addAmounts(document, cert);
            addQrSection(document, cert);
            addLegalMentions(document, cert);

            document.close();
        } catch (DocumentException e) {
            throw new RuntimeException("Erreur lors de la génération du certificat de retenue", e);
        }
        return out.toByteArray();
    }

    /** Contenu du QR : type document + numéro de certificat, lu par le scanner. */
    public static String qrPayload(WithholdingCertificateResponse cert) {
        return DocumentQrPayload.of(DocumentQrPayload.TYPE_WITHHOLDING_CERTIFICATE, cert.number());
    }

    private void addQrSection(Document document, WithholdingCertificateResponse cert) throws DocumentException {
        document.add(new Paragraph(" "));
        try {
            BufferedImage qr = renderQr(qrPayload(cert), 140);
            Image img = Image.getInstance(qr, null);
            img.setAlignment(Element.ALIGN_CENTER);
            document.add(img);
        } catch (Exception e) {
            document.add(new Paragraph("Référence de vérification: " + cert.number(), SMALL_FONT));
        }
        Paragraph ref = new Paragraph("Vérification: " + cert.number(), SMALL_FONT);
        ref.setAlignment(Element.ALIGN_CENTER);
        document.add(ref);
    }

    private void addHeader(Document document, WithholdingCertificateResponse cert) throws DocumentException {
        Paragraph title = new Paragraph("CERTIFICAT DE RETENUE À LA SOURCE", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("Certificat n°: " + cert.number(), BOLD_FONT));
        left.addElement(new Paragraph("Facture n°: " + cert.orderReference(), NORMAL_FONT));
        left.addElement(new Paragraph("Date de facturation: " + cert.orderDate().format(DATE), NORMAL_FONT));
        if (cert.billingPeriod() != null) {
            left.addElement(new Paragraph("Exercice de facturation: " + cert.billingPeriod(), NORMAL_FONT));
        }

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.addElement(new Paragraph("Échéance légale: " + cert.dueDate().format(DATE), NORMAL_FONT));
        if (cert.tejReference() != null && !cert.tejReference().isBlank()) {
            right.addElement(new Paragraph("Référence TEJ: " + cert.tejReference(), BOLD_FONT));
        } else {
            right.addElement(new Paragraph("Référence TEJ: en attente de dépôt", SMALL_FONT));
        }
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);

        table.addCell(left);
        table.addCell(right);
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addParties(Document document, WithholdingCertificateResponse cert) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        addSectionHeader(table, "Débiteur (acheteur)");
        addSectionHeader(table, "Bénéficiaire (fournisseur)");
        addPartyCell(table, cert.debtorName(), cert.debtorMatricule(), cert.debtorAddress());
        addPartyCell(table, cert.beneficiaryName(), cert.beneficiaryMatricule(), cert.beneficiaryAddress());
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addSectionHeader(PdfPTable table, String text) {
        PdfPCell header = new PdfPCell(new Phrase(text, HEADER_FONT));
        header.setBackgroundColor(new Color(15, 52, 96));
        header.setPadding(6f);
        header.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(header);
    }

    private void addPartyCell(PdfPTable table, String name, String mf, String address) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(6f);
        cell.addElement(new Paragraph(name != null ? name : "-", BOLD_FONT));
        cell.addElement(new Paragraph("MF: " + (mf != null ? mf : "—"), NORMAL_FONT));
        if (address != null && !address.isBlank()) {
            cell.addElement(new Paragraph(address, NORMAL_FONT));
        }
        table.addCell(cell);
    }

    private void addOperation(Document document, WithholdingCertificateResponse cert) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{40, 60});
        addRow(table, "Nature du montant facturé:", cert.nature());
        addRow(table, "Référence de la facture:", cert.orderReference());
        addRow(table, "Taux de retenue:", cert.rate().stripTrailingZeros().toPlainString() + " %");
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addRow(PdfPTable table, String label, String value) {
        PdfPCell l = new PdfPCell(new Phrase(label, SMALL_FONT));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(4f);
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, BOLD_FONT));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(4f);
        table.addCell(v);
    }

    private void addAmounts(Document document, WithholdingCertificateResponse cert) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(55);
        table.setWidths(new float[]{60, 40});
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setKeepTogether(true);
        addTotalRow(table, "Montant brut facturé (TTC):",
                fmt(cert.withholdingAmount().add(cert.netAmount())) + " " + cert.currency(), false);
        addTotalRow(table, "Retenue à la source (" + cert.rate().stripTrailingZeros().toPlainString() + " %):",
                fmt(cert.withholdingAmount()) + " " + cert.currency(), false);
        addTotalRow(table, "Montant net à payer:", fmt(cert.netAmount()) + " " + cert.currency(), true);
        document.add(table);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean bold) {
        Font font = bold ? TOTAL_FONT : NORMAL_FONT;
        PdfPCell l = new PdfPCell(new Phrase(label, font));
        l.setBorder(Rectangle.NO_BORDER);
        l.setHorizontalAlignment(Element.ALIGN_RIGHT);
        l.setPadding(4f);
        table.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, font));
        v.setBorder(Rectangle.NO_BORDER);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(4f);
        table.addCell(v);
    }

    static BufferedImage renderQr(String payload, int size) throws WriterException {
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }

    private void addLegalMentions(Document document, WithholdingCertificateResponse cert) throws DocumentException {
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Mentions légales", BOLD_FONT));
        String text = "Certificat délivré en application de l'article 55 du Code de l'IRPP et de l'IS "
                + "(plateforme TEJ — arrêté du 10 mai 2024) au titre de la facture "
                + cert.orderReference() + ". Le débiteur reverse la retenue au Trésor ; "
                + "le bénéficiaire l'impute sur son impôt sous réserve de son inscription sur TEJ. "
                + "Échéance légale d'établissement : " + cert.dueDate().format(DATE) + ". "
                + "Attestation hors TEJ : amende de 30% du montant retenu (min. 50 DT).";
        Paragraph p = new Paragraph(text, SMALL_FONT);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        document.add(p);
    }

    private String fmt(java.math.BigDecimal amount) {
        if (amount == null) return "0,000";
        return String.format(java.util.Locale.FRENCH, "%,.3f", amount).replace(" ", " ");
    }
}
