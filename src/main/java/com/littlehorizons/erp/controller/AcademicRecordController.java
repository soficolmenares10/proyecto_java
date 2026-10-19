package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.ReportCardRow;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectAverage;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Business logic for grades (notas): registering them, and calculating the averages
 * by subject, by period and by course.
 */
public class AcademicRecordController {

    private static final int AVERAGE_DECIMALS = 2;

    private final AcademicRecordDao recordDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AssignmentDao assignmentDao;
    private final ProfessorDao professorDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;

    public AcademicRecordController(AcademicRecordDao recordDao, SubjectEnrollmentDao subjectEnrollmentDao,
                                    AssignmentDao assignmentDao, ProfessorDao professorDao,
                                    StudentDao studentDao, SubjectDao subjectDao) {
        this.recordDao = recordDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.assignmentDao = assignmentDao;
        this.professorDao = professorDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
    }

    // ------------------------------------------------------------ registering

    /**
     * Saves the grade of a student. If the student already has a grade for that subject and
     * period, it is replaced.
     *
     * @return true if a new grade was created, false if an existing one was updated
     */
    public boolean registerGrade(int studentId, int subjectId, int professorId, int academicYear,
                                 String period, BigDecimal value, String notes) {
        if (studentDao.findById(studentId).isEmpty()) {
            throw new BusinessException("El estudiante no existe.");
        }
        if (professorDao.findById(professorId).isEmpty()) {
            throw new BusinessException("El profesor no existe.");
        }
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("La materia no existe."));
        Rules.requireAcademicYear(academicYear);
        Rules.requireOneOf(period, AcademicRecord.PERIODS, "El período");
        if (!Validator.isValidGrade(value)) {
            throw new BusinessException("La nota debe estar entre 0.0 y 5.0 con un decimal.");
        }
        Rules.limitText(notes, "Las observaciones", 1000);
        if (!assignmentDao.isAssigned(professorId, subjectId, academicYear)) {
            throw new BusinessException("El profesor no dicta " + subject.getName() + " en " + academicYear
                    + ". Asígnele la materia primero.");
        }
        checkStudentIsEnrolled(studentId, subject, academicYear, period);

