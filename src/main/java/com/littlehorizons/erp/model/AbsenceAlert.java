package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** A student who was absent on too many days in one week. */
public record AbsenceAlert(int studentId, String studentName, String studentDocument,
                           LocalDate weekStart, long absentDays) {
}
