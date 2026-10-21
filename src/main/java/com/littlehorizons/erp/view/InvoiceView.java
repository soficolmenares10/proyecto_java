package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.InvoiceController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Money;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for electronic invoices.
 */
public class InvoiceView extends BaseView {

    private final InvoiceController invoiceController;
    private final PaymentController paymentController;

    public InvoiceView(InvoiceController invoiceController, PaymentController paymentController) {
        this.invoiceController = invoiceController;
        this.paymentController = paymentController;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Facturas electrónicas");
            System.out.println("1. Generar la factura de un pago que no la tiene");
            System.out.println("2. Listar facturas");
            System.out.println("3. Ver el detalle de una factura");
            System.out.println("4. Volver a generar el PDF de una factura");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> runAndPause(this::generateInvoice);
                case 2 -> runAndPause(this::listInvoices);
                case 3 -> runAndPause(this::showInvoice);
                case 4 -> runAndPause(this::regeneratePdf);
                default -> back = true;
            }
        }
    }

    private void generateInvoice() {
        Payment payment = paymentController.getByReceipt(
                ConsoleUtil.readRequiredText("Número de recibo del pago (por ejemplo REC-2026-000001): ", 50));
        ConsoleUtil.printInfo(payment.studentName() + " - " + payment.conceptName() + " - " + Money.format(payment.amount()));
        if (!ConsoleUtil.confirm("¿Generar la factura de este pago?")) {
            ConsoleUtil.printInfo("Operación cancelada.");
            return;
        }
        Invoice invoice = invoiceController.generateForPayment(payment.id());
        ConsoleUtil.printSuccess("Factura electrónica " + invoice.number() + " generada.");
        System.out.println("  Archivo PDF: " + invoice.pdfPath());
    }

    private void listInvoices() {
        ConsoleUtil.printTitle("Facturas emitidas");
        List<Invoice> invoices = invoiceController.listAll();
        if (invoices.isEmpty()) {
            ConsoleUtil.printInfo("Todavía no se han emitido facturas.");
            return;
        }
        List<String[]> rows = invoices.stream()
                .map(i -> new String[]{i.number(), String.valueOf(i.issueDate()), i.receiptNumber(), i.studentName(),
                        Money.format(i.total()), i.pdfFileName()})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"Factura", "Fecha", "Recibo", "Estudiante", "Total", "Archivo"}, rows);
        System.out.println("Total: " + invoices.size() + " facturas");
    }

    private void showInvoice() {
        Invoice invoice = invoiceController.getByNumber(
                ConsoleUtil.readRequiredText("Número de factura (por ejemplo FE-000001): ", 50));
        ConsoleUtil.printTitle("Factura " + invoice.number());
        printLine("Fecha de emisión", String.valueOf(invoice.issueDate()));
        printLine("Recibo", invoice.receiptNumber());
        printLine("Estudiante", invoice.studentName());
        printLine("Subtotal", Money.format(invoice.subtotal()));
        printLine("IVA", Money.format(invoice.vat()));
        printLine("Total", Money.format(invoice.total()));
        printLine("CUFE (simulado)", invoice.cufe());
        printLine("Archivo PDF", invoice.pdfPath());

        List<String[]> rows = invoiceController.linesOf(invoice).stream()
                .map(l -> new String[]{l.description(), String.valueOf(l.quantity()), Money.format(l.unitPrice()),
                        Money.format(l.subtotal())})
                .collect(Collectors.toList());
        System.out.println("\nDetalle:");
        ConsoleUtil.printTable(new String[]{"Descripción", "Cant.", "Valor unitario", "Subtotal"}, rows);
    }

    private void regeneratePdf() {
        Invoice invoice = invoiceController.regeneratePdf(
                ConsoleUtil.readRequiredText("Número de factura (por ejemplo FE-000001): ", 50));
        ConsoleUtil.printSuccess("PDF generado de nuevo para la factura " + invoice.number() + ".");
        System.out.println("  Archivo PDF: " + invoice.pdfPath());
    }
}
