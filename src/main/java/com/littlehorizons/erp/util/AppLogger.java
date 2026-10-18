package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple file logger. Every message is appended to error_log.txt
 * (created in the folder from which the program is run).
 *
 * Single Responsibility: this class only knows how to write log lines.
 */
public final class AppLogger {

    private static final Path LOG_FILE = Paths.get("error_log.txt");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {
        // Utility class: no instances.
    }

    public static void info(String message) {
        write("INFO", message, null);
    }

    public static void warning(String message) {
        write("WARNING", message, null);
    }

    public static void error(String message, Throwable throwable) {
        write("ERROR", message, throwable);
    }

    private static synchronized void write(String level, String message, Throwable throwable) {
        StringBuilder line = new StringBuilder();
        line.append('[').append(LocalDateTime.now().format(TIMESTAMP_FORMAT)).append("] ")
            .append(level).append(" - ").append(message).append(System.lineSeparator());

        if (throwable != null) {
            StringWriter stackTrace = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stackTrace));
            line.append(stackTrace);
        }

        try {
            Files.writeString(LOG_FILE, line.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // If the log itself fails we can only report it on the console.
            System.err.println("Could not write to log file: " + e.getMessage());
        }
    }
}
