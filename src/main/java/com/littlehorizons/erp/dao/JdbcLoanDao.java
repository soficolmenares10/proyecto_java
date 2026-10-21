package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.ResourceLoan;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of LoanDao (tables "prestamos_estudiante", "prestamos_profesor" and "recursos").
 */
public class JdbcLoanDao implements LoanDao {

    /** Both loan tables in one query, with the same columns. */
    private static final String ALL_LOANS =
            "SELECT 'Estudiante' AS tipo, p.id_prestamo, p.id_recurso, r.codigo_recurso, r.nombre_recurso, "
            + "p.id_estudiante AS id_prestatario, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) AS nombre, "
            + "e.numero_documento AS documento, p.fecha_prestamo, p.fecha_devolucion_esperada, "
            + "p.fecha_devolucion_real, p.estado_prestamo, p.observaciones "
            + "FROM prestamos_estudiante p JOIN recursos r ON r.id_recurso = p.id_recurso "
            + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
            + "UNION ALL "
            + "SELECT 'Profesor', p.id_prestamo, p.id_recurso, r.codigo_recurso, r.nombre_recurso, "
            + "p.id_profesor, "
            + "CONCAT_WS(' ', pr.primer_nombre, pr.segundo_nombre, pr.primer_apellido, pr.segundo_apellido), "
            + "pr.numero_documento, p.fecha_prestamo, p.fecha_devolucion_esperada, p.fecha_devolucion_real, "
            + "p.estado_prestamo, p.observaciones "
            + "FROM prestamos_profesor p JOIN recursos r ON r.id_recurso = p.id_recurso "
            + "JOIN profesores pr ON pr.id_profesor = p.id_profesor";

    private static final String SELECT = "SELECT * FROM (" + ALL_LOANS + ") l ";

