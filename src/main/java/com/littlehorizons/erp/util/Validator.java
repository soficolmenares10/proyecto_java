package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * Pure validation rules (no console input/output here).
 * Keeping the rules separate from the console makes them easy to reuse and test.
 */
public final class Validator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("^[A-Za-z0-9]{5,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+ ]{7,20}$");

    private static final BigDecimal MIN_GRADE = new BigDecimal("0.0");
    private static final BigDecimal MAX_GRADE = new BigDecimal("5.0");

    private Validator() {
        // Utility class: no instances.
    }

    public static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidDocument(String document) {
        return document != null && DOCUMENT_PATTERN.matcher(document.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    /**
     * Colombian grade scale: 0.0 to 5.0 with at most one decimal.
     */
    public static boolean isValidGrade(BigDecimal grade) {
        return grade != null
                && grade.scale() <= 1
                && grade.compareTo(MIN_GRADE) >= 0
                && grade.compareTo(MAX_GRADE) <= 0;
    }
}

