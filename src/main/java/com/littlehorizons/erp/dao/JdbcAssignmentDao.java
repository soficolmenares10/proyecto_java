package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeachingAssignment;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of AssignmentDao (table "asignaciones_profesor"). */
public class JdbcAssignmentDao implements AssignmentDao {

    private static final String SELECT =
            "SELECT a.id_asignacion, a.id_profesor, "
            + "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) "
            + "AS nombre_profesor, a.id_materia, m.codigo_materia, m.nombre_materia, m.id_grado, "
            + "g.nombre_grado, a.anio_academico, a.seccion "
            + "FROM asignaciones_profesor a "
            + "JOIN profesores p ON p.id_profesor = a.id_profesor "
            + "JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN grados g ON g.id_grado = m.id_grado ";

    @Override
    public int insert(int professorId, int subjectId, int academicYear, String section) {
        String sql = "INSERT INTO asignaciones_profesor (id_profesor, id_materia, anio_academico, seccion) "
                + "VALUES (?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, professorId);
                statement.setInt(2, subjectId);
                statement.setInt(3, academicYear);
                statement.setString(4, section);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public boolean exists(int professorId, int subjectId, int academicYear, String section) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? "
                + "AND id_materia = ? AND anio_academico = ? AND seccion = ?", statement -> {
                    statement.setInt(1, professorId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    statement.setString(4, section);
                }) > 0;
    }

    @Override
    public boolean isAssigned(int professorId, int subjectId, int academicYear) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? "
                + "AND id_materia = ? AND anio_academico = ?", statement -> {
                    statement.setInt(1, professorId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                }) > 0;
    }

    @Override
    public Optional<TeachingAssignment> findById(int assignmentId) {
        return JdbcSupport.queryList(SELECT + "WHERE a.id_asignacion = ?",
                statement -> statement.setInt(1, assignmentId), this::map).stream().findFirst();
    }

    @Override
    public List<TeachingAssignment> findByProfessorAndYear(int professorId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE a.id_profesor = ? AND a.anio_academico = ? "
                + "ORDER BY g.nivel_numerico, m.nombre_materia", statement -> {
                    statement.setInt(1, professorId);
                    statement.setInt(2, academicYear);
                }, this::map);
    }

    @Override
    public List<TeachingAssignment> findBySubjectAndYear(int subjectId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE a.id_materia = ? AND a.anio_academico = ? "
                + "ORDER BY a.seccion", statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, academicYear);
                }, this::map);
    }

    @Override
    public boolean delete(int assignmentId) {
        return JdbcSupport.update("DELETE FROM asignaciones_profesor WHERE id_asignacion = ?",
                statement -> statement.setInt(1, assignmentId)) > 0;
    }

    private TeachingAssignment map(ResultSet rs) throws SQLException {
        return new TeachingAssignment(rs.getInt("id_asignacion"), rs.getInt("id_profesor"),
                rs.getString("nombre_profesor"), rs.getInt("id_materia"), rs.getString("codigo_materia"),
                rs.getString("nombre_materia"), rs.getInt("id_grado"), rs.getString("nombre_grado"),
                rs.getInt("anio_academico"), rs.getString("seccion"));
    }
}