        Optional<AcademicRecord> existing = recordDao.find(studentId, subjectId, academicYear, period);
        if (existing.isPresent()) {
            recordDao.update(existing.get().id(), value, notes, professorId, LocalDate.now());
            return false;
        }
        recordDao.insert(new AcademicRecord(0, studentId, null, null, subjectId, null, professorId,
                academicYear, period, value, notes, LocalDate.now()));
        return true;
    }

    private void checkStudentIsEnrolled(int studentId, Subject subject, int academicYear, String period) {
        boolean enrolled;
        if (AcademicRecord.REGULAR_PERIODS.contains(period)) {
            enrolled = subjectEnrollmentDao.find(studentId, subject.getId(), academicYear, period)
                    .filter(enrollment -> !SubjectEnrollment.WITHDRAWN.equals(enrollment.status()))
                    .isPresent();
        } else {
            enrolled = subjectEnrollmentDao.findDistinctStudentsBySubject(subject.getId(), academicYear).stream()
                    .anyMatch(enrollment -> enrollment.studentId() == studentId);
        }
        if (!enrolled) {
            throw new BusinessException("El estudiante no está inscrito en " + subject.getName()
                    + " para ese período. Inscríbalo primero.");
        }
    }

    // ---------------------------------------------------------------- queries

    /** Complete academic history of a student, newest year first. */
    public List<AcademicRecord> history(int studentId) {
        return recordDao.findByStudent(studentId).stream()
                .sorted(Comparator.comparingInt(AcademicRecord::academicYear).reversed()
                        .thenComparing(AcademicRecord::subjectName)
                        .thenComparingInt(record -> AcademicRecord.PERIODS.indexOf(record.period())))
                .collect(Collectors.toList());
    }

    /** Grades of one course (subject) in one period, ordered by student name. */
    public List<AcademicRecord> courseGrades(int subjectId, int academicYear, String period) {
        Rules.requireAcademicYear(academicYear);
        return recordDao.findBySubjectAndYear(subjectId, academicYear).stream()
                .filter(record -> record.period().equals(period))
                .sorted(Comparator.comparing(AcademicRecord::studentName))
                .collect(Collectors.toList());
    }

    // --------------------------------------------------------------- averages

    /**
     * Average of the whole course in one subject.
     *
     * @param period a regular period (I to IV), or null to use all the regular periods
     */
    public SubjectAverage courseAverage(int subjectId, int academicYear, String period) {
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("La materia no existe."));
        Rules.requireAcademicYear(academicYear);
        List<BigDecimal> values = recordDao.findBySubjectAndYear(subjectId, academicYear).stream()
                .filter(record -> period == null
                        ? AcademicRecord.REGULAR_PERIODS.contains(record.period())
                        : record.period().equals(period))
                .map(AcademicRecord::value)
                .collect(Collectors.toList());
        return new SubjectAverage(subject.getName(), values.size(), average(values));
    }

    /** Average of each subject of a student during a year (periods I to IV). */
    public List<SubjectAverage> studentSubjectAverages(int studentId, int academicYear) {
        return regularRecordsBySubject(studentId, academicYear).entrySet().stream()
                .map(entry -> new SubjectAverage(entry.getKey(), entry.getValue().size(),
                        average(entry.getValue().stream().map(AcademicRecord::value).collect(Collectors.toList()))))
                .collect(Collectors.toList());
    }

    /** General average of a student in one period (all his or her subjects). */
    public Optional<BigDecimal> studentPeriodAverage(int studentId, int academicYear, String period) {
        List<BigDecimal> values = recordDao.findByStudentAndYear(studentId, academicYear).stream()
                .filter(record -> record.period().equals(period))
                .map(AcademicRecord::value)
                .collect(Collectors.toList());
        return Optional.ofNullable(average(values));
    }

    /** General average of a student in a year: the average of his or her subject averages. */
    public Optional<BigDecimal> studentGeneralAverage(int studentId, int academicYear) {
        List<BigDecimal> subjectAverages = studentSubjectAverages(studentId, academicYear).stream()
                .map(SubjectAverage::average)
                .collect(Collectors.toList());
        return Optional.ofNullable(average(subjectAverages));
    }

    /** The report card of a student: one row per subject with the grade of each period. */
    public List<ReportCardRow> reportCard(int studentId, int academicYear) {
        return regularRecordsBySubject(studentId, academicYear).entrySet().stream()
                .map(entry -> {
                    Map<String, BigDecimal> byPeriod = new LinkedHashMap<>();
                    for (String period : AcademicRecord.REGULAR_PERIODS) {
                        entry.getValue().stream()
                                .filter(record -> record.period().equals(period))
                                .findFirst()
                                .ifPresent(record -> byPeriod.put(period, record.value()));
                    }
                    BigDecimal subjectAverage = average(byPeriod.values());
                    return new ReportCardRow(entry.getKey(), byPeriod, subjectAverage);
                })
                .collect(Collectors.toList());
    }

    private Map<String, List<AcademicRecord>> regularRecordsBySubject(int studentId, int academicYear) {
        Rules.requireAcademicYear(academicYear);
        return recordDao.findByStudentAndYear(studentId, academicYear).stream()
                .filter(record -> AcademicRecord.REGULAR_PERIODS.contains(record.period()))
                .collect(Collectors.groupingBy(AcademicRecord::subjectName, TreeMap::new, Collectors.toList()));
    }

    /** Average rounded to 2 decimals, or null if there are no values. */
    private static BigDecimal average(Collection<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), AVERAGE_DECIMALS, RoundingMode.HALF_UP);
    }
}
