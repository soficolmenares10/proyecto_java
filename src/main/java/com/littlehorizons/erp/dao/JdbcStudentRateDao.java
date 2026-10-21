package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentRate;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of StudentRateDao (table "valores_estudiante"). */
public class JdbcStudentRateDao implements StudentRateDao {

    private static final String SELECT =
            "SELECT v.id_valor, v.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, v.id_concepto, c.nombre_concepto, v.monto_personalizado, v.motivo, "
            + "v.anio_academico, v.esta_activo "
            + "FROM valores_estudiante v "
            + "JOIN estudiantes e ON e.id_estudiante = v.id_estudiante "
            + "JOIN conceptos_pago c ON c.id_concepto = v.id_concepto ";

    @Override
    public Optional<StudentRate> findActive(int studentId, int conceptId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE v.id_estudiante = ? AND v.id_concepto = ? "
                + "AND v.anio_academico = ? AND v.esta_activo = TRUE", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, conceptId);
                    statement.setInt(3, academicYear);
                }, this::map).stream().findFirst();
    }

    @Override
    public List<StudentRate> findByStudent(int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE v.id_estudiante = ? ORDER BY v.anio_academico DESC, "
                + "c.nombre_concepto", statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public Optional<StudentRate> findById(int rateId) {
        return JdbcSupport.queryList(SELECT + "WHERE v.id_valor = ?",
                statement -> statement.setInt(1, rateId), this::map).stream().findFirst();
    }

    @Override
    public void save(int studentId, int conceptId, int academicYear, BigDecimal amount, String reason) {
        JdbcSupport.inTransaction(connection -> {
            int existingId = 0;
            try (PreparedStatement find = connection.prepareStatement(
                    "SELECT id_valor FROM valores_estudiante WHERE id_estudiante = ? AND id_concepto = ? "
                    + "AND anio_academico = ?")) {
                find.setInt(1, studentId);
                find.setInt(2, conceptId);
                find.setInt(3, academicYear);
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt(1);
                    }
                }
            }
            if (existingId > 0) {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE valores_estudiante SET monto_personalizado = ?, motivo = ?, esta_activo = TRUE "
                        + "WHERE id_valor = ?")) {
                    update.setBigDecimal(1, amount);
                    update.setString(2, reason);
                    update.setInt(3, existingId);
                    update.executeUpdate();
                }
            } else {
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO valores_estudiante (id_estudiante, id_concepto, monto_personalizado, motivo, "
                        + "anio_academico, esta_activo) VALUES (?, ?, ?, ?, ?, TRUE)")) {
                    insert.setInt(1, studentId);
                    insert.setInt(2, conceptId);
                    insert.setBigDecimal(3, amount);
                    insert.setString(4, reason);
                    insert.setInt(5, academicYear);
                    insert.executeUpdate();
                }
            }
            return null;
        });
    }

    @Override
    public boolean deactivate(int rateId) {
        return JdbcSupport.update("UPDATE valores_estudiante SET esta_activo = FALSE WHERE id_valor = ?",
                statement -> statement.setInt(1, rateId)) > 0;
    }

    private StudentRate map(ResultSet rs) throws SQLException {
        return new StudentRate(rs.getInt("id_valor"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
                rs.getInt("id_concepto"), rs.getString("nombre_concepto"), rs.getBigDecimal("monto_personalizado"),
                rs.getString("motivo"), rs.getInt("anio_academico"), rs.getBoolean("esta_activo"));
    }
}
