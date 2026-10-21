package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentRequest;
import com.littlehorizons.erp.model.PendingCharge;
import com.littlehorizons.erp.util.Validator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for registering payments and consulting the payment history.
 */
public class PaymentController {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private final PaymentDao paymentDao;
    private final ChargeDao chargeDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final ConceptController conceptController;

    public PaymentController(PaymentDao paymentDao, ChargeDao chargeDao, StudentDao studentDao,
                             GuardianDao guardianDao, ConceptController conceptController) {
        this.paymentDao = paymentDao;
        this.chargeDao = chargeDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.conceptController = conceptController;
    }

    /**
     * Registers a payment after checking all the rules, and returns it already saved
     * (with its receipt number). If the request has a chargeId, that charge is marked as paid.
     */
    public Payment register(PaymentRequest request) {
        if (studentDao.findById(request.studentId()).isEmpty()) {
            throw new BusinessException("El estudiante no existe.");
        }
        PaymentConcept concept = conceptController.getById(request.conceptId());
        if (!concept.isActive()) {
            throw new BusinessException("El concepto " + concept.getName() + " está inactivo.");
        }
        boolean guardianBelongsToStudent = guardianDao.findByStudentId(request.studentId()).stream()
                .anyMatch(guardian -> guardian.getId() == request.guardianId());
        if (!guardianBelongsToStudent) {
            throw new BusinessException("El acudiente que paga debe estar asociado al estudiante.");
        }
        BigDecimal amount = request.amount();
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0 || amount.scale() > 2) {
            throw new BusinessException("El monto debe ser mayor que cero, con máximo 2 decimales.");
        }
        Rules.requireOneOf(request.method(), Payment.METHODS, "El método de pago");
        Rules.requireOneOf(request.period(), Payment.PERIODS, "El período");
        Rules.requireAcademicYear(request.academicYear());
        Rules.limitText(request.notes(), "Las notas", 1000);
        LocalDate date = request.date();
        if (date == null || date.isAfter(LocalDate.now()) || date.getYear() < 2000) {
            throw new BusinessException("La fecha de pago no puede ser posterior a hoy.");
        }
        if (request.chargeId() != null) {
            checkCharge(request, amount);
        }

        String receipt = paymentDao.nextReceiptNumber(date.getYear());
        Payment payment = new Payment(0, request.studentId(), null, null, request.guardianId(), null,
                request.conceptId(), null, amount, date, request.method(), receipt, request.academicYear(),
                request.period(), Validator.isNotBlank(request.notes()) ? request.notes().trim() : null);
        int id = paymentDao.insert(payment, request.chargeId());
        return paymentDao.findById(id).orElseThrow(() -> new BusinessException("No se pudo leer el pago guardado."));
    }

    public List<Payment> historyOfStudent(int studentId) {
        return paymentDao.findByStudent(studentId);
    }

    /** All the payments of the students linked to a guardian (for example, all his or her children). */
    public List<Payment> historyOfGuardian(int guardianId) {
        return paymentDao.findByGuardian(guardianId);
    }

    public List<Payment> paymentsBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
        return paymentDao.findBetween(from, to);
    }

    public Payment getByReceipt(String receiptNumber) {
        return paymentDao.findByReceipt(receiptNumber.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException("No existe un pago con el recibo " + receiptNumber + "."));
    }

    /** Suggests the period of a payment: "Anual" for one-time concepts, or the period of the date. */
    public static String suggestPeriod(PaymentConcept concept, LocalDate date) {
        if (!concept.isRecurring()) {
            return "Anual";
        }
        int month = date.getMonthValue();
        if (month <= 3) {
            return "I";
        }
        if (month <= 6) {
            return "II";
        }
        return month <= 9 ? "III" : "IV";
    }

    private void checkCharge(PaymentRequest request, BigDecimal amount) {
        PendingCharge charge = chargeDao.findById(request.chargeId())
                .orElseThrow(() -> new BusinessException("El cobro no existe."));
        if (charge.studentId() != request.studentId() || charge.conceptId() != request.conceptId()) {
            throw new BusinessException("El cobro no corresponde a este estudiante y concepto.");
        }
        if (!PendingCharge.PENDING.equals(charge.status())) {
            throw new BusinessException("Ese cobro ya fue pagado.");
        }
        if (amount.compareTo(charge.expectedAmount()) != 0) {
            throw new BusinessException("El pago de un cobro debe ser exacto: " + charge.expectedAmount().toPlainString() + ".");
        }
    }
}
