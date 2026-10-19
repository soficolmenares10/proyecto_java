package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** JDBC implementation of AttendanceDao (table "asistencias"). */
public class JdbcAttendanceDao implements AttendanceDao {

    private static final String SELECT =
            "SELECT a.id_asistencia, a.id_estudiante, e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, a.id_materia, m.nombre_materia, a.fecha_asistencia, "
            + "a.estado_asistencia, a.observaciones, a.registrado_por "
            + "FROM asistencias a "
            + "JOIN estudiantes e ON e.id_estudiante = a.id_estudiante "
            + "JOIN materias m ON m.id_materia = a.id_materia ";

    @Override
    public void saveAll(List<Attendance> attendances) {
        JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement find = connection.prepareStatement(
                         "SELECT id_asistencia FROM asistencias WHERE id_estudiante = ? AND id_materia = ? "
                         + "AND fecha_asistencia = ?");
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, "
                         + "estado_asistencia, observaciones, registrado_por) VALUES (?, ?, ?, ?, ?, ?)");
                 PreparedStatement update = connection.prepareStatement(
                         "UPDATE asistencias SET estado_asistencia = ?, observaciones = ?, registrado_por = ? "
                         + "WHERE id_asistencia = ?")) {
                for (Attendance attendance : attendances) {
                    find.setInt(1, attendance.studentId());
                    find.setInt(2, attendance.subjectId());
                    JdbcSupport.setDate(find, 3, attendance.date());
                    int existingId = 0;
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next()) {
                            existingId = rs.getInt(1);
                        }
                    }
                    if (existingId > 0) {
                        update.setString(1, attendance.status());
                        JdbcSupport.setNullableString(update, 2, attendance.notes());
                        update.setObject(3, attendance.registeredBy());
                        update.setInt(4, existingId);
                        update.executeUpdate();
                    } else {
                        insert.setInt(1, attendance.studentId());
                        insert.setInt(2, attendance.subjectId());
                        JdbcSupport.setDate(insert, 3, attendance.date());
                        insert.setString(4, attendance.status());
                        JdbcSupport.setNullableString(insert, 5, attendance.notes());
                        insert.setObject(6, attendance.registeredBy());
                        insert.executeUpdate();
                    }
                }
            }
            return null;
        });
    }

    @Override
    public List<Attendance> findBySubjectAndDate(int subjectId, LocalDate date) {
        return JdbcSupport.queryList(SELECT + "WHERE a.id_materia = ? AND a.fecha_asistencia = ? "
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre", statement -> {
                    statement.setInt(1, subjectId);
                    JdbcSupport.setDate(statement, 2, date);
                }, this::map);
    }

    @Override
    public List<Attendance> findByStudent(int studentId, LocalDate from, LocalDate to) {
        return JdbcSupport.queryList(SELECT + "WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ? "
                + "ORDER BY a.fecha_asistencia, m.nombre_materia", statement -> {
                    statement.setInt(1, studentId);
                    JdbcSupport.setDate(statement, 2, from);
                    JdbcSupport.setDate(statement, 3, to);
                }, this::map);
    }

    @Override
    public List<Attendance> findByStatusBetween(String status, LocalDate from, LocalDate to) {
        return JdbcSupport.queryList(SELECT + "WHERE a.estado_asistencia = ? AND a.fecha_asistencia BETWEEN ? AND ?",
                statement -> {
                    statement.setString(1, status);
                    JdbcSupport.setDate(statement, 2, from);
                    JdbcSupport.setDate(statement, 3, to);
                }, this::map);
    }

    private Attendance map(ResultSet rs) throws SQLException {
        int registeredBy = rs.getInt("registrado_por");
        Integer professorId = rs.wasNull() ? null : registeredBy;
        return new Attendance(rs.getInt("id_asistencia"), rs.getInt("id_estudiante"),
                rs.getString("numero_documento"), rs.getString("nombre_estudiante"), rs.getInt("id_materia"),
                rs.getString("nombre_materia"), JdbcSupport.toLocalDate(rs.getDate("fecha_asistencia")),
                rs.getString("estado_asistencia"), rs.getString("observaciones"), professorId);
    }
}
