package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * The data the user types to register a payment. If chargeId is not null, the payment
 * cancels that pending charge.
 */
public record PaymentRequest(int studentId, int conceptId, Integer chargeId, int guardianId, BigDecimal amount,
                             String method, int academicYear, String period, LocalDate date, String notes) {
}
