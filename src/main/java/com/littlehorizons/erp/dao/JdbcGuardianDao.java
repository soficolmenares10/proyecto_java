package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of GuardianDao (tables "acudientes" and "estudiante_acudiente").
 */
public class JdbcGuardianDao implements GuardianDao {

    private static final String COLUMNS =
            "g.id_acudiente, g.tipo_documento, g.numero_documento, g.primer_nombre, g.segundo_nombre, "
            + "g.primer_apellido, g.segundo_apellido, g.parentesco, g.telefono_principal, "
            + "g.telefono_secundario, g.correo_electronico, g.direccion, g.ocupacion, "
            + "g.es_responsable_financiero";

    private static final String ORDER_BY = " ORDER BY g.primer_apellido, g.segundo_apellido, g.primer_nombre";

    private static final String INSERT_SQL =
            "INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
            + "primer_apellido, segundo_apellido, parentesco, telefono_principal, telefono_secundario, "
            + "correo_electronico, direccion, ocupacion, es_responsable_financiero) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, "
            + "segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, parentesco = ?, "
            + "telefono_principal = ?, telefono_secundario = ?, correo_electronico = ?, direccion = ?, "
            + "ocupacion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?";

    @Override
    public int insert(Guardian guardian) {
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                bindEditableFields(statement, guardian);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<Guardian> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM acudientes g WHERE g.id_acudiente = ?";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, id), this::map)
                .stream().findFirst();
    }

    @Override
    public Optional<Guardian> findByDocument(String documentNumber) {
        String sql = "SELECT " + COLUMNS + " FROM acudientes g WHERE g.numero_documento = ?";
        return JdbcSupport.queryList(sql, statement -> statement.setString(1, documentNumber), this::map)
                .stream().findFirst();
    }

    @Override
    public List<Guardian> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM acudientes g" + ORDER_BY;
        return JdbcSupport.queryList(sql, statement -> { }, this::map);
    }

    @Override
    public List<Guardian> findByStudentId(int studentId) {
        String sql = "SELECT " + COLUMNS + ", ea.es_principal FROM acudientes g "
                + "JOIN estudiante_acudiente ea ON ea.id_acudiente = g.id_acudiente "
                + "WHERE ea.id_estudiante = ? ORDER BY ea.es_principal DESC, g.primer_apellido";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, studentId), resultSet -> {
            Guardian guardian = map(resultSet);
            guardian.setPrimaryForStudent(resultSet.getBoolean("es_principal"));
            return guardian;
        });
    }

    @Override
    public boolean update(Guardian guardian) {
        return JdbcSupport.update(UPDATE_SQL, statement -> {
            bindEditableFields(statement, guardian);
            statement.setInt(14, guardian.getId());
        }) > 0;
    }

    @Override
    public boolean existsByDocument(String documentNumber, int excludeId) {
        String sql = "SELECT COUNT(*) FROM acudientes WHERE numero_documento = ? AND id_acudiente <> ?";
        return JdbcSupport.queryCount(sql, statement -> {
            statement.setString(1, documentNumber);
            statement.setInt(2, excludeId);
        }) > 0;
    }

    @Override
    public void linkToStudent(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        JdbcSupport.inTransaction(connection -> {
            if (primary) {
                try (PreparedStatement demote = connection.prepareStatement(
                        "UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?")) {
                    demote.setInt(1, studentId);
                    demote.executeUpdate();
                }
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
                    + "es_contacto_emergencia) VALUES (?, ?, ?, ?)")) {
                insert.setInt(1, studentId);
                insert.setInt(2, guardianId);
                insert.setBoolean(3, primary);
                insert.setBoolean(4, emergencyContact);
                insert.executeUpdate();
            }
            return null;
        });
    }

    @Override
    public boolean unlink(int studentId, int guardianId) {
        return JdbcSupport.inTransaction(connection -> {
            int linked = count(connection,
                    "SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ?", studentId);
            if (linked <= 1) {
                return false;
            }
            try (PreparedStatement delete = connection.prepareStatement(
                    "DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?")) {
                delete.setInt(1, studentId);
                delete.setInt(2, guardianId);
                if (delete.executeUpdate() == 0) {
                    return false;
                }
            }
            // If the removed guardian was the primary contact, promote another one.
            int primaries = count(connection,
                    "SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ? AND es_principal = TRUE",
                    studentId);
            if (primaries == 0) {
                try (PreparedStatement promote = connection.prepareStatement(
                        "UPDATE estudiante_acudiente SET es_principal = TRUE WHERE id_estudiante = ? LIMIT 1")) {
                    promote.setInt(1, studentId);
                    promote.executeUpdate();
                }
            }
            return true;
        });
    }

    private int count(java.sql.Connection connection, String sql, int parameter) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        }
    }

    /** Sets parameters 1 to 13 (the same fields in INSERT and UPDATE). */
    private void bindEditableFields(PreparedStatement statement, Guardian guardian) throws SQLException {
        statement.setString(1, guardian.getDocumentType());
        statement.setString(2, guardian.getDocumentNumber());
        statement.setString(3, guardian.getFirstName());
        JdbcSupport.setNullableString(statement, 4, guardian.getMiddleName());
        statement.setString(5, guardian.getLastName());
        JdbcSupport.setNullableString(statement, 6, guardian.getSecondLastName());
        statement.setString(7, guardian.getRelationship());
        statement.setString(8, guardian.getPrimaryPhone());
        JdbcSupport.setNullableString(statement, 9, guardian.getSecondaryPhone());
        JdbcSupport.setNullableString(statement, 10, guardian.getEmail());
        JdbcSupport.setNullableString(statement, 11, guardian.getAddress());
        JdbcSupport.setNullableString(statement, 12, guardian.getOccupation());
        statement.setBoolean(13, guardian.isFinancialResponsible());
    }

    private Guardian map(ResultSet rs) throws SQLException {
        Guardian guardian = new Guardian();
        guardian.setId(rs.getInt("id_acudiente"));
        guardian.setDocumentType(rs.getString("tipo_documento"));
        guardian.setDocumentNumber(rs.getString("numero_documento"));
        guardian.setFirstName(rs.getString("primer_nombre"));
        guardian.setMiddleName(rs.getString("segundo_nombre"));
        guardian.setLastName(rs.getString("primer_apellido"));
        guardian.setSecondLastName(rs.getString("segundo_apellido"));
        guardian.setRelationship(rs.getString("parentesco"));
        guardian.setPrimaryPhone(rs.getString("telefono_principal"));
        guardian.setSecondaryPhone(rs.getString("telefono_secundario"));
        guardian.setEmail(rs.getString("correo_electronico"));
        guardian.setAddress(rs.getString("direccion"));
        guardian.setOccupation(rs.getString("ocupacion"));
        guardian.setFinancialResponsible(rs.getBoolean("es_responsable_financiero"));
        return guardian;
    }
}
