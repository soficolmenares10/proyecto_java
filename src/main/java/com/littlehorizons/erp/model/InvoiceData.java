package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything needed to draw an invoice. The PDF generator receives this object and does
 * not need to know anything about the database.
 */
public record InvoiceData(String schoolName, String schoolNit, String schoolAddress, String schoolPhone,
                          String schoolEmail, String resolutionText, String invoiceNumber, LocalDate issueDate,
                          String cufe, String clientName, String clientDocument, String clientAddress,
                          String clientEmail, String studentName, String paymentMethod, String receiptNumber,
                          List<InvoiceLine> lines, BigDecimal subtotal, BigDecimal vat, BigDecimal total) {
}
