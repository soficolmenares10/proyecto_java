package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeachingAssignment;
import java.util.List;
import java.util.Optional;

/** Data access operations for the assignment of subjects to professors. */
public interface AssignmentDao {

    /** @return the generated assignment id */
    int insert(int professorId, int subjectId, int academicYear, String section);

    boolean exists(int professorId, int subjectId, int academicYear, String section);

    /** True if the professor teaches the subject in that year (in any section). */
    boolean isAssigned(int professorId, int subjectId, int academicYear);

    Optional<TeachingAssignment> findById(int assignmentId);

    List<TeachingAssignment> findByProfessorAndYear(int professorId, int academicYear);

    List<TeachingAssignment> findBySubjectAndYear(int subjectId, int academicYear);

    boolean delete(int assignmentId);
}
