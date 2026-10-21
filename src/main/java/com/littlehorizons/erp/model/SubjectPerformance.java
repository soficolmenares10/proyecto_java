package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** How the students of a subject are doing: average, number of students and low grades. */
public record SubjectPerformance(String subjectName, String gradeName, long students, BigDecimal average,
                                 long lowGrades) {
}