    @Override
    public int lend(int resourceId, BorrowerType type, int borrowerId, LocalDate loanDate, LocalDate dueDate,
                    String notes) {
        String table = loanTable(type);
        String borrowerColumn = borrowerColumn(type);
        return JdbcSupport.inTransaction(connection -> {
            String status = null;
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT estado_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE")) {
                lock.setInt(1, resourceId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (rs.next()) {
                        status = rs.getString(1);
                    }
                }
            }
            if (!"Disponible".equals(status)) {
                return -1;
            }
            int loanId = 0;
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO " + table + " (id_recurso, " + borrowerColumn + ", fecha_prestamo, "
                    + "fecha_devolucion_esperada, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, 'Activo', ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, resourceId);
                insert.setInt(2, borrowerId);
                JdbcSupport.setDate(insert, 3, loanDate);
                JdbcSupport.setDate(insert, 4, dueDate);
                JdbcSupport.setNullableString(insert, 5, notes);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        loanId = keys.getInt(1);
                    }
                }
            }
            try (PreparedStatement mark = connection.prepareStatement(
                    "UPDATE recursos SET estado_recurso = 'Prestado' WHERE id_recurso = ?")) {
                mark.setInt(1, resourceId);
                mark.executeUpdate();
            }
            return loanId;
        });
    }

    @Override
    public boolean giveBack(BorrowerType type, int loanId, LocalDate returnDate) {
        String table = loanTable(type);
        return JdbcSupport.inTransaction(connection -> {
            int resourceId = 0;
            try (PreparedStatement find = connection.prepareStatement(
                    "SELECT id_recurso FROM " + table + " WHERE id_prestamo = ? "
                    + "AND estado_prestamo IN ('Activo', 'Vencido') FOR UPDATE")) {
                find.setInt(1, loanId);
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        resourceId = rs.getInt(1);
                    }
                }
            }
            if (resourceId == 0) {
                return false;
            }
            try (PreparedStatement close = connection.prepareStatement(
                    "UPDATE " + table + " SET estado_prestamo = 'Devuelto', fecha_devolucion_real = ? "
                    + "WHERE id_prestamo = ?")) {
                JdbcSupport.setDate(close, 1, returnDate);
                close.setInt(2, loanId);
                close.executeUpdate();
            }
            try (PreparedStatement release = connection.prepareStatement(
                    "UPDATE recursos SET estado_recurso = 'Disponible' "
                    + "WHERE id_recurso = ? AND estado_recurso = 'Prestado'")) {
                release.setInt(1, resourceId);
                release.executeUpdate();
            }
            return true;
        });
    }

    @Override
    public int markOverdue(LocalDate today) {
        return JdbcSupport.inTransaction(connection -> {
            int changed = 0;
            for (BorrowerType type : BorrowerType.values()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE " + loanTable(type) + " SET estado_prestamo = 'Vencido' "
                        + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < ?")) {
                    JdbcSupport.setDate(statement, 1, today);
                    changed += statement.executeUpdate();
                }
            }
            return changed;
        });
    }

    @Override
    public Optional<ResourceLoan> findOpenByResource(int resourceId) {
        return JdbcSupport.queryList(SELECT + "WHERE l.id_recurso = ? AND l.estado_prestamo IN ('Activo', 'Vencido')",
                statement -> statement.setInt(1, resourceId), this::map).stream().findFirst();
    }

    @Override
    public List<ResourceLoan> findOpen() {
        return JdbcSupport.queryList(SELECT + "WHERE l.estado_prestamo IN ('Activo', 'Vencido') "
                + "ORDER BY l.fecha_devolucion_esperada, l.nombre", statement -> { }, this::map);
    }

    @Override
    public List<ResourceLoan> findOverdue() {
        return JdbcSupport.queryList(SELECT + "WHERE l.estado_prestamo = 'Vencido' "
                + "ORDER BY l.fecha_devolucion_esperada, l.nombre", statement -> { }, this::map);
    }

    @Override
    public List<ResourceLoan> findByBorrower(BorrowerType type, int borrowerId) {
        return JdbcSupport.queryList(SELECT + "WHERE l.tipo = ? AND l.id_prestatario = ? "
                + "ORDER BY l.fecha_prestamo DESC, l.id_prestamo DESC", statement -> {
                    statement.setString(1, type.getLabel());
                    statement.setInt(2, borrowerId);
                }, this::map);
    }

    @Override
    public List<ResourceLoan> findByResource(int resourceId) {
        return JdbcSupport.queryList(SELECT + "WHERE l.id_recurso = ? ORDER BY l.fecha_prestamo DESC, l.id_prestamo DESC",
                statement -> statement.setInt(1, resourceId), this::map);
    }

    @Override
    public long countOpenByBorrower(BorrowerType type, int borrowerId) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM (" + ALL_LOANS + ") l WHERE l.tipo = ? "
                + "AND l.id_prestatario = ? AND l.estado_prestamo IN ('Activo', 'Vencido')", statement -> {
                    statement.setString(1, type.getLabel());
                    statement.setInt(2, borrowerId);
                });
    }

    /**
     * Table names cannot be sent as "?" parameters. They come only from the BorrowerType enum
     * (never from user text), so building the SQL with them is safe.
     */
    private static String loanTable(BorrowerType type) {
        return type == BorrowerType.STUDENT ? "prestamos_estudiante" : "prestamos_profesor";
    }

    private static String borrowerColumn(BorrowerType type) {
        return type == BorrowerType.STUDENT ? "id_estudiante" : "id_profesor";
    }

    private ResourceLoan map(ResultSet rs) throws SQLException {
        BorrowerType type = "Estudiante".equals(rs.getString("tipo")) ? BorrowerType.STUDENT : BorrowerType.PROFESSOR;
        return new ResourceLoan(rs.getInt("id_prestamo"), rs.getInt("id_recurso"), rs.getString("codigo_recurso"),
                rs.getString("nombre_recurso"), type, rs.getInt("id_prestatario"), rs.getString("nombre"),
                rs.getString("documento"), JdbcSupport.toLocalDate(rs.getDate("fecha_prestamo")),
                JdbcSupport.toLocalDate(rs.getDate("fecha_devolucion_esperada")),
                JdbcSupport.toLocalDate(rs.getDate("fecha_devolucion_real")), rs.getString("estado_prestamo"),
                rs.getString("observaciones"));
    }
}
