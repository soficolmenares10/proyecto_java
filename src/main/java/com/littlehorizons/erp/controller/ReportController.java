package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.DebtorSummary;
import com.littlehorizons.erp.model.CollectionSummary;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EventSummary;
import com.littlehorizons.erp.model.GeneralSummary;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.GradeAverage;
import com.littlehorizons.erp.model.InventoryRow;
import com.littlehorizons.erp.model.ParticipationSummary;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.model.SubjectPerformance;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for the reports and statistics. It combines data from several DAOs
 * and uses streams to filter, group and sort it.
 */
public class ReportController {

    private final ReportDao reportDao;
    private final StudentDao studentDao;
    private final ProfessorDao professorDao;
    private final ClubDao clubDao;
    private final EnrollmentDao enrollmentDao;
    private final PaymentDao paymentDao;
    private final ChargeController chargeController;
    private final LoanController loanController;

    public ReportController(ReportDao reportDao, StudentDao studentDao, ProfessorDao professorDao,
                            ClubDao clubDao, EnrollmentDao enrollmentDao, PaymentDao paymentDao,
                            ChargeController chargeController, LoanController loanController) {
        this.reportDao = reportDao;
        this.studentDao = studentDao;
        this.professorDao = professorDao;
        this.clubDao = clubDao;
        this.enrollmentDao = enrollmentDao;
        this.paymentDao = paymentDao;
        this.chargeController = chargeController;
        this.loanController = loanController;
    }

    // ------------------------------------------------------------------ academic

    /** Students whose average in the year is below the limit, the lowest first. */
    public List<StudentAverage> studentsNeedingSupport(int academicYear, BigDecimal limit) {
        Rules.requireAcademicYear(academicYear);
        checkGradeLimit(limit);
        return reportDao.studentAverages(academicYear).stream()
                .filter(student -> student.average().compareTo(limit) < 0)
                .sorted(Comparator.comparing(StudentAverage::average).thenComparing(StudentAverage::studentName))
                .collect(Collectors.toList());
    }

    /** The average of every grade (Preescolar to Once) in the year, in grade order. */
    public List<GradeAverage> averageByGrade(int academicYear) {
        Rules.requireAcademicYear(academicYear);
        List<String> gradeOrder = enrollmentDao.findAllGrades().stream()
                .map(Grade::getName)
                .collect(Collectors.toList());
        Map<String, List<StudentAverage>> byGrade = reportDao.studentAverages(academicYear).stream()
                .collect(Collectors.groupingBy(StudentAverage::gradeName));
        return byGrade.entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> gradeOrder.indexOf(entry.getKey())))
                .map(entry -> new GradeAverage(entry.getKey(), entry.getValue().size(),
                        average(entry.getValue().stream().map(StudentAverage::average).collect(Collectors.toList()))))
                .collect(Collectors.toList());
    }

    public List<SubjectPerformance> professorPerformance(int professorId, int academicYear) {
        Rules.requireAcademicYear(academicYear);
        return reportDao.subjectPerformanceOfProfessor(professorId, academicYear);
    }

    public List<StudentAverage> professorLowStudents(int professorId, int academicYear, BigDecimal limit) {
        Rules.requireAcademicYear(academicYear);
        checkGradeLimit(limit);
        return reportDao.lowStudentsOfProfessor(professorId, academicYear, limit);
    }

    // ------------------------------------------------------------ extracurricular

    /** The students with more activities (clubs + events), the most active first. */
    public List<ParticipationSummary> mostActiveStudents(int limit) {
        return reportDao.participationByStudent().stream()
                .filter(summary -> summary.totalActivities() > 0)
                .sorted(Comparator.comparingLong(ParticipationSummary::totalActivities).reversed()
                        .thenComparing(Comparator.comparingLong(ParticipationSummary::podiums).reversed())
                        .thenComparing(ParticipationSummary::studentName))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** Active students that are not in any club and did not take part in any event. */
    public List<ParticipationSummary> studentsWithoutActivities() {
        return reportDao.participationByStudent().stream()
                .filter(summary -> summary.totalActivities() == 0)
                .collect(Collectors.toList());
    }

    public List<EventSummary> eventParticipation(int year) {
        return reportDao.eventSummaries(year);
    }

    /** Active clubs ordered by how full they are (most full first). */
    public List<Club> clubOccupancy() {
        return clubDao.findAll().stream()
                .filter(Club::isActive)
                .sorted(Comparator.comparingDouble((Club club) -> (double) club.getActiveMembers() / club.getCapacity())
                        .reversed().thenComparing(Club::getName))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- financial

    /** Money collected between two dates, in total and grouped by concept and by payment method. */
    public CollectionSummary collections(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException("La fecha inicial debe ser anterior o igual a la fecha final.");
        }
        List<Payment> payments = paymentDao.findBetween(from, to);
        BigDecimal total = payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> byConcept = payments.stream()
                .collect(Collectors.groupingBy(Payment::conceptName, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)));
        Map<String, BigDecimal> byMethod = payments.stream()
                .collect(Collectors.groupingBy(Payment::method, TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Payment::amount, BigDecimal::add)));
        return new CollectionSummary(total, payments.size(), byConcept, byMethod);
    }

    /** Students with charges overdue for more than one month. */
    public List<DebtorSummary> debtors() {
        return chargeController.debtors();
    }

    // ---------------------------------------------------------------- resources

    public List<InventoryRow> inventory() {
        return reportDao.inventory();
    }

    // ------------------------------------------------------------------ general

    /** The main numbers of the school today. */
    public GeneralSummary generalSummary() {
        LocalDate today = LocalDate.now();
        Map<String, Long> studentsByStatus = studentDao.findAll().stream()
                .collect(Collectors.groupingBy(Student::getStatus, TreeMap::new, Collectors.counting()));
        long activeProfessors = professorDao.findAll().stream()
                .filter(professor -> "Activo".equals(professor.getStatus()))
                .count();
        long activeClubs = clubDao.findAll().stream().filter(Club::isActive).count();
        long enrollments = enrollmentDao.findByYear(today.getYear()).stream()
                .filter(enrollment -> Enrollment.ACTIVE.equals(enrollment.getStatus()))
                .count();
        BigDecimal collected = paymentDao.findBetween(today.withDayOfMonth(1), today).stream()
                .map(Payment::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GeneralSummary(studentsByStatus, activeProfessors, activeClubs, enrollments, collected,
                chargeController.debtors().size(), loanController.overdueLoans().size());
    }

    // ------------------------------------------------------------------ helpers

    private void checkGradeLimit(BigDecimal limit) {
        if (limit == null || limit.signum() < 0 || limit.compareTo(new BigDecimal("5.0")) > 0) {
            throw new BusinessException("El promedio mínimo debe estar entre 0.0 y 5.0.");
        }
    }

    private static BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }
}
