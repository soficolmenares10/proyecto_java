package com.littlehorizons.erp.model;

/**
 * A professor assigned to a subject for an academic year and section.
 * Maps the table "asignaciones_profesor". It is a Java record: an immutable class
 * whose constructor, getters (id(), professorName(), ...) and toString are generated automatically.
 */
public record TeachingAssignment(int id, int professorId, String professorName, int subjectId,
                                 String subjectCode, String subjectName, int gradeId, String gradeName,
                                 int academicYear, String section) {
}
