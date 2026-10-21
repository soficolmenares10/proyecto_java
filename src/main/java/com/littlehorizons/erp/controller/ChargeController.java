package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.ChargeGenerationResult;
import com.littlehorizons.erp.model.DebtorSummary;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PendingCharge;
import com.littlehorizons.erp.model.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for the charges billed to students and for the report of debtors (morosos).
 */
public class ChargeController {

    private static final int DUE_DAY = 5;

    private final ChargeDao chargeDao;
    private final StudentDao studentDao;
    private final EnrollmentDao enrollmentDao;
    private final ConceptController conceptController;

    public ChargeController(ChargeDao chargeDao, StudentDao studentDao, EnrollmentDao enrollmentDao,
                            ConceptController conceptController) {
        this.chargeDao = chargeDao;
        this.studentDao = studentDao;
        this.enrollmentDao = enrollmentDao;
        this.conceptController = conceptController;
    }

    /**
     * Creates one charge for one student. The amount is the base amount of the concept or the
     * student's custom amount.
     *
     * @return the new charge id
     */
    public int create(int studentId, int conceptId, int academicYear, String description, LocalDate dueDate) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no existe."));
        if (!"Activo".equals(student.getStatus())) {
            throw new BusinessException("Solo se cobra a estudiantes activos. Estado actual: " + student.getStatus() + ".");
        }
        PaymentConcept concept = conceptController.getById(conceptId);
        if (!concept.isActive()) {
            throw new BusinessException("El concepto " + concept.getName() + " está inactivo.");
        }
        Rules.requireAcademicYear(academicYear);
        Rules.requireText(description, "La descripción del cobro", 60);
        if (dueDate == null) {
            throw new BusinessException("La fecha de vencimiento es obligatoria.");
        }
        if (chargeDao.exists(studentId, conceptId, academicYear, description.trim())) {
            throw new BusinessException("Ya existe un cobro con esa descripción para el estudiante.");
        }
        BigDecimal amount = conceptController.effectiveAmount(studentId, conceptId, academicYear);
        return chargeDao.insert(new PendingCharge(0, studentId, null, null, conceptId, null, academicYear,
                description.trim(), amount, dueDate, PendingCharge.PENDING, null));
    }

    /**
     * Creates the charge of a recurring concept (for example the monthly tuition) for every
     * active student that is enrolled in that year. Charges that already exist are skipped.
     * The due date is the 5th day of the month.
     */
    public ChargeGenerationResult generateMonthly(int conceptId, int academicYear, int month) {
        PaymentConcept concept = conceptController.getById(conceptId);
        if (!concept.isActive() || !concept.isRecurring()) {
            throw new BusinessException("Solo se pueden generar cobros mensuales de conceptos recurrentes y activos.");
        }
        Rules.requireAcademicYear(academicYear);
        if (month < 1 || month > 12) {
            throw new BusinessException("El mes debe estar entre 1 y 12.");
        }
        String description = concept.getName() + " " + academicYear + "-" + String.format("%02d", month);
        LocalDate dueDate = LocalDate.of(academicYear, month, DUE_DAY);

        int created = 0;
        int existed = 0;
        for (Student student : studentDao.findAll()) {
            boolean enrolled = enrollmentDao.findByStudentAndYear(student.getId(), academicYear)
                    .filter(enrollment -> Enrollment.ACTIVE.equals(enrollment.getStatus()))
                    .isPresent();
            if (!"Activo".equals(student.getStatus()) || !enrolled) {
                continue;
            }
            if (chargeDao.exists(student.getId(), conceptId, academicYear, description)) {
                existed++;
                continue;
            }
            BigDecimal amount = conceptController.effectiveAmount(student.getId(), conceptId, academicYear);
            chargeDao.insert(new PendingCharge(0, student.getId(), null, null, conceptId, null, academicYear,
                    description, amount, dueDate, PendingCharge.PENDING, null));
            created++;
        }
        return new ChargeGenerationResult(created, existed);
    }

    public List<PendingCharge> pendingOf(int studentId) {
        return chargeDao.findPendingByStudent(studentId);
    }

    public void deletePending(int chargeId) {
        PendingCharge charge = chargeDao.findById(chargeId)
                .orElseThrow(() -> new BusinessException("El cobro no existe."));
        if (!PendingCharge.PENDING.equals(charge.status())) {
            throw new BusinessException("Solo se pueden eliminar cobros pendientes.");
        }
        chargeDao.deletePending(chargeId);
    }

    /**
     * Students with at least one pending charge that was due more than one month ago.
     * Each student appears once, with how many charges are overdue, the total owed and the
     * oldest due date. The most overdue students come first.
     */
    public List<DebtorSummary> debtors() {
        LocalDate today = LocalDate.now();
        LocalDate limit = today.minusMonths(1);
        Map<Integer, List<PendingCharge>> byStudent = chargeDao.findPendingDueBefore(limit).stream()
                .collect(Collectors.groupingBy(PendingCharge::studentId, LinkedHashMap::new, Collectors.toList()));

        return byStudent.values().stream()
                .map(charges -> {
                    PendingCharge first = charges.get(0);
                    BigDecimal total = charges.stream()
                            .map(PendingCharge::expectedAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    LocalDate oldest = charges.stream()
                            .map(PendingCharge::dueDate)
                            .min(Comparator.naturalOrder())
                            .orElse(today);
                    return new DebtorSummary(first.studentId(), first.studentName(), first.studentDocument(),
                            charges.size(), total, oldest, ChronoUnit.DAYS.between(oldest, today));
                })
                .sorted(Comparator.comparingLong(DebtorSummary::daysOverdue).reversed()
                        .thenComparing(DebtorSummary::studentName))
                .collect(Collectors.toList());
    }
}
