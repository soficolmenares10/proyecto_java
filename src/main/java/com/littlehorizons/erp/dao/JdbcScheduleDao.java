package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/** JDBC implementation of ScheduleDao (table "horarios_clase"). */
public class JdbcScheduleDao implements ScheduleDao {

    private static final String SELECT =
            "SELECT h.id_horario, h.id_asignacion, a.id_profesor, h.dia_semana, h.hora_inicio, h.hora_fin, "
            + "h.aula, a.anio_academico, a.seccion, m.nombre_materia, m.id_grado, g.nombre_grado "
            + "FROM horarios_clase h "
            + "JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion "
            + "JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN grados g ON g.id_grado = m.id_grado ";

    @Override
    public int insert(ClassSchedule schedule) {
        String sql = "INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin, aula) "
                + "VALUES (?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, schedule.assignmentId());
                statement.setString(2, schedule.day());
                JdbcSupport.setTime(statement, 3, schedule.startTime());
                JdbcSupport.setTime(statement, 4, schedule.endTime());
                JdbcSupport.setNullableString(statement, 5, schedule.room());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public List<ClassSchedule> findByProfessorAndYear(int professorId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE a.id_profesor = ? AND a.anio_academico = ?", statement -> {
            statement.setInt(1, professorId);
            statement.setInt(2, academicYear);
        }, this::map);
    }

    @Override
    public List<ClassSchedule> findByGroupAndYear(int gradeId, String section, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE m.id_grado = ? AND a.seccion = ? AND a.anio_academico = ?",
                statement -> {
                    statement.setInt(1, gradeId);
                    statement.setString(2, section);
                    statement.setInt(3, academicYear);
                }, this::map);
    }

    @Override
    public boolean delete(int scheduleId) {
        return JdbcSupport.update("DELETE FROM horarios_clase WHERE id_horario = ?",
                statement -> statement.setInt(1, scheduleId)) > 0;
    }

    private ClassSchedule map(ResultSet rs) throws SQLException {
        return new ClassSchedule(rs.getInt("id_horario"), rs.getInt("id_asignacion"), rs.getInt("id_profesor"),
                rs.getString("dia_semana"), JdbcSupport.toLocalTime(rs.getTime("hora_inicio")),
                JdbcSupport.toLocalTime(rs.getTime("hora_fin")), rs.getString("aula"),
                rs.getInt("anio_academico"), rs.getString("seccion"), rs.getString("nombre_materia"),
                rs.getInt("id_grado"), rs.getString("nombre_grado"));
    }
}
