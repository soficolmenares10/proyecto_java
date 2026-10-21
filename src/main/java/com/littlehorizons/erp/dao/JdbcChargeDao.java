package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PendingCharge;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ChargeDao (table "cobros_pendientes"). */
public class JdbcChargeDao implements ChargeDao {

    private static final String SELECT =
            "SELECT c.id_cobro, c.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, e.numero_documento, c.id_concepto, cp.nombre_concepto, c.anio_academico, "
            + "c.periodo_facturado, c.monto_esperado, c.fecha_vencimiento, c.estado, c.id_pago "
            + "FROM cobros_pendientes c "
            + "JOIN estudiantes e ON e.id_estudiante = c.id_estudiante "
            + "JOIN conceptos_pago cp ON cp.id_concepto = c.id_concepto ";

    @Override
    public int insert(PendingCharge charge) {
        String sql = "INSERT INTO cobros_pendientes (id_estudiante, id_concepto, anio_academico, periodo_facturado, "
                + "monto_esperado, fecha_vencimiento, estado) VALUES (?, ?, ?, ?, ?, ?, 'Pendiente')";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, charge.studentId());
                statement.setInt(2, charge.conceptId());
                statement.setInt(3, charge.academicYear());
                statement.setString(4, charge.description());
                statement.setBigDecimal(5, charge.expectedAmount());
                JdbcSupport.setDate(statement, 6, charge.dueDate());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<PendingCharge> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE c.id_cobro = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public List<PendingCharge> findPendingByStudent(int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE c.id_estudiante = ? AND c.estado = 'Pendiente' "
                + "ORDER BY c.fecha_vencimiento", statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public List<PendingCharge> findPendingDueBefore(LocalDate date) {
        return JdbcSupport.queryList(SELECT + "WHERE c.estado = 'Pendiente' AND c.fecha_vencimiento < ? "
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre, c.fecha_vencimiento",
                statement -> JdbcSupport.setDate(statement, 1, date), this::map);
    }

    @Override
    public boolean exists(int studentId, int conceptId, int academicYear, String description) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM cobros_pendientes WHERE id_estudiante = ? "
                + "AND id_concepto = ? AND anio_academico = ? AND periodo_facturado = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, conceptId);
                    statement.setInt(3, academicYear);
                    statement.setString(4, description);
                }) > 0;
    }

    @Override
    public boolean deletePending(int chargeId) {
        return JdbcSupport.update("DELETE FROM cobros_pendientes WHERE id_cobro = ? AND estado = 'Pendiente'",
                statement -> statement.setInt(1, chargeId)) > 0;
    }

    private PendingCharge map(ResultSet rs) throws SQLException {
        int paymentId = rs.getInt("id_pago");
        Integer payment = rs.wasNull() ? null : paymentId;
        return new PendingCharge(rs.getInt("id_cobro"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
                rs.getString("numero_documento"), rs.getInt("id_concepto"), rs.getString("nombre_concepto"),
                rs.getInt("anio_academico"), rs.getString("periodo_facturado"), rs.getBigDecimal("monto_esperado"),
                JdbcSupport.toLocalDate(rs.getDate("fecha_vencimiento")), rs.getString("estado"), payment);
    }
}
