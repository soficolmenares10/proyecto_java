package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;
import java.util.List;
import java.util.Optional;

/** Data access operations for the enrollment of students in subjects (period by period). */
public interface SubjectEnrollmentDao {

    /**
     * Saves several enrollments in one transaction (all or none).
     *
     * @return how many enrollments were saved
     */
    int enrollAll(List<SubjectEnrollment> enrollments);

    Optional<SubjectEnrollment> find(int studentId, int subjectId, int academicYear, String period);

    List<SubjectEnrollment> findByStudent(int studentId, int academicYear);

    List<SubjectEnrollment> findBySubject(int subjectId, int academicYear, String period);

    /** Each student enrolled (and not withdrawn) in the subject during the year, listed once. */
    List<SubjectEnrollment> findDistinctStudentsBySubject(int subjectId, int academicYear);

    boolean setStatus(int enrollmentId, String status);
}
