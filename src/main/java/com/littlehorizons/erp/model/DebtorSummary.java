package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A student with overdue charges: how many, how much, and since when. */
public record DebtorSummary(int studentId, String studentName, String studentDocument, long overdueCharges,
                            BigDecimal totalOwed, LocalDate oldestDueDate, long daysOverdue) {
}
