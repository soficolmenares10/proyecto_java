package com.littlehorizons.erp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.Money;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Draws the electronic invoice in a PDF file using OpenPDF, and the (simulated) QR code using ZXing.
 */
public class PdfInvoiceGenerator implements InvoiceDocumentGenerator {

    private static final Color DARK_BLUE = new Color(0x1F, 0x3A, 0x5F);
    private static final Color LIGHT_GRAY = new Color(0xEE, 0xEE, 0xEE);

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Font.NORMAL, DARK_BLUE);
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 7);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.NORMAL, Color.WHITE);

    @Override
    public void generate(InvoiceData data, Path destination) {
        try {
            Files.createDirectories(destination.toAbsolutePath().getParent());
            try (OutputStream output = Files.newOutputStream(destination)) {
                Document document = new Document(PageSize.A4, 40, 40, 40, 40);
                PdfWriter.getInstance(document, output);
                document.open();
                addHeader(document, data);
                addParties(document, data);
                addLines(document, data);
                addTotals(document, data);
                addFooter(document, data);
                document.close();
            }
        } catch (Exception e) {
            // Catches the file errors (IOException), the PDF errors (DocumentException) and the QR errors
            // (WriterException). In OpenPDF the PDF errors are unchecked, so one catch covers them all.
            AppLogger.error("Could not create the PDF of invoice " + data.invoiceNumber(), e);
            throw new InvoiceGenerationException("No se pudo crear el PDF de la factura " + data.invoiceNumber(), e);
        }
    }

    // ------------------------------------------------------------------ sections

    private void addHeader(Document document, InvoiceData data) throws DocumentException {
        Paragraph school = new Paragraph(data.schoolName(), TITLE_FONT);
        school.setAlignment(Element.ALIGN_CENTER);
        document.add(school);

        Paragraph details = new Paragraph("NIT " + data.schoolNit() + "  |  " + data.schoolAddress()
                + "  |  Tel. " + data.schoolPhone() + "  |  " + data.schoolEmail(), NORMAL_FONT);
        details.setAlignment(Element.ALIGN_CENTER);
        document.add(details);

        Paragraph title = new Paragraph("FACTURA ELECTRÓNICA DE VENTA No. " + data.invoiceNumber(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(12);
        document.add(title);

        Paragraph resolution = new Paragraph(data.resolutionText(), SMALL_FONT);
        resolution.setAlignment(Element.ALIGN_CENTER);
        resolution.setSpacingAfter(12);
        document.add(resolution);
    }

    private void addParties(Document document, InvoiceData data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{55, 45});
        table.setSpacingAfter(12);

        Phrase client = new Phrase();
        client.add(new com.lowagie.text.Chunk("CLIENTE (Acudiente responsable)\n", BOLD_FONT));
        client.add(new com.lowagie.text.Chunk(data.clientName() + "\n", NORMAL_FONT));
        client.add(new com.lowagie.text.Chunk("Documento: " + data.clientDocument() + "\n", NORMAL_FONT));
        client.add(new com.lowagie.text.Chunk("Dirección: " + data.clientAddress() + "\n", NORMAL_FONT));
        client.add(new com.lowagie.text.Chunk("Correo: " + data.clientEmail(), NORMAL_FONT));
        table.addCell(boxedCell(client));

        Phrase info = new Phrase();
        info.add(new com.lowagie.text.Chunk("DATOS DE LA FACTURA\n", BOLD_FONT));
        info.add(new com.lowagie.text.Chunk("Fecha de emisión: " + data.issueDate() + "\n", NORMAL_FONT));
        info.add(new com.lowagie.text.Chunk("Medio de pago: " + data.paymentMethod() + "\n", NORMAL_FONT));
        info.add(new com.lowagie.text.Chunk("Recibo de caja: " + data.receiptNumber() + "\n", NORMAL_FONT));
        info.add(new com.lowagie.text.Chunk("Estudiante: " + data.studentName(), NORMAL_FONT));
        table.addCell(boxedCell(info));

        document.add(table);
    }

    private void addLines(Document document, InvoiceData data) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 10, 20, 20});

        for (String header : new String[]{"Descripción", "Cant.", "Valor unitario", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setBackgroundColor(DARK_BLUE);
            cell.setPadding(5);
            table.addCell(cell);
        }
        for (InvoiceLine line : data.lines()) {
            table.addCell(textCell(line.description(), Element.ALIGN_LEFT));
            table.addCell(textCell(String.valueOf(line.quantity()), Element.ALIGN_CENTER));
            table.addCell(textCell(Money.format(line.unitPrice()), Element.ALIGN_RIGHT));
            table.addCell(textCell(Money.format(line.subtotal()), Element.ALIGN_RIGHT));
        }
        document.add(table);
    }

    private void addTotals(Document document, InvoiceData data) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(8);
        table.setSpacingAfter(16);

        addTotalRow(table, "Subtotal", Money.format(data.subtotal()), false);
        addTotalRow(table, "IVA", Money.format(data.vat()), false);
        addTotalRow(table, "TOTAL", Money.format(data.total()), true);
        document.add(table);
    }

    private void addFooter(Document document, InvoiceData data)
            throws DocumentException, WriterException, IOException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{75, 25});

        Phrase cufe = new Phrase();
        cufe.add(new com.lowagie.text.Chunk("CUFE (simulado):\n", BOLD_FONT));
        cufe.add(new com.lowagie.text.Chunk(data.cufe() + "\n\n", SMALL_FONT));
        cufe.add(new com.lowagie.text.Chunk("Representación gráfica de una factura electrónica simulada con fines "
                + "académicos. No tiene validez fiscal ante la DIAN.", SMALL_FONT));
        PdfPCell left = new PdfPCell(cufe);
        left.setBorder(Rectangle.NO_BORDER);
        table.addCell(left);

        Image qr = Image.getInstance(createQrImage(buildQrContent(data)), null);
        qr.scaleToFit(110, 110);
        PdfPCell right = new PdfPCell(qr);
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(right);

        document.add(table);
    }

    // ------------------------------------------------------------------ helpers

    private PdfPCell boxedCell(Phrase content) {
        PdfPCell cell = new PdfPCell(content);
        cell.setPadding(7);
        cell.setBorderColor(Color.GRAY);
        return cell;
    }

    private PdfPCell textCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, NORMAL_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        return cell;
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean highlighted) {
        Font font = highlighted ? BOLD_FONT : NORMAL_FONT;
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell cell : new PdfPCell[]{labelCell, valueCell}) {
            cell.setPadding(5);
            cell.setBorderColor(Color.LIGHT_GRAY);
            if (highlighted) {
                cell.setBackgroundColor(LIGHT_GRAY);
            }
            table.addCell(cell);
        }
    }

    /** The text "contained" in the QR code: the main data of the invoice (simulated DIAN format). */
    private String buildQrContent(InvoiceData data) {
        return "NumFac: " + data.invoiceNumber() + "\n"
                + "FecFac: " + data.issueDate() + "\n"
                + "NitFac: " + data.schoolNit() + "\n"
                + "DocAdq: " + data.clientDocument() + "\n"
                + "ValFac: " + data.subtotal().toPlainString() + "\n"
                + "ValIva: " + data.vat().toPlainString() + "\n"
                + "ValTotal: " + data.total().toPlainString() + "\n"
                + "CUFE: " + data.cufe() + "\n"
                + "(QR simulado)";
    }

    private BufferedImage createQrImage(String content) throws WriterException {
        int size = 220;
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size,
                Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 1));
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        return image;
    }
}