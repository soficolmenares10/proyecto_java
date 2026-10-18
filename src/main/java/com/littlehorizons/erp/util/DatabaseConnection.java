package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton that reads the database settings once (from database.properties)
 * and hands out JDBC connections.
 *
 * Why a Singleton? There must be only ONE place that knows the URL, user and
 * password. Every DAO asks this class for a connection.
 *
 * Each call to getConnection() returns a NEW connection, so callers can close
 * it safely with try-with-resources.
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "database.properties";
    private static DatabaseConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        Properties properties = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IllegalStateException("File " + CONFIG_FILE
                        + " was not found in src/main/resources");
            }
            properties.load(input);
        } catch (IOException e) {
            AppLogger.error("Could not read " + CONFIG_FILE, e);
            throw new IllegalStateException("Could not read " + CONFIG_FILE, e);
        }

        String host = properties.getProperty("db.host", "localhost");
        String port = properties.getProperty("db.port", "3306");
        String name = properties.getProperty("db.name", "erp_little_horizons");

        this.url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useUnicode=true&characterEncoding=UTF-8"
                + "&serverTimezone=America/Bogota&useSSL=false&allowPublicKeyRetrieval=true";
        this.user = properties.getProperty("db.user", "root");
        this.password = properties.getProperty("db.password", "");
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * @return a new open connection. The caller must close it (try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * @return true if a connection can be opened with the current settings.
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException e) {
            AppLogger.error("Database connection test failed", e);
            return false;
        }
    }
}
