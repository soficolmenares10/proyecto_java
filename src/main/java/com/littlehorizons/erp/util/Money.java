package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Formats amounts in Colombian pesos, for example $ 1.250.000,00
 */
public final class Money {

    private static final Locale COLOMBIA = Locale.forLanguageTag("es-CO");

    private Money() {
        // Utility class: no instances.
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return String.format(COLOMBIA, "$ %,.2f", amount);
    }
}
