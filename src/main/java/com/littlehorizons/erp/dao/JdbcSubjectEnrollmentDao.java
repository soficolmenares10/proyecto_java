package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of SubjectEnrollmentDao (table "inscripciones_materia"). */
public class JdbcSubjectEnrollmentDao implements SubjectEnrollmentDao {

    private static final String STUDENT_NAME =
            "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido)";

    private static final String SELECT =
            "SELECT i.id_inscripcion, i.id_estudiante, e.numero_documento, " + STUDENT_NAME
            + " AS nombre_estudiante, i.id_materia, m.nombre_materia, i.anio_academico, "
            + "i.periodo_academico, i.seccion, i.fecha_inscripcion, i.estado "
            + "FROM inscripciones_materia i "
            + "JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
            + "JOIN materias m ON m.id_materia = i.id_materia ";

    @Override
    public int enrollAll(List<SubjectEnrollment> enrollments) {
        String sql = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, anio_academico, "
                + "periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            int saved = 0;
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (SubjectEnrollment enrollment : enrollments) {
                    statement.setInt(1, enrollment.studentId());
                    statement.setInt(2, enrollment.subjectId());
                    statement.setInt(3, enrollment.academicYear());
                    statement.setString(4, enrollment.period());
                    JdbcSupport.setNullableString(statement, 5, enrollment.section());
                    JdbcSupport.setDate(statement, 6, enrollment.enrollmentDate());
                    statement.setString(7, enrollment.status());
                    saved += statement.executeUpdate();
                }
            }
            return saved;
        });
    }

    @Override
    public Optional<SubjectEnrollment> find(int studentId, int subjectId, int academicYear, String period) {
        return JdbcSupport.queryList(SELECT + "WHERE i.id_estudiante = ? AND i.id_materia = ? "
                + "AND i.anio_academico = ? AND i.periodo_academico = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    statement.setString(4, period);
                }, this::map).stream().findFirst();
    }

    @Override
    public List<SubjectEnrollment> findByStudent(int studentId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE i.id_estudiante = ? AND i.anio_academico = ? "
                + "ORDER BY i.periodo_academico, m.nombre_materia", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, academicYear);
                }, this::map);
    }

    @Override
    public List<SubjectEnrollment> findBySubject(int subjectId, int academicYear, String period) {
        return JdbcSupport.queryList(SELECT + "WHERE i.id_materia = ? AND i.anio_academico = ? "
                + "AND i.periodo_academico = ? ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre",
                statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, academicYear);
                    statement.setString(3, period);
                }, this::map);
    }

    @Override
    public List<SubjectEnrollment> findDistinctStudentsBySubject(int subjectId, int academicYear) {
        String sql = "SELECT DISTINCT i.id_estudiante, e.numero_documento, " + STUDENT_NAME
                + " AS nombre_estudiante, CONCAT_WS(' ', e.primer_apellido, e.segundo_apellido, e.primer_nombre) "
                + "AS orden FROM inscripciones_materia i "
                + "JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
                + "WHERE i.id_materia = ? AND i.anio_academico = ? AND i.estado <> 'Retirada' ORDER BY orden";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
        }, rs -> new SubjectEnrollment(0, rs.getInt("id_estudiante"), rs.getString("numero_documento"),
                rs.getString("nombre_estudiante"), subjectId, null, academicYear, null, null, null, null));
    }

    @Override
    public boolean setStatus(int enrollmentId, String status) {
        return JdbcSupport.update("UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?",
                statement -> {
                    statement.setString(1, status);
                    statement.setInt(2, enrollmentId);
                }) > 0;
    }

    private SubjectEnrollment map(ResultSet rs) throws SQLException {
        return new SubjectEnrollment(rs.getInt("id_inscripcion"), rs.getInt("id_estudiante"),
                rs.getString("numero_documento"), rs.getString("nombre_estudiante"), rs.getInt("id_materia"),
                rs.getString("nombre_materia"), rs.getInt("anio_academico"), rs.getString("periodo_academico"),
                rs.getString("seccion"), JdbcSupport.toLocalDate(rs.getDate("fecha_inscripcion")),
                rs.getString("estado"));
    }
}
