package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An amount the school billed to a student. It is "Pendiente" until a payment cancels it.
 * Maps the table "cobros_pendientes".
 */
public record PendingCharge(int id, int studentId, String studentName, String studentDocument,
                            int conceptId, String conceptName, int academicYear, String description,
                            BigDecimal expectedAmount, LocalDate dueDate, String status, Integer paymentId) {

    public static final String PENDING = "Pendiente";
    public static final String PAID = "Pagado";
}
