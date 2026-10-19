package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance of a student to one class on one day. Maps the table "asistencias".
 */
public record Attendance(int id, int studentId, String studentDocument, String studentName,
                         int subjectId, String subjectName, LocalDate date, String status,
                         String notes, Integer registeredBy) {

    public static final String PRESENT = "Presente";
    public static final String ABSENT = "Ausente";
    public static final List<String> STATUSES = List.of(PRESENT, ABSENT, "Tardanza", "Excusa Médica");
    /** Statuses that can be marked as an "exception" (everything except Presente). */
    public static final List<String> EXCEPTIONS = List.of(ABSENT, "Tardanza", "Excusa Médica");
}
