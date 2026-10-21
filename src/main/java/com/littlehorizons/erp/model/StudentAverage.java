package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** The average of a student in one year (periods I to IV), used by the performance reports. */
public record StudentAverage(int studentId, String studentName, String studentDocument, String gradeName,
                             BigDecimal average, long gradesCount) {
}
