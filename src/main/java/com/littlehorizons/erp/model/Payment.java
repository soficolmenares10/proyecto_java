package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A payment made to the school. Maps the table "pagos".
 */
public record Payment(int id, int studentId, String studentName, String studentDocument,
                      Integer guardianId, String guardianName, int conceptId, String conceptName,
                      BigDecimal amount, LocalDate date, String method, String receiptNumber,
                      Integer academicYear, String period, String notes) {

    public static final List<String> METHODS =
            List.of("Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cheque");
    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Anual");
}
