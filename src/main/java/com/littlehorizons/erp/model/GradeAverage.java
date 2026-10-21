package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** The average of all the students of a grade in one year. */
public record GradeAverage(String gradeName, long students, BigDecimal average) {
}
