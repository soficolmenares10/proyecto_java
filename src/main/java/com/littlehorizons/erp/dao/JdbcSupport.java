package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Small helper used by all the JDBC DAOs. It avoids repeating the same
 * try-with-resources / error handling code in every method.
 * Every query goes through a PreparedStatement.
 */
final class JdbcSupport {

    /** Sets the "?" parameters of a PreparedStatement. */
    @FunctionalInterface
    interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    /** Converts the current row of a ResultSet into an object. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    /** A group of SQL operations executed inside one transaction. */
    @FunctionalInterface
    interface TransactionBody<T> {
        T execute(Connection connection) throws SQLException;
    }

    private JdbcSupport() {
        // Utility class: no instances.
    }

    static <T> List<T> queryList(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(mapper.map(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw fail("Query failed: " + sql, e);
        }
    }

    /** Runs a "SELECT COUNT(*)" style query and returns the first number. */
    static int queryCount(String sql, StatementBinder binder) {
        List<Integer> result = queryList(sql, binder, resultSet -> resultSet.getInt(1));
        return result.isEmpty() ? 0 : result.get(0);
    }

    /** Runs an INSERT/UPDATE/DELETE and returns the number of affected rows. */
    static int update(String sql, StatementBinder binder) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw fail("Update failed: " + sql, e);
        }
    }

    /** Runs several operations in one transaction: all of them succeed or none does. */
    static <T> T inTransaction(TransactionBody<T> body) {
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
                T result = body.execute(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw fail("Transaction failed", e);
        }
    }

    static DataAccessException fail(String message, SQLException cause) {
        AppLogger.error(message, cause);
        return new DataAccessException(message, cause);
    }

    static void setNullableString(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    static void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DATE);
        } else {
            statement.setDate(index, Date.valueOf(value));
        }
    }

    static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    static void setTime(PreparedStatement statement, int index, LocalTime value) throws SQLException {
        statement.setTime(index, Time.valueOf(value));
    }

    static LocalTime toLocalTime(Time time) {
        return time == null ? null : time.toLocalTime();
    }
}
