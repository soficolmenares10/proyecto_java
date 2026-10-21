package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.controller.ChargeController;
import com.littlehorizons.erp.controller.ConceptController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.InvoiceController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentRequest;
import com.littlehorizons.erp.model.PendingCharge;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Console screens for registering payments and consulting the payment history.
 * When a payment is confirmed, the electronic invoice (PDF) is generated automatically.
 */
public class PaymentView extends BaseView {

    private final PaymentController paymentController;
    private final StudentController studentController;
    private final GuardianController guardianController;
    private final ChargeController chargeController;
    private final ConceptController conceptController;
    private final InvoiceController invoiceController;
    private final ConceptView conceptView;

    public PaymentView(PaymentController paymentController, StudentController studentController,
                       GuardianController guardianController, ChargeController chargeController,
                       ConceptController conceptController, InvoiceController invoiceController,
                       ConceptView conceptView) {
        this.paymentController = paymentController;
        this.studentController = studentController;
        this.guardianController = guardianController;
        this.chargeController = chargeController;
        this.conceptController = conceptController;
        this.invoiceController = invoiceController;
        this.conceptView = conceptView;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            ConsoleUtil.printTitle("Pagos");
            System.out.println("1. Registrar un pago (genera la factura electrónica)");
            System.out.println("2. Historial de pagos de un estudiante");
            System.out.println("3. Historial de pagos de un acudiente (todos sus hijos)");
            System.out.println("4. Consultar un pago por número de recibo");
            System.out.println("0. Volver");
            int option = ConsoleUtil.readInt("Seleccione una opción: ", 0, 4);
            switch (option) {
                case 1 -> runAndPause(this::registerPayment);
                case 2 -> runAndPause(this::showStudentHistory);
                case 3 -> runAndPause(this::showGuardianHistory);
                case 4 -> runAndPause(this::showPayment);
                default -> back = true;
            }
        }
    }

    // ------------------------------------------------------------------ actions

    private void registerPayment() {
        ConsoleUtil.printTitle("Registrar pago");
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());

        Integer chargeId = null;
        PaymentConcept concept;
        BigDecimal amount;
        int year;
        String period;
        String notes = null;

        PendingCharge charge = chooseCharge(student);
        LocalDate today = LocalDate.now();
        if (charge != null) {
            chargeId = charge.id();
            concept = conceptController.getById(charge.conceptId());
            amount = charge.expectedAmount();
            year = charge.academicYear();
            period = PaymentController.suggestPeriod(concept, charge.dueDate());
            notes = charge.description();
        } else {
            concept = conceptView.chooseConcept(true, false);
            year = readAcademicYear();
            BigDecimal suggested = conceptController.effectiveAmount(student.getId(), concept.getId(), year);
            ConsoleUtil.printInfo("Valor del concepto para este estudiante: " + Money.format(suggested));
            amount = ConsoleUtil.confirm("¿Usar ese valor?")
                    ? suggested
                    : ConsoleUtil.readPositiveAmount("Valor pagado en pesos: ");
            period = PaymentController.suggestPeriod(concept, today);
            notes = ConsoleUtil.readOptionalText("Observaciones", 1000);
        }

        Guardian guardian = chooseGuardian(student);
        String method = ConsoleUtil.readChoice("Método de pago", Payment.METHODS);
        LocalDate date = ConsoleUtil.confirm("¿El pago es de hoy (" + today + ")?")
                ? today
                : ConsoleUtil.readDate("Fecha del pago");

        System.out.println("\nResumen del pago:");
        printLine("Estudiante", student.getFullName());
        printLine("Concepto", concept.getName() + (charge == null ? "" : " - " + charge.description()));
        printLine("Valor", Money.format(amount));
        printLine("Paga", guardian.getFullName());
        printLine("Método", method);
        printLine("Fecha", String.valueOf(date));
        if (!ConsoleUtil.confirm("¿Confirma el pago?")) {
            ConsoleUtil.printInfo("Pago cancelado.");
            return;
        }

        Payment payment = paymentController.register(new PaymentRequest(student.getId(), concept.getId(), chargeId,
                guardian.getId(), amount, method, year, period, date, notes));
        ConsoleUtil.printSuccess("Pago registrado. Recibo " + payment.receiptNumber() + " por " + Money.format(amount) + ".");

        Invoice invoice = callSafely(() -> invoiceController.generateForPayment(payment.id()));
        if (invoice != null) {
            ConsoleUtil.printSuccess("Factura electrónica " + invoice.number() + " generada.");
            System.out.println("  Archivo PDF: " + invoice.pdfPath());
        } else {
            ConsoleUtil.printInfo("El pago quedó registrado. Genere la factura desde Gestión Financiera > Facturas.");
        }
    }

    private void showStudentHistory() {
        Student student = studentController.getByDocument(ConsoleUtil.readDocument("Documento del estudiante: "));
        List<Payment> payments = paymentController.historyOfStudent(student.getId());
        ConsoleUtil.printInfo("Estudiante: " + student.getFullName());
        printPayments(payments, false);
    }

    private void showGuardianHistory() {
        Guardian guardian = guardianController.getByDocument(ConsoleUtil.readDocument("Documento del acudiente: "));
        List<Payment> payments = paymentController.historyOfGuardian(guardian.getId());
        ConsoleUtil.printInfo("Acudiente: " + guardian.getFullName());
        printPayments(payments, true);
    }

    private void showPayment() {
        Payment payment = paymentController.getByReceipt(
                ConsoleUtil.readRequiredText("Número de recibo (por ejemplo REC-2026-000001): ", 50));
        ConsoleUtil.printTitle("Recibo " + payment.receiptNumber());
        printLine("Estudiante", payment.studentName());
        printLine("Acudiente que pagó", payment.guardianName());
        printLine("Concepto", payment.conceptName());
        printLine("Valor", Money.format(payment.amount()));
        printLine("Fecha", String.valueOf(payment.date()));
        printLine("Método", payment.method());
        printLine("Año y período", payment.academicYear() + " - " + show(payment.period()));
        printLine("Notas", payment.notes());
    }

    // ------------------------------------------------------------------ helpers

    /** Shows the pending charges of the student and lets the user pick one (or none). */
    private PendingCharge chooseCharge(Student student) {
        List<PendingCharge> pending = chargeController.pendingOf(student.getId());
        if (pending.isEmpty()) {
            return null;
        }
        System.out.println("\nCobros pendientes del estudiante:");
        List<String[]> rows = pending.stream()
                .map(c -> new String[]{String.valueOf(c.id()), c.description(), String.valueOf(c.dueDate()),
                        Money.format(c.expectedAmount())})
                .collect(Collectors.toList());
        ConsoleUtil.printTable(new String[]{"ID", "Cobro", "Vence", "Valor"}, rows);
        int id = ConsoleUtil.readInt("ID del cobro que va a pagar (0 para pagar otro concepto): ", 0, Integer.MAX_VALUE);
        if (id == 0) {
            return null;
        }
        return pending.stream()
                .filter(charge -> charge.id() == id)
                .findFirst()
                .orElseThrow(() -> new BusinessException("Ese ID no está en la lista de cobros pendientes."));
    }

    private Guardian chooseGuardian(Student student) {
        List<Guardian> guardians = guardianController.getGuardiansOfStudent(student.getId());
        if (guardians.isEmpty()) {
            throw new BusinessException("El estudiante no tiene acudientes. Asocie uno primero.");
        }
        List<String> labels = guardians.stream()
                .map(g -> g.getFullName() + " (" + g.getRelationship() + ")"
                        + (g.isFinancialResponsible() ? " - responsable de pagos" : ""))
                .collect(Collectors.toList());
        String chosen = ConsoleUtil.readChoice("Acudiente que realiza el pago", labels);
        return guardians.get(labels.indexOf(chosen));
    }

    private void printPayments(List<Payment> payments, boolean withStudent) {
        if (payments.isEmpty()) {
            ConsoleUtil.printInfo("No hay pagos registrados.");
            return;
        }
        List<String[]> rows = payments.stream()
                .map(p -> withStudent
                        ? new String[]{p.receiptNumber(), String.valueOf(p.date()), p.studentName(), p.conceptName(),
                                Money.format(p.amount()), p.method()}
                        : new String[]{p.receiptNumber(), String.valueOf(p.date()), p.conceptName(),
                                Money.format(p.amount()), p.method(), show(p.period())})
                .collect(Collectors.toList());
        String[] headers = withStudent
                ? new String[]{"Recibo", "Fecha", "Estudiante", "Concepto", "Valor", "Método"}
                : new String[]{"Recibo", "Fecha", "Concepto", "Valor", "Método", "Período"};
        ConsoleUtil.printTable(headers, rows);
        BigDecimal total = payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        System.out.println("Total de " + payments.size() + " pagos: " + Money.format(total));
    }
}
