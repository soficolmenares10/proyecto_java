package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

/**
 * Builds a SIMULATED CUFE (Unique Electronic Invoice Code).
 * The real CUFE is calculated by the DIAN rules; here it is a SHA-384 hash of the invoice
 * number, the school NIT, the date and the total, which is enough for this project.
 */
public final class CufeGenerator {

    private CufeGenerator() {
        // Utility class: no instances.
    }

    public static String generate(String invoiceNumber, String nit, LocalDate issueDate, BigDecimal total) {
        String source = invoiceNumber + "|" + nit + "|" + issueDate + "|" + total.toPlainString();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-384").digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-384 exists in every Java installation, so this should never happen.
            throw new IllegalStateException("SHA-384 is not available", e);
        }
    }
}
