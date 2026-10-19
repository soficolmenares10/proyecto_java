package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Professor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ProfessorDao (table "profesores"). */
public class JdbcProfessorDao implements ProfessorDao {

    private static final String COLUMNS =
            "p.id_profesor, p.tipo_documento, p.numero_documento, p.primer_nombre, p.segundo_nombre, "
            + "p.primer_apellido, p.segundo_apellido, p.correo_electronico, p.telefono, "
            + "p.especializacion, p.fecha_contratacion, p.estado";
    private static final String ORDER_BY = " ORDER BY p.primer_apellido, p.segundo_apellido, p.primer_nombre";

    private static final String INSERT_SQL =
            "INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
            + "primer_apellido, segundo_apellido, correo_electronico, telefono, especializacion, "
            + "fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE profesores SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, "
            + "segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, correo_electronico = ?, "
            + "telefono = ?, especializacion = ?, fecha_contratacion = ? WHERE id_profesor = ?";

    @Override
    public int insert(Professor professor) {
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                bindEditableFields(statement, professor);
                statement.setString(11, professor.getStatus());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<Professor> findById(int id) {
        return JdbcSupport.queryList("SELECT " + COLUMNS + " FROM profesores p WHERE p.id_profesor = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public Optional<Professor> findByDocument(String documentNumber) {
        return JdbcSupport.queryList("SELECT " + COLUMNS + " FROM profesores p WHERE p.numero_documento = ?",
                statement -> statement.setString(1, documentNumber), this::map).stream().findFirst();
    }

    @Override
    public List<Professor> findAll() {
        return JdbcSupport.queryList("SELECT " + COLUMNS + " FROM profesores p" + ORDER_BY,
                statement -> { }, this::map);
    }

    @Override
    public List<Professor> search(String text) {
        String sql = "SELECT " + COLUMNS + " FROM profesores p WHERE p.numero_documento LIKE ? "
                + "OR CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) LIKE ? "
                + "OR p.especializacion LIKE ?" + ORDER_BY;
        String pattern = "%" + text + "%";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
        }, this::map);
    }

    @Override
    public boolean update(Professor professor) {
        return JdbcSupport.update(UPDATE_SQL, statement -> {
            bindEditableFields(statement, professor);
            statement.setInt(11, professor.getId());
        }) > 0;
    }

    @Override
    public boolean updateStatus(int professorId, String status) {
        return JdbcSupport.update("UPDATE profesores SET estado = ? WHERE id_profesor = ?", statement -> {
            statement.setString(1, status);
            statement.setInt(2, professorId);
        }) > 0;
    }

    @Override
    public boolean existsByDocument(String documentNumber, int excludeId) {
        return JdbcSupport.queryCount(
                "SELECT COUNT(*) FROM profesores WHERE numero_documento = ? AND id_profesor <> ?", statement -> {
                    statement.setString(1, documentNumber);
                    statement.setInt(2, excludeId);
                }) > 0;
    }

    @Override
    public boolean existsByEmail(String email, int excludeId) {
        return JdbcSupport.queryCount(
                "SELECT COUNT(*) FROM profesores WHERE correo_electronico = ? AND id_profesor <> ?", statement -> {
                    statement.setString(1, email);
                    statement.setInt(2, excludeId);
                }) > 0;
    }

    /** Sets parameters 1 to 10 (the same fields in INSERT and UPDATE). */
    private void bindEditableFields(PreparedStatement statement, Professor professor) throws SQLException {
        statement.setString(1, professor.getDocumentType());
        statement.setString(2, professor.getDocumentNumber());
        statement.setString(3, professor.getFirstName());
        JdbcSupport.setNullableString(statement, 4, professor.getMiddleName());
        statement.setString(5, professor.getLastName());
        JdbcSupport.setNullableString(statement, 6, professor.getSecondLastName());
        statement.setString(7, professor.getEmail());
        JdbcSupport.setNullableString(statement, 8, professor.getPhone());
        JdbcSupport.setNullableString(statement, 9, professor.getSpecialization());
        JdbcSupport.setDate(statement, 10, professor.getHireDate());
    }

    private Professor map(ResultSet rs) throws SQLException {
        Professor professor = new Professor();
        professor.setId(rs.getInt("id_profesor"));
        professor.setDocumentType(rs.getString("tipo_documento"));
        professor.setDocumentNumber(rs.getString("numero_documento"));
        professor.setFirstName(rs.getString("primer_nombre"));
        professor.setMiddleName(rs.getString("segundo_nombre"));
        professor.setLastName(rs.getString("primer_apellido"));
        professor.setSecondLastName(rs.getString("segundo_apellido"));
        professor.setEmail(rs.getString("correo_electronico"));
        professor.setPhone(rs.getString("telefono"));
        professor.setSpecialization(rs.getString("especializacion"));
        professor.setHireDate(JdbcSupport.toLocalDate(rs.getDate("fecha_contratacion")));
        professor.setStatus(rs.getString("estado"));
        return professor;
    }
}
