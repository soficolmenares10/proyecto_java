package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for enrolling students in the subjects of their grade, period by period.
 */
public class SubjectEnrollmentController {

    private static final String DEFAULT_SECTION = "A";

    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final StudentDao studentDao;
    private final EnrollmentDao enrollmentDao;
    private final SubjectDao subjectDao;

    public SubjectEnrollmentController(SubjectEnrollmentDao subjectEnrollmentDao, StudentDao studentDao,
                                       EnrollmentDao enrollmentDao, SubjectDao subjectDao) {
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.studentDao = studentDao;
        this.enrollmentDao = enrollmentDao;
        this.subjectDao = subjectDao;
    }

    /**
     * Enrolls the student in all the active subjects of the grade where he or she is enrolled
     * that year. Subjects where the student is already enrolled are skipped.
     *
     * @return how many subjects were added
     */
    public int enrollInGradeSubjects(int studentId, int academicYear, String period) {
        Enrollment gradeEnrollment = findActiveGradeEnrollment(studentId, academicYear);
        checkPeriod(period);
        List<SubjectEnrollment> toSave = new ArrayList<>();
        for (Subject subject : subjectDao.findByGrade(gradeEnrollment.getGradeId())) {
            if (subject.isActive()
                    && subjectEnrollmentDao.find(studentId, subject.getId(), academicYear, period).isEmpty()) {
                toSave.add(newEnrollment(studentId, subject.getId(), academicYear, period));
            }
        }
        if (toSave.isEmpty()) {
            throw new BusinessException("El estudiante ya está inscrito en todas las materias de "
                    + gradeEnrollment.getGradeName() + " para el período " + period + ".");
        }
        return subjectEnrollmentDao.enrollAll(toSave);
    }

    /** Enrolls the student in one subject, which must belong to the grade he or she attends. */
    public void enrollInSubject(int studentId, int subjectId, int academicYear, String period) {
        Enrollment gradeEnrollment = findActiveGradeEnrollment(studentId, academicYear);
        checkPeriod(period);
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("La materia no existe."));
        if (!subject.isActive()) {
            throw new BusinessException("La materia " + subject.getName() + " está inactiva.");
        }
        if (subject.getGradeId() != gradeEnrollment.getGradeId()) {
            throw new BusinessException("La materia " + subject.getName() + " es de " + subject.getGradeName()
                    + " y el estudiante está matriculado en " + gradeEnrollment.getGradeName() + ".");
        }
        if (subjectEnrollmentDao.find(studentId, subjectId, academicYear, period).isPresent()) {
            throw new BusinessException("El estudiante ya está inscrito en esa materia para el período " + period + ".");
        }
        subjectEnrollmentDao.enrollAll(List.of(newEnrollment(studentId, subjectId, academicYear, period)));
    }

    public void withdraw(int studentId, int subjectId, int academicYear, String period) {
        Optional<SubjectEnrollment> found = subjectEnrollmentDao.find(studentId, subjectId, academicYear, period);
        SubjectEnrollment enrollment = found.orElseThrow(
                () -> new BusinessException("El estudiante no está inscrito en esa materia para ese período."));
        if (SubjectEnrollment.WITHDRAWN.equals(enrollment.status())) {
            throw new BusinessException("La inscripción ya estaba retirada.");
        }
        subjectEnrollmentDao.setStatus(enrollment.id(), SubjectEnrollment.WITHDRAWN);
    }

    public List<SubjectEnrollment> listByStudent(int studentId, int academicYear) {
        Rules.requireAcademicYear(academicYear);
        return subjectEnrollmentDao.findByStudent(studentId, academicYear);
    }

    public List<SubjectEnrollment> listBySubject(int subjectId, int academicYear, String period) {
        Rules.requireAcademicYear(academicYear);
        checkPeriod(period);
        return subjectEnrollmentDao.findBySubject(subjectId, academicYear, period);
    }

    private Enrollment findActiveGradeEnrollment(int studentId, int academicYear) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no existe."));
        Rules.requireAcademicYear(academicYear);
        Enrollment enrollment = enrollmentDao.findByStudentAndYear(studentId, academicYear)
                .orElseThrow(() -> new BusinessException(student.getFullName()
                        + " no tiene matrícula en el año " + academicYear + ". Matricúlelo primero."));
        if (!Enrollment.ACTIVE.equals(enrollment.getStatus())) {
            throw new BusinessException("La matrícula de " + academicYear + " no está activa (estado: "
                    + enrollment.getStatus() + ").");
        }
        return enrollment;
    }

    private SubjectEnrollment newEnrollment(int studentId, int subjectId, int academicYear, String period) {
        return new SubjectEnrollment(0, studentId, null, null, subjectId, null, academicYear, period,
                DEFAULT_SECTION, LocalDate.now(), SubjectEnrollment.ACTIVE);
    }

    private void checkPeriod(String period) {
        Rules.requireOneOf(period, SubjectEnrollment.PERIODS, "El período");
    }
}
