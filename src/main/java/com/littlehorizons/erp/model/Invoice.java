package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * An electronic invoice generated from a payment. Maps the table "facturas".
 */
public record Invoice(int id, String number, int paymentId, String receiptNumber, String studentName,
                      LocalDate issueDate, BigDecimal subtotal, BigDecimal vat, BigDecimal total,
                      String cufe, String pdfFileName, String pdfPath) {
}
