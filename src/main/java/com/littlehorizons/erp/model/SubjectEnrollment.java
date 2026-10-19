package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A student enrolled in a subject for one academic period.
 * Maps the table "inscripciones_materia".
 */
public record SubjectEnrollment(int id, int studentId, String studentDocument, String studentName,
                                int subjectId, String subjectName, int academicYear, String period,
                                String section, LocalDate enrollmentDate, String status) {

    public static final List<String> PERIODS = List.of("I", "II", "III", "IV");
    public static final String ACTIVE = "Activa";
    public static final String WITHDRAWN = "Retirada";
}
