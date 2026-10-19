package com.littlehorizons.erp.model;

import java.time.LocalTime;
import java.util.List;

/**
 * One weekly class hour of a teaching assignment (day, start and end time).
 * Maps the table "horarios_clase".
 */
public record ClassSchedule(int id, int assignmentId, int professorId, String day, LocalTime startTime,
                            LocalTime endTime, String room, int academicYear, String section,
                            String subjectName, int gradeId, String gradeName) {

    public static final List<String> DAYS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");

    /** True if both schedules are on the same day and their hours overlap. */
    public boolean overlapsWith(String otherDay, LocalTime otherStart, LocalTime otherEnd) {
        return day.equals(otherDay) && startTime.isBefore(otherEnd) && endTime.isAfter(otherStart);
    }
}
