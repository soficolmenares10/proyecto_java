package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ConceptDao (table "conceptos_pago"). */
public class JdbcConceptDao implements ConceptDao {

    private static final String SELECT =
            "SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente, "
            + "porcentaje_iva, esta_activo FROM conceptos_pago ";

    @Override
    public int insert(PaymentConcept concept) {
        String sql = "INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, "
                + "es_recurrente, porcentaje_iva, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, concept.getCode());
                statement.setString(2, concept.getName());
                JdbcSupport.setNullableString(statement, 3, concept.getDescription());
                statement.setBigDecimal(4, concept.getBaseAmount());
                statement.setBoolean(5, concept.isRecurring());
                statement.setBigDecimal(6, concept.getVatPercent());
                statement.setBoolean(7, concept.isActive());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<PaymentConcept> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE id_concepto = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public List<PaymentConcept> findAll() {
        return JdbcSupport.queryList(SELECT + "ORDER BY nombre_concepto", statement -> { }, this::map);
    }

    @Override
    public boolean update(PaymentConcept concept) {
        String sql = "UPDATE conceptos_pago SET nombre_concepto = ?, descripcion = ?, monto_base = ?, "
                + "es_recurrente = ?, porcentaje_iva = ? WHERE id_concepto = ?";
        return JdbcSupport.update(sql, statement -> {
            statement.setString(1, concept.getName());
            JdbcSupport.setNullableString(statement, 2, concept.getDescription());
            statement.setBigDecimal(3, concept.getBaseAmount());
            statement.setBoolean(4, concept.isRecurring());
            statement.setBigDecimal(5, concept.getVatPercent());
            statement.setInt(6, concept.getId());
        }) > 0;
    }

    @Override
    public boolean setActive(int conceptId, boolean active) {
        return JdbcSupport.update("UPDATE conceptos_pago SET esta_activo = ? WHERE id_concepto = ?", statement -> {
            statement.setBoolean(1, active);
            statement.setInt(2, conceptId);
        }) > 0;
    }

    @Override
    public boolean existsByCode(String code) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM conceptos_pago WHERE codigo_concepto = ?",
                statement -> statement.setString(1, code)) > 0;
    }

    private PaymentConcept map(ResultSet rs) throws SQLException {
        PaymentConcept concept = new PaymentConcept();
        concept.setId(rs.getInt("id_concepto"));
        concept.setCode(rs.getString("codigo_concepto"));
        concept.setName(rs.getString("nombre_concepto"));
        concept.setDescription(rs.getString("descripcion"));
        concept.setBaseAmount(rs.getBigDecimal("monto_base"));
        concept.setRecurring(rs.getBoolean("es_recurrente"));
        concept.setVatPercent(rs.getBigDecimal("porcentaje_iva"));
        concept.setActive(rs.getBoolean("esta_activo"));
        return concept;
    }
}
