package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.BusinessException;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtil;
import java.util.function.Supplier;

/**
 * Common behaviour of all the views: run an action and show a friendly message
 * if something fails, instead of letting the program crash.
 */
public abstract class BaseView {

    /**
     * Runs the action. If it fails, prints the error and returns null.
     */
    protected <T> T callSafely(Supplier<T> action) {
        try {
            return action.get();
        } catch (BusinessException e) {
            ConsoleUtil.printError(e.getMessage());
        } catch (DataAccessException e) {
            ConsoleUtil.printError("Ocurrió un error al acceder a la base de datos. "
                    + "Revise el archivo error_log.txt.");
        } catch (RuntimeException e) {
            AppLogger.error("Unexpected error in the console view", e);
            ConsoleUtil.printError("Ocurrió un error inesperado. Revise el archivo error_log.txt.");
        }
        return null;
    }

    protected void runSafely(Runnable action) {
        callSafely(() -> {
            action.run();
            return Boolean.TRUE;
        });
    }

    /** Runs the action safely and waits for ENTER, so the user can read the result. */
    protected void runAndPause(Runnable action) {
        runSafely(action);
        ConsoleUtil.pause();
    }

    protected static String show(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    protected static void printLine(String label, String value) {
        System.out.printf("  %-26s %s%n", label + ":", show(value));
    }
}
