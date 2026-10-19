package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A grade (nota) of a student in a subject and period. Maps the table "registros_academicos".
 */
public record AcademicRecord(int id, int studentId, String studentDocument, String studentName,
                             int subjectId, String subjectName, int professorId, int academicYear,
                             String period, BigDecimal value, String notes, LocalDate recordDate) {

    /** Periods that count for averages. */
    public static final List<String> REGULAR_PERIODS = List.of("I", "II", "III", "IV");
    /** All the periods a grade can be saved in. */
    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Final");
}
