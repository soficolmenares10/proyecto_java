package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of StudentDao (table "estudiantes").
 */
public class JdbcStudentDao implements StudentDao {

    private static final String COLUMNS =
            "e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, e.segundo_nombre, "
            + "e.primer_apellido, e.segundo_apellido, e.fecha_nacimiento, e.genero, e.direccion, "
            + "e.telefono, e.correo_electronico, e.tipo_sangre, e.alergias, e.condiciones_medicas, "
            + "e.fecha_ingreso, e.estado";

    private static final String ORDER_BY =
            " ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";

    private static final String INSERT_SQL =
            "INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
            + "primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, "
            + "correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, "
            + "segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, "
            + "genero = ?, direccion = ?, telefono = ?, correo_electronico = ?, tipo_sangre = ?, "
            + "alergias = ?, condiciones_medicas = ?, fecha_ingreso = ? WHERE id_estudiante = ?";

    private static final String LINK_SQL =
            "INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, "
            + "es_contacto_emergencia) VALUES (?, ?, TRUE, TRUE)";

    @Override
    public int insert(Student student, int primaryGuardianId) {
        return JdbcSupport.inTransaction(connection -> {
            int studentId = 0;
            try (PreparedStatement statement =
                         connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
                bindEditableFields(statement, student);
                statement.setString(16, student.getStatus());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        studentId = keys.getInt(1);
                    }
                }
            }
            try (PreparedStatement link = connection.prepareStatement(LINK_SQL)) {
                link.setInt(1, studentId);
                link.setInt(2, primaryGuardianId);
                link.executeUpdate();
            }
            return studentId;
        });
    }

    @Override
    public Optional<Student> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM estudiantes e WHERE e.id_estudiante = ?";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, id), this::map)
                .stream().findFirst();
    }

    @Override
    public Optional<Student> findByDocument(String documentNumber) {
        String sql = "SELECT " + COLUMNS + " FROM estudiantes e WHERE e.numero_documento = ?";
        return JdbcSupport.queryList(sql, statement -> statement.setString(1, documentNumber), this::map)
                .stream().findFirst();
    }

    @Override
    public List<Student> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM estudiantes e" + ORDER_BY;
        return JdbcSupport.queryList(sql, statement -> { }, this::map);
    }

    @Override
    public List<Student> search(String text) {
        String sql = "SELECT " + COLUMNS + " FROM estudiantes e "
                + "WHERE e.numero_documento LIKE ? "
                + "OR CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, "
                + "e.segundo_apellido) LIKE ?" + ORDER_BY;
        String pattern = "%" + text + "%";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
        }, this::map);
    }

    @Override
    public List<Student> findByGuardianId(int guardianId) {
        String sql = "SELECT " + COLUMNS + " FROM estudiantes e "
                + "JOIN estudiante_acudiente ea ON ea.id_estudiante = e.id_estudiante "
                + "WHERE ea.id_acudiente = ?" + ORDER_BY;
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, guardianId), this::map);
    }

    @Override
    public boolean update(Student student) {
        return JdbcSupport.update(UPDATE_SQL, statement -> {
            bindEditableFields(statement, student);
            statement.setInt(16, student.getId());
        }) > 0;
    }

    @Override
    public boolean updateStatus(int studentId, String status) {
        String sql = "UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?";
        return JdbcSupport.update(sql, statement -> {
            statement.setString(1, status);
            statement.setInt(2, studentId);
        }) > 0;
    }

    @Override
    public boolean existsByDocument(String documentNumber, int excludeId) {
        String sql = "SELECT COUNT(*) FROM estudiantes WHERE numero_documento = ? AND id_estudiante <> ?";
        return JdbcSupport.queryCount(sql, statement -> {
            statement.setString(1, documentNumber);
            statement.setInt(2, excludeId);
        }) > 0;
    }

    /** Sets parameters 1 to 15 (the same fields in INSERT and UPDATE). */
    private void bindEditableFields(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.getDocumentType());
        statement.setString(2, student.getDocumentNumber());
        statement.setString(3, student.getFirstName());
        JdbcSupport.setNullableString(statement, 4, student.getMiddleName());
        statement.setString(5, student.getLastName());
        JdbcSupport.setNullableString(statement, 6, student.getSecondLastName());
        JdbcSupport.setDate(statement, 7, student.getBirthDate());
        statement.setString(8, student.getGender());
        JdbcSupport.setNullableString(statement, 9, student.getAddress());
        JdbcSupport.setNullableString(statement, 10, student.getPhone());
        JdbcSupport.setNullableString(statement, 11, student.getEmail());
        JdbcSupport.setNullableString(statement, 12, student.getBloodType());
        JdbcSupport.setNullableString(statement, 13, student.getAllergies());
        JdbcSupport.setNullableString(statement, 14, student.getMedicalConditions());
        JdbcSupport.setDate(statement, 15, student.getAdmissionDate());
    }

    private Student map(ResultSet rs) throws SQLException {
        Student student = new Student();
        student.setId(rs.getInt("id_estudiante"));
        student.setDocumentType(rs.getString("tipo_documento"));
        student.setDocumentNumber(rs.getString("numero_documento"));
        student.setFirstName(rs.getString("primer_nombre"));
        student.setMiddleName(rs.getString("segundo_nombre"));
        student.setLastName(rs.getString("primer_apellido"));
        student.setSecondLastName(rs.getString("segundo_apellido"));
        student.setBirthDate(JdbcSupport.toLocalDate(rs.getDate("fecha_nacimiento")));
        student.setGender(rs.getString("genero"));
        student.setAddress(rs.getString("direccion"));
        student.setPhone(rs.getString("telefono"));
        student.setEmail(rs.getString("correo_electronico"));
        student.setBloodType(rs.getString("tipo_sangre"));
        student.setAllergies(rs.getString("alergias"));
        student.setMedicalConditions(rs.getString("condiciones_medicas"));
        student.setAdmissionDate(JdbcSupport.toLocalDate(rs.getDate("fecha_ingreso")));
        student.setStatus(rs.getString("estado"));
        return student;
    }
}
