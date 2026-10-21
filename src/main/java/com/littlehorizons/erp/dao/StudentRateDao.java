package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentRate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Data access operations for custom amounts per student (discounts, scholarships). */
public interface StudentRateDao {

    /** The active custom amount of a student for a concept in a year, if there is one. */
    Optional<StudentRate> findActive(int studentId, int conceptId, int academicYear);

    List<StudentRate> findByStudent(int studentId);

    Optional<StudentRate> findById(int rateId);

    /**
     * Creates the custom amount, or updates it (and reactivates it) if the student already had
     * one for that concept and year.
     */
    void save(int studentId, int conceptId, int academicYear, BigDecimal amount, String reason);

    boolean deactivate(int rateId);
}
