package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Predicate;

/**
 * Helper methods to print and to read validated data from the console.
 * Every read method repeats the question until the answer is valid.
 */
public final class ConsoleUtil {

    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleUtil() {
        // Utility class: no instances.
    }

    // ------------------------------------------------------------------ output

    public static void printTitle(String title) {
        String line = "═".repeat(title.length() + 4);
        System.out.println();
        System.out.println("╔" + line + "╗");
        System.out.println("║  " + title + "  ║");
        System.out.println("╚" + line + "╝");
    }

    public static void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    public static void printError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void printInfo(String message) {
        System.out.println("[INFO] " + message);
    }

    /**
     * Prints a table with automatic column widths.
     */
    public static void printTable(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < headers.length; i++) {
                widths[i] = Math.max(widths[i], cell(row[i]).length());
            }
        }
        String separator = buildSeparator(widths);
        System.out.println(separator);
        System.out.println(buildRow(headers, widths));
        System.out.println(separator);
        for (String[] row : rows) {
            System.out.println(buildRow(row, widths));
        }
        System.out.println(separator);
    }

    private static String cell(String value) {
        return value == null ? "" : value;
    }

    private static String buildSeparator(int[] widths) {
        StringBuilder line = new StringBuilder("+");
        for (int width : widths) {
            line.append("-".repeat(width + 2)).append('+');
        }
        return line.toString();
    }

    private static String buildRow(String[] values, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            line.append(' ').append(String.format("%-" + widths[i] + "s", cell(values[i]))).append(" |");
        }
        return line.toString();
    }

    public static void pause() {
        System.out.print("\nPresione ENTER para continuar...");
        SCANNER.nextLine();
    }

    // ------------------------------------------------------------------- input

    /**
     * Reads an integer between min and max (both included).
     */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                printError("El número debe estar entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                printError("Escriba un número entero (por ejemplo 3).");
            }
        }
    }

    public static String readRequiredText(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (!Validator.isNotBlank(input)) {
                printError("Este campo es obligatorio.");
            } else if (input.length() > maxLength) {
                printError("El texto no puede tener más de " + maxLength + " caracteres.");
            } else {
                return input;
            }
        }
    }

    /**
     * Reads an optional text. Returns null if the user just presses ENTER.
     */
    public static String readOptionalText(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt + " (opcional, ENTER para omitir): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            if (input.length() <= maxLength) {
                return input;
            }
            printError("El texto no puede tener más de " + maxLength + " caracteres.");
        }
    }

    public static String readEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (Validator.isValidEmail(input)) {
                return input;
            }
            printError("Correo inválido. Ejemplo: nombre@ejemplo.com");
        }
    }

    public static String readOptionalEmail(String prompt) {
        while (true) {
            System.out.print(prompt + " (opcional, ENTER para omitir): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            if (Validator.isValidEmail(input)) {
                return input;
            }
            printError("Correo inválido. Ejemplo: nombre@ejemplo.com");
        }
    }

    public static String readDocument(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (Validator.isValidDocument(input)) {
                return input;
            }
            printError("Documento inválido. Use de 5 a 20 letras o números, sin puntos ni espacios.");
        }
    }

    public static String readPhone(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (Validator.isValidPhone(input)) {
                return input;
            }
            printError("Teléfono inválido. Use de 7 a 20 dígitos (puede incluir + y espacios).");
        }
    }

    public static String readOptionalPhone(String prompt) {
        while (true) {
            System.out.print(prompt + " (opcional, ENTER para omitir): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            if (Validator.isValidPhone(input)) {
                return input;
            }
            printError("Teléfono inválido. Use de 7 a 20 dígitos (puede incluir + y espacios).");
        }
    }

    /**
     * Reads a date typed as yyyy-MM-dd (for example 2026-03-15).
     */
    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (aaaa-mm-dd): ");
            String input = SCANNER.nextLine().trim();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                printError("Fecha inválida. Use el formato aaaa-mm-dd, por ejemplo 2026-03-15.");
            }
        }
    }

    public static BigDecimal readPositiveAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                BigDecimal value = new BigDecimal(input);
                if (value.compareTo(BigDecimal.ZERO) > 0) {
                    return value;
                }
                printError("El valor debe ser mayor que cero.");
            } catch (NumberFormatException e) {
                printError("Valor inválido. Use solo números, por ejemplo 350000.");
            }
        }
    }

    /**
     * Reads a grade from 0.0 to 5.0 with one decimal.
     */
    public static BigDecimal readGrade(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim().replace(',', '.');
            try {
                BigDecimal value = new BigDecimal(input);
                if (Validator.isValidGrade(value)) {
                    return value;
                }
                printError("La nota debe estar entre 0.0 y 5.0 con un decimal (por ejemplo 4.3).");
            } catch (NumberFormatException e) {
                printError("Nota inválida. Ejemplo: 4.3");
            }
        }
    }

    /**
     * Asks a yes/no question. Accepts s (sí) and n (no).
     */
    public static boolean confirm(String prompt) {
        while (true) {
            System.out.print(prompt + " (s/n): ");
            String input = SCANNER.nextLine().trim().toLowerCase();
            if (input.equals("s") || input.equals("y")) {
                return true;
            }
            if (input.equals("n")) {
                return false;
            }
            printError("Escriba s para sí o n para no.");
        }
    }

    // ----------------------------------------------------------- choice lists

    /**
     * Shows a numbered list and returns the option chosen by the user.
     */
    public static String readChoice(String title, List<String> options) {
        return chooseOption(title, options, false);
    }

    /**
     * Same as readChoice, but the user may press ENTER to skip (returns null).
     */
    public static String readOptionalChoice(String title, List<String> options) {
        return chooseOption(title, options, true);
    }

    private static String chooseOption(String title, List<String> options, boolean optional) {
        System.out.println(title + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + options.get(i));
        }
        while (true) {
            System.out.print(optional ? "Seleccione (ENTER para omitir): " : "Seleccione: ");
            String input = SCANNER.nextLine().trim();
            if (optional && input.isEmpty()) {
                return null;
            }
            try {
                int number = Integer.parseInt(input);
                if (number >= 1 && number <= options.size()) {
                    return options.get(number - 1);
                }
            } catch (NumberFormatException e) {
                // Falls through to the error message below.
            }
            printError("Elija un número entre 1 y " + options.size() + ".");
        }
    }

    // ------------------------------------------- "press ENTER to keep" (updates)

    /**
     * Shows the current value; ENTER keeps it, "-" clears it (only if not required),
     * anything else must pass the validation.
     */
    public static String readKeepingCurrent(String label, String current, boolean required,
                                            Predicate<String> isValid, String errorMessage) {
        String shown = (current == null || current.isBlank()) ? "(vacío)" : current;
        String hint = required ? "ENTER = conservar" : "ENTER = conservar, - = borrar";
        while (true) {
            System.out.print(label + " [" + shown + "] (" + hint + "): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            if (input.equals("-") && !required) {
                return null;
            }
            if (isValid.test(input)) {
                return input;
            }
            printError(errorMessage);
        }
    }

    public static String readTextKeepingCurrent(String label, String current, int maxLength, boolean required) {
        return readKeepingCurrent(label, current, required, text -> text.length() <= maxLength,
                "El texto no puede tener más de " + maxLength + " caracteres.");
    }

    public static LocalDate readDateKeepingCurrent(String label, LocalDate current) {
        while (true) {
            System.out.print(label + " [" + current + "] (ENTER = conservar, aaaa-mm-dd): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                printError("Fecha inválida. Use el formato aaaa-mm-dd.");
            }
        }
    }

    public static String readChoiceKeepingCurrent(String label, List<String> options, String current) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < options.size(); i++) {
            list.append(i + 1).append(")").append(options.get(i)).append("  ");
        }
        String shown = (current == null || current.isBlank()) ? "(vacío)" : current;
        while (true) {
            System.out.print(label + " [" + shown + "] " + list + "(ENTER = conservar): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return current;
            }
            try {
                int number = Integer.parseInt(input);
                if (number >= 1 && number <= options.size()) {
                    return options.get(number - 1);
                }
            } catch (NumberFormatException e) {
                // Falls through to the error message below.
            }
            printError("Elija un número entre 1 y " + options.size() + ".");
        }
    }
}
