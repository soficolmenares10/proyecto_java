package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.JdbcGuardianDao;
import com.littlehorizons.erp.dao.JdbcStudentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.view.EnrollmentView;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.StudentView;

/**
 * Application entry point.
 *
 * It checks the database connection and then builds the objects of every layer
 * (DAO -> controller -> view) and starts the main menu.
 * Each object receives the ones it needs in its constructor (dependency injection).
 */
public class Main {

    public static void main(String[] args) {
        try {
            if (!DatabaseConnection.getInstance().testConnection()) {
                ConsoleUtil.printError("No se pudo conectar con MySQL. Revise database.properties y "
                        + "confirme que MySQL esté encendido. Detalles en error_log.txt");
                return;
            }
        } catch (IllegalStateException e) {
            AppLogger.error("Configuration error", e);
            ConsoleUtil.printError("No se pudo leer database.properties. Detalles en error_log.txt");
            return;
        }

        // Data access layer
        StudentDao studentDao = new JdbcStudentDao();
        GuardianDao guardianDao = new JdbcGuardianDao();
        EnrollmentDao enrollmentDao = new JdbcEnrollmentDao();

        // Business layer
        StudentController studentController = new StudentController(studentDao, guardianDao);
        GuardianController guardianController = new GuardianController(guardianDao, studentDao);
        EnrollmentController enrollmentController = new EnrollmentController(enrollmentDao, studentDao);

        // Presentation layer
        GuardianView guardianView = new GuardianView(guardianController, studentController);
        EnrollmentView enrollmentView = new EnrollmentView(enrollmentController, studentController);
        StudentView studentView = new StudentView(studentController, enrollmentController,
                guardianView, enrollmentView);

        new MainMenuView(studentView).run();
    }
}
