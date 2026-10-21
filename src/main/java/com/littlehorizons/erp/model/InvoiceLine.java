package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** One line of an invoice: description, quantity, unit value and subtotal (before IVA). */
public record InvoiceLine(String description, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
