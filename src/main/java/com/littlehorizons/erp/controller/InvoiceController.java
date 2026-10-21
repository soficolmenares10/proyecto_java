package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceData;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.service.InvoiceDocumentGenerator;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.InvoiceSettings;
import com.littlehorizons.erp.util.Validator;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for electronic invoices: it collects the data of a payment, calculates the IVA
 * and the (simulated) CUFE, creates the PDF and saves the invoice.
 */
public class InvoiceController {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;
    private final GuardianDao guardianDao;
    private final StudentDao studentDao;
    private final ConceptDao conceptDao;
    private final InvoiceDocumentGenerator documentGenerator;
    private final InvoiceSettings settings;

    public InvoiceController(InvoiceDao invoiceDao, PaymentDao paymentDao, GuardianDao guardianDao,
                             StudentDao studentDao, ConceptDao conceptDao,
                             InvoiceDocumentGenerator documentGenerator, InvoiceSettings settings) {
        this.invoiceDao = invoiceDao;
        this.paymentDao = paymentDao;
        this.guardianDao = guardianDao;
        this.studentDao = studentDao;
        this.conceptDao = conceptDao;
        this.documentGenerator = documentGenerator;
        this.settings = settings;
    }

    /**
     * Generates the invoice of a payment: PDF file included. A payment can have only one invoice.
     * The amount paid already includes IVA, so the IVA is separated from the total.
     */
    public Invoice generateForPayment(int paymentId) {
        Payment payment = paymentDao.findById(paymentId)
                .orElseThrow(() -> new BusinessException("El pago no existe."));
        if (invoiceDao.findByPaymentId(paymentId).isPresent()) {
            throw new BusinessException("El pago " + payment.receiptNumber() + " ya tiene factura.");
        }
        if (payment.guardianId() == null) {
            throw new BusinessException("El pago no tiene acudiente responsable, no se puede facturar.");
        }
        PaymentConcept concept = conceptDao.findById(payment.conceptId())
                .orElseThrow(() -> new BusinessException("El concepto del pago no existe."));

        long sequence = invoiceDao.nextSequence();
        if (sequence < settings.rangeFrom() || sequence > settings.rangeTo()) {
            throw new BusinessException("Se agotó el rango de numeración de la resolución de facturación.");
        }
        String number = settings.invoicePrefix() + "-" + String.format("%06d", sequence);
        String fileName = "factura_" + String.format("%06d", sequence) + ".pdf";
        Path file = settings.outputFolder().resolve(fileName).toAbsolutePath();

        LocalDate issueDate = LocalDate.now();
        BigDecimal total = payment.amount();
        BigDecimal subtotal = total;
        if (concept.getVatPercent().signum() > 0) {
            BigDecimal factor = BigDecimal.ONE.add(concept.getVatPercent().divide(HUNDRED));
            subtotal = total.divide(factor, 2, RoundingMode.HALF_UP);
        }
        BigDecimal vat = total.subtract(subtotal);
        List<InvoiceLine> lines = List.of(new InvoiceLine(describe(payment), 1, subtotal, subtotal));
        String cufe = CufeGenerator.generate(number, settings.schoolNit(), issueDate, total);

        InvoiceData data = buildData(payment, number, issueDate, cufe, lines, subtotal, vat, total);
        documentGenerator.generate(data, file);

        Invoice invoice = new Invoice(0, number, paymentId, payment.receiptNumber(), payment.studentName(),
                issueDate, subtotal, vat, total, cufe, fileName, file.toString());
        try {
            int id = invoiceDao.insert(invoice, lines);
            return invoiceDao.findById(id).orElse(invoice);
        } catch (DataAccessException e) {
            deleteQuietly(file);
            throw e;
        }
    }

    /** Creates the PDF of an existing invoice again, for example if the file was lost or deleted. */
    public Invoice regeneratePdf(String invoiceNumber) {
        Invoice invoice = getByNumber(invoiceNumber);
        Payment payment = paymentDao.findById(invoice.paymentId())
                .orElseThrow(() -> new BusinessException("El pago de la factura no existe."));
        List<InvoiceLine> lines = invoiceDao.findLines(invoice.id());
        InvoiceData data = buildData(payment, invoice.number(), invoice.issueDate(), invoice.cufe(), lines,
                invoice.subtotal(), invoice.vat(), invoice.total());
        Path file = Validator.isNotBlank(invoice.pdfPath())
                ? Paths.get(invoice.pdfPath())
                : settings.outputFolder().resolve(invoice.pdfFileName()).toAbsolutePath();
        documentGenerator.generate(data, file);
        return invoice;
    }

    public Invoice getByNumber(String number) {
        return invoiceDao.findByNumber(number.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("No existe la factura " + number + "."));
    }

    public List<InvoiceLine> linesOf(Invoice invoice) {
        return invoiceDao.findLines(invoice.id());
    }

    public List<Invoice> listAll() {
        return invoiceDao.findAll();
    }

    // ------------------------------------------------------------------ helpers

    private String describe(Payment payment) {
        String detail = Validator.isNotBlank(payment.notes())
                ? payment.notes()
                : (payment.academicYear() == null ? "" : "año " + payment.academicYear());
        String text = payment.conceptName() + (detail.isEmpty() ? "" : " - " + detail);
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private InvoiceData buildData(Payment payment, String number, LocalDate issueDate, String cufe,
                                  List<InvoiceLine> lines, BigDecimal subtotal, BigDecimal vat, BigDecimal total) {
        Guardian guardian = guardianDao.findById(payment.guardianId())
                .orElseThrow(() -> new BusinessException("El acudiente del pago no existe."));
        Student student = studentDao.findById(payment.studentId())
                .orElseThrow(() -> new BusinessException("El estudiante del pago no existe."));
        String address = Validator.isNotBlank(guardian.getAddress()) ? guardian.getAddress() : student.getAddress();
        return new InvoiceData(settings.schoolName(), settings.schoolNit(), settings.schoolAddress(),
                settings.schoolPhone(), settings.schoolEmail(), settings.resolutionText(), number, issueDate, cufe,
                guardian.getFullName(), guardian.getDocumentType() + " " + guardian.getDocumentNumber(),
                Validator.isNotBlank(address) ? address : "No registrada",
                Validator.isNotBlank(guardian.getEmail()) ? guardian.getEmail() : "No registrado",
                student.getFullName(), payment.method(), payment.receiptNumber(), lines, subtotal, vat, total);
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            AppLogger.error("Could not delete the file " + file, e);
        }
    }
}
