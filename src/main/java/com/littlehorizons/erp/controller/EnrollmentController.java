package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Business logic for enrollments (matrículas).
 */
public class EnrollmentController {

    private static final int MIN_YEAR = 2000;
    private static final int LAST_GRADE_LEVEL = 11;

    private final EnrollmentDao enrollmentDao;
    private final StudentDao studentDao;

    public EnrollmentController(EnrollmentDao enrollmentDao, StudentDao studentDao) {
        this.enrollmentDao = enrollmentDao;
        this.studentDao = studentDao;
    }

    public List<Grade> getGrades() {
        return enrollmentDao.findAllGrades();
    }

    /**
     * Enrolls an active student in a grade for an academic year.
     *
     * @return the new enrollment id
     */
    public int enroll(int studentId, int gradeId, int academicYear, String notes) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no existe."));
        if (!"Activo".equals(student.getStatus())) {
            throw new BusinessException("Solo se pueden matricular estudiantes activos. Estado actual: "
                    + student.getStatus() + ".");
        }
        checkYear(academicYear);
        boolean gradeExists = getGrades().stream().anyMatch(grade -> grade.getId() == gradeId);
        if (!gradeExists) {
            throw new BusinessException("El grado seleccionado no existe.");
        }
        if (enrollmentDao.findByStudentAndYear(studentId, academicYear).isPresent()) {
            throw new BusinessException("El estudiante ya tiene una matrícula en el año " + academicYear + ".");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudentId(studentId);
        enrollment.setGradeId(gradeId);
        enrollment.setAcademicYear(academicYear);
        enrollment.setStatus(Enrollment.ACTIVE);
        enrollment.setEnrollmentDate(LocalDate.now());
        enrollment.setNotes(notes);
        return enrollmentDao.enroll(enrollment);
    }

    public List<Enrollment> history(int studentId) {
        return enrollmentDao.findByStudentId(studentId);
    }

    public List<Enrollment> listByGradeAndYear(int gradeId, int academicYear) {
        checkYear(academicYear);
        return enrollmentDao.findByGradeAndYear(gradeId, academicYear);
    }

    public List<Enrollment> listByYear(int academicYear) {
        checkYear(academicYear);
        return enrollmentDao.findByYear(academicYear);
    }

    /** Map: student id -> grade name, for the enrollments of one year. */
    public Map<Integer, String> currentGradeByStudent(int academicYear) {
        return enrollmentDao.findByYear(academicYear).stream()
                .collect(Collectors.toMap(Enrollment::getStudentId, Enrollment::getGradeName,
                        (first, second) -> first));
    }

    public Enrollment getActiveEnrollment(int studentId, int academicYear) {
        Enrollment enrollment = enrollmentDao.findByStudentAndYear(studentId, academicYear)
                .orElseThrow(() -> new BusinessException(
                        "El estudiante no tiene matrícula en el año " + academicYear + "."));
        if (!Enrollment.ACTIVE.equals(enrollment.getStatus())) {
            throw new BusinessException("La matrícula de " + academicYear + " no está activa. Estado: "
                    + enrollment.getStatus() + ".");
        }
        return enrollment;
    }

    public void withdraw(int studentId, int academicYear, String reason) {
        Enrollment enrollment = getActiveEnrollment(studentId, academicYear);
        if (!enrollmentDao.withdraw(enrollment.getId(), LocalDate.now(), reason)) {
            throw new BusinessException("No se pudo retirar al estudiante.");
        }
    }

    public void graduate(int studentId, int academicYear) {
        Enrollment enrollment = getActiveEnrollment(studentId, academicYear);
        if (enrollment.getGradeLevel() != LAST_GRADE_LEVEL) {
            throw new BusinessException("Solo se pueden graduar estudiantes de grado Once. Grado actual: "
                    + enrollment.getGradeName() + ".");
        }
        if (!enrollmentDao.graduate(enrollment.getId(), LocalDate.now(), null)) {
            throw new BusinessException("No se pudo graduar al estudiante.");
        }
    }

    private void checkYear(int academicYear) {
        int maxYear = LocalDate.now().getYear() + 1;
        if (academicYear < MIN_YEAR || academicYear > maxYear) {
            throw new BusinessException("El año académico debe estar entre " + MIN_YEAR + " y " + maxYear + ".");
        }
    }
}
