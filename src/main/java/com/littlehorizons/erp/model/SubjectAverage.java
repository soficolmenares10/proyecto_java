package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** The average of a set of grades for one subject, and how many grades were used. */
public record SubjectAverage(String subjectName, long gradesCount, BigDecimal average) {
}
