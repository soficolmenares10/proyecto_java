package com.littlehorizons.erp;

import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleUtil;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Application entry point.
 *
 * PHASE 1: it only checks that the database connection works.
 * In the next phases this class will start the main menu.
 */
public class Main {

    private static final String DATABASE_NAME = "erp_little_horizons";

    public static void main(String[] args) {
        ConsoleUtil.printTitle("ERP Colegio Little Horizons");
        System.out.println("Checking the database connection...");

        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            if (!database.testConnection()) {
                ConsoleUtil.printError("Could not connect to MySQL. Check database.properties "
                        + "and make sure MySQL is running. Details in error_log.txt");
                return;
            }
            ConsoleUtil.printSuccess("Connection OK.");
            ConsoleUtil.printInfo("Tables found in " + DATABASE_NAME + ": " + countTables());
        } catch (SQLException e) {
            AppLogger.error("Error while counting tables", e);
            ConsoleUtil.printError("Could not read the database. Details in error_log.txt");
        } catch (IllegalStateException e) {
            AppLogger.error("Configuration error", e);
            ConsoleUtil.printError(e.getMessage());
        }
    }

    private static int countTables() throws SQLException {
        String sql = "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, DATABASE_NAME);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }
}