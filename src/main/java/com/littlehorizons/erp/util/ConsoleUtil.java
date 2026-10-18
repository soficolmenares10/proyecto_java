package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class ConsoleUtil {

    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleUtil() {
      
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
        System.out.println("✔ " + message);
    }

    public static void printError(String message) {
        System.out.println("✘ " + message);
    }

    public static void printInfo(String message) {
        System.out.println("ℹ " + message);
    }

    public static void pause() {
        System.out.print("\nPress ENTER to continue...");
        SCANNER.nextLine();
    }

    // ------------------------------------------------------------------- input


    public static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                printError("The number must be between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                printError("Please type a whole number (for example 3).");
            }
        }
    }


    public static String readRequiredText(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (!Validator.isNotBlank(input)) {
                printError("This field is required.");
            } else if (input.length() > maxLength) {
                printError("The text cannot have more than " + maxLength + " characters.");
            } else {
                return input;
            }
        }
    }


    public static String readOptionalText(String prompt, int maxLength) {
        while (true) {
            System.out.print(prompt + " (optional, ENTER to skip): ");
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                return null;
            }
            if (input.length() <= maxLength) {
                return input;
            }
            printError("The text cannot have more than " + maxLength + " characters.");
        }
    }

    public static String readEmail(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (Validator.isValidEmail(input)) {
                return input;
            }
            printError("Invalid e-mail. Example: name@example.com");
        }
    }

    public static String readDocument(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (Validator.isValidDocument(input)) {
                return input;
            }
            printError("Invalid document. Use 5 to 20 letters or digits, without dots or spaces.");
        }
    }

    public static String readPhone(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (Validator.isValidPhone(input)) {
                return input;
            }
            printError("Invalid phone. Use 7 to 20 digits (you may include + and spaces).");
        }
    }

    public static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt + " (yyyy-MM-dd): ");
            String input = SCANNER.nextLine().trim();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                printError("Invalid date. Use the format yyyy-MM-dd, for example 2026-03-15.");
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
                printError("The amount must be greater than zero.");
            } catch (NumberFormatException e) {
                printError("Invalid amount. Use digits only, for example 350000.");
            }
        }
    }

    public static BigDecimal readGrade(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim().replace(',', '.');
            try {
                BigDecimal value = new BigDecimal(input);
                if (Validator.isValidGrade(value)) {
                    return value;
                }
                printError("The grade must be between 0.0 and 5.0 with one decimal (for example 4.3).");
            } catch (NumberFormatException e) {
                printError("Invalid grade. Example: 4.3");
            }
        }
    }


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
            printError("Type s for yes or n for no.");
        }
    }
}
