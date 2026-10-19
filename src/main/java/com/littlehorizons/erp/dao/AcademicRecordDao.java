package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access operations for grades (registros académicos). */
public interface AcademicRecordDao {

    /** @return the generated record id */
    int insert(AcademicRecord record);

    /** Changes the grade, the observations, the professor and the date of an existing record. */
    boolean update(int recordId, BigDecimal value, String notes, int professorId, LocalDate date);

    Optional<AcademicRecord> find(int studentId, int subjectId, int academicYear, String period);

    /** The whole academic history of a student (all years). */
    List<AcademicRecord> findByStudent(int studentId);

    List<AcademicRecord> findByStudentAndYear(int studentId, int academicYear);

    List<AcademicRecord> findBySubjectAndYear(int subjectId, int academicYear);
}
