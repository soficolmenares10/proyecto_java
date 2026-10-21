package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.util.Map;

/** The main numbers of the school, shown in the general summary report. */
public record GeneralSummary(Map<String, Long> studentsByStatus, long activeProfessors, long activeClubs,
                             long enrollmentsThisYear, BigDecimal collectedThisMonth, long debtors,
                             long overdueLoans) {
}
