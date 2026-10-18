package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access operations for grades and enrollments.
 */
public interface EnrollmentDao {

    List<Grade> findAllGrades();

    /** @return the generated enrollment id */
    int enroll(Enrollment enrollment);

    Optional<Enrollment> findByStudentAndYear(int studentId, int academicYear);

    /** All the enrollments of one student, newest year first. */
    List<Enrollment> findByStudentId(int studentId);

    List<Enrollment> findByGradeAndYear(int gradeId, int academicYear);

    List<Enrollment> findByYear(int academicYear);

    /**
     * Marks an active enrollment (and its student) as withdrawn, in one transaction.
     *
     * @return false if the enrollment was not active
     */
    boolean withdraw(int enrollmentId, LocalDate date, String notes);

    /**
     * Marks an active enrollment (and its student) as graduated, in one transaction.
     *
     * @return false if the enrollment was not active
     */
    boolean graduate(int enrollmentId, LocalDate date, String notes);
}
