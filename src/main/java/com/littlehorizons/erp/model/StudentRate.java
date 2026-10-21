package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/**
 * A custom amount of a concept for one student (sibling discount, partial scholarship...).
 * Maps the table "valores_estudiante".
 */
public record StudentRate(int id, int studentId, String studentName, int conceptId, String conceptName,
                          BigDecimal amount, String reason, int academicYear, boolean active) {
}
