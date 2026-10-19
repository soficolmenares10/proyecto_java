package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.util.Map;

/** One line of a student's report card: the grade of each period and the subject average. */
public record ReportCardRow(String subjectName, Map<String, BigDecimal> gradeByPeriod, BigDecimal average) {
}
