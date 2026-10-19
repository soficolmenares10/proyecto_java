package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.util.Validator;
import java.time.LocalDate;
import java.util.List;

/**
 * Small reusable checks used by the controllers. Each one throws a
 * BusinessException with a clear message when the rule is broken.
 */
final class Rules {

    private Rules() {
        // Utility class: no instances.
    }

    static void requireText(String value, String label, int maxLength) {
        if (!Validator.isNotBlank(value)) {
            throw new BusinessException(label + " es obligatorio.");
        }
        limitText(value, label, maxLength);
    }

    static void limitText(String value, String label, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(label + " no puede tener más de " + maxLength + " caracteres.");
        }
    }

    static void requireOneOf(String value, List<String> allowed, String label) {
        if (value == null || !allowed.contains(value)) {
            throw new BusinessException(label + " no es válido. Opciones: " + String.join(", ", allowed) + ".");
        }
    }

    static void optionalPhone(String value, String label) {
        if (value != null && !Validator.isValidPhone(value)) {
            throw new BusinessException(label + " no es un teléfono válido.");
        }
    }

    static void optionalEmail(String value, String label) {
        if (value != null && !Validator.isValidEmail(value)) {
            throw new BusinessException(label + " no es un correo válido.");
        }
    }

    static void requireAcademicYear(int year) {
        int maxYear = LocalDate.now().getYear() + 1;
        if (year < 2000 || year > maxYear) {
            throw new BusinessException("El año académico debe estar entre 2000 y " + maxYear + ".");
        }
    }
}
