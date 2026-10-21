package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of PaymentDao (table "pagos"). */
public class JdbcPaymentDao implements PaymentDao {

    private static final String SELECT =
            "SELECT p.id_pago, p.id_estudiante, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, e.numero_documento, p.id_acudiente, "
            + "CONCAT_WS(' ', a.primer_nombre, a.segundo_nombre, a.primer_apellido, a.segundo_apellido) "
            + "AS nombre_acudiente, p.id_concepto, c.nombre_concepto, p.monto_pagado, p.fecha_pago, "
            + "p.metodo_pago, p.numero_recibo, p.anio_academico, p.periodo_academico, p.notas "
            + "FROM pagos p "
            + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
            + "JOIN conceptos_pago c ON c.id_concepto = p.id_concepto "
            + "LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente ";

    @Override
    public String nextReceiptNumber(int year) {
        String prefix = "REC-" + year + "-";
        int last = JdbcSupport.queryCount(
                "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(numero_recibo, '-', -1) AS UNSIGNED)), 0) "
                + "FROM pagos WHERE numero_recibo LIKE ?", statement -> statement.setString(1, prefix + "%"));
        return String.format("%s%06d", prefix, last + 1);
    }

    @Override
    public int insert(Payment payment, Integer chargeId) {
        String sql = "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago, "
                + "metodo_pago, numero_recibo, anio_academico, periodo_academico, notas) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            int paymentId = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, payment.studentId());
                if (payment.guardianId() == null) {
                    statement.setNull(2, Types.INTEGER);
                } else {
                    statement.setInt(2, payment.guardianId());
                }
                statement.setInt(3, payment.conceptId());
                statement.setBigDecimal(4, payment.amount());
                JdbcSupport.setDate(statement, 5, payment.date());
                statement.setString(6, payment.method());
                statement.setString(7, payment.receiptNumber());
                if (payment.academicYear() == null) {
                    statement.setNull(8, Types.INTEGER);
                } else {
                    statement.setInt(8, payment.academicYear());
                }
                JdbcSupport.setNullableString(statement, 9, payment.period());
                JdbcSupport.setNullableString(statement, 10, payment.notes());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        paymentId = keys.getInt(1);
                    }
                }
            }
            if (chargeId != null) {
                try (PreparedStatement markPaid = connection.prepareStatement(
                        "UPDATE cobros_pendientes SET estado = 'Pagado', id_pago = ? "
                        + "WHERE id_cobro = ? AND estado = 'Pendiente'")) {
                    markPaid.setInt(1, paymentId);
                    markPaid.setInt(2, chargeId);
                    if (markPaid.executeUpdate() == 0) {
                        // Someone else paid the charge first: cancel the whole payment.
                        throw new SQLException("The charge " + chargeId + " is not pending anymore");
                    }
                }
            }
            return paymentId;
        });
    }

    @Override
    public Optional<Payment> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE p.id_pago = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public Optional<Payment> findByReceipt(String receiptNumber) {
        return JdbcSupport.queryList(SELECT + "WHERE p.numero_recibo = ?",
                statement -> statement.setString(1, receiptNumber), this::map).stream().findFirst();
    }

    @Override
    public List<Payment> findByStudent(int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC",
                statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public List<Payment> findByGuardian(int guardianId) {
        return JdbcSupport.queryList(SELECT + "WHERE p.id_estudiante IN "
                + "(SELECT id_estudiante FROM estudiante_acudiente WHERE id_acudiente = ?) "
                + "ORDER BY p.fecha_pago DESC, p.id_pago DESC", statement -> statement.setInt(1, guardianId), this::map);
    }

    @Override
    public List<Payment> findBetween(LocalDate from, LocalDate to) {
        return JdbcSupport.queryList(SELECT + "WHERE p.fecha_pago BETWEEN ? AND ? ORDER BY p.fecha_pago, p.id_pago",
                statement -> {
                    JdbcSupport.setDate(statement, 1, from);
                    JdbcSupport.setDate(statement, 2, to);
                }, this::map);
    }

    private Payment map(ResultSet rs) throws SQLException {
        int guardianId = rs.getInt("id_acudiente");
        Integer guardian = rs.wasNull() ? null : guardianId;
        int year = rs.getInt("anio_academico");
        Integer academicYear = rs.wasNull() ? null : year;
        return new Payment(rs.getInt("id_pago"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
                rs.getString("numero_documento"), guardian, rs.getString("nombre_acudiente"),
                rs.getInt("id_concepto"), rs.getString("nombre_concepto"), rs.getBigDecimal("monto_pagado"),
                JdbcSupport.toLocalDate(rs.getDate("fecha_pago")), rs.getString("metodo_pago"),
                rs.getString("numero_recibo"), academicYear, rs.getString("periodo_academico"), rs.getString("notas"));
    }
}
