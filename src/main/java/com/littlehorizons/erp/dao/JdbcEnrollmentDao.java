package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of EnrollmentDao (tables "matriculas" and "grados").
 */
public class JdbcEnrollmentDao implements EnrollmentDao {

    private static final String SELECT_ENROLLMENT =
            "SELECT m.id_matricula, m.id_estudiante, m.id_grado, g.nombre_grado, g.nivel_numerico, "
            + "m.anio_academico, m.estado_matricula, m.fecha_matricula, m.fecha_retiro, m.observaciones, "
            + "e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante "
            + "FROM matriculas m "
            + "JOIN grados g ON g.id_grado = m.id_grado "
            + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante ";

    @Override
    public List<Grade> findAllGrades() {
        String sql = "SELECT id_grado, nombre_grado, nivel_numerico FROM grados ORDER BY nivel_numerico";
        return JdbcSupport.queryList(sql, statement -> { }, resultSet -> new Grade(
                resultSet.getInt("id_grado"),
                resultSet.getString("nombre_grado"),
                resultSet.getInt("nivel_numerico")));
    }

    @Override
    public int enroll(Enrollment enrollment) {
        String sql = "INSERT INTO matriculas (id_estudiante, id_grado, anio_academico, estado_matricula, "
                + "fecha_matricula, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, enrollment.getStudentId());
                statement.setInt(2, enrollment.getGradeId());
                statement.setInt(3, enrollment.getAcademicYear());
                statement.setString(4, enrollment.getStatus());
                JdbcSupport.setDate(statement, 5, enrollment.getEnrollmentDate());
                JdbcSupport.setNullableString(statement, 6, enrollment.getNotes());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<Enrollment> findByStudentAndYear(int studentId, int academicYear) {
        String sql = SELECT_ENROLLMENT + "WHERE m.id_estudiante = ? AND m.anio_academico = ?";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, academicYear);
        }, this::map).stream().findFirst();
    }

    @Override
    public List<Enrollment> findByStudentId(int studentId) {
        String sql = SELECT_ENROLLMENT + "WHERE m.id_estudiante = ? ORDER BY m.anio_academico DESC";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public List<Enrollment> findByGradeAndYear(int gradeId, int academicYear) {
        String sql = SELECT_ENROLLMENT + "WHERE m.id_grado = ? AND m.anio_academico = ? "
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setInt(1, gradeId);
            statement.setInt(2, academicYear);
        }, this::map);
    }

    @Override
    public List<Enrollment> findByYear(int academicYear) {
        String sql = SELECT_ENROLLMENT + "WHERE m.anio_academico = ? "
                + "ORDER BY g.nivel_numerico, e.primer_apellido, e.segundo_apellido, e.primer_nombre";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, academicYear), this::map);
    }

    @Override
    public boolean withdraw(int enrollmentId, LocalDate date, String notes) {
        return closeEnrollment(enrollmentId, Enrollment.WITHDRAWN, date, notes);
    }

    @Override
    public boolean graduate(int enrollmentId, LocalDate date, String notes) {
        return closeEnrollment(enrollmentId, Enrollment.GRADUATED, date, notes);
    }

    /**
     * Changes the enrollment status and the student status together (same transaction),
     * so the two tables can never disagree.
     */
    private boolean closeEnrollment(int enrollmentId, String newStatus, LocalDate date, String notes) {
        return JdbcSupport.inTransaction(connection -> {
            int changed;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ?, "
                    + "observaciones = COALESCE(?, observaciones) "
                    + "WHERE id_matricula = ? AND estado_matricula = ?")) {
                statement.setString(1, newStatus);
                JdbcSupport.setDate(statement, 2, date);
                JdbcSupport.setNullableString(statement, 3, notes);
                statement.setInt(4, enrollmentId);
                statement.setString(5, Enrollment.ACTIVE);
                changed = statement.executeUpdate();
            }
            if (changed == 0) {
                return false;
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE estudiantes SET estado = ? WHERE id_estudiante = "
                    + "(SELECT id_estudiante FROM matriculas WHERE id_matricula = ?)")) {
                statement.setString(1, newStatus);
                statement.setInt(2, enrollmentId);
                statement.executeUpdate();
            }
            return true;
        });
    }

    private Enrollment map(ResultSet rs) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(rs.getInt("id_matricula"));
        enrollment.setStudentId(rs.getInt("id_estudiante"));
        enrollment.setStudentName(rs.getString("nombre_estudiante"));
        enrollment.setStudentDocument(rs.getString("numero_documento"));
        enrollment.setGradeId(rs.getInt("id_grado"));
        enrollment.setGradeName(rs.getString("nombre_grado"));
        enrollment.setGradeLevel(rs.getInt("nivel_numerico"));
        enrollment.setAcademicYear(rs.getInt("anio_academico"));
        enrollment.setStatus(rs.getString("estado_matricula"));
        enrollment.setEnrollmentDate(JdbcSupport.toLocalDate(rs.getDate("fecha_matricula")));
        enrollment.setWithdrawalDate(JdbcSupport.toLocalDate(rs.getDate("fecha_retiro")));
        enrollment.setNotes(rs.getString("observaciones"));
        return enrollment;
    }
}
