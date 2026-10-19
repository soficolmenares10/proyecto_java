package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
import java.time.LocalDate;
import java.util.List;

/** Data access operations for daily attendance. */
public interface AttendanceDao {

    /**
     * Saves the attendance of several students in one transaction. If a student already has
     * attendance for that subject and day, it is updated instead of duplicated.
     */
    void saveAll(List<Attendance> attendances);

    List<Attendance> findBySubjectAndDate(int subjectId, LocalDate date);

    List<Attendance> findByStudent(int studentId, LocalDate from, LocalDate to);

    /** All the attendance rows with the given status between two dates (all students). */
    List<Attendance> findByStatusBetween(String status, LocalDate from, LocalDate to);
}
