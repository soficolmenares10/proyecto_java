package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventSummary;
import com.littlehorizons.erp.model.InventoryRow;
import com.littlehorizons.erp.model.ParticipationSummary;
import com.littlehorizons.erp.model.StudentAverage;
import com.littlehorizons.erp.model.SubjectPerformance;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** JDBC implementation of ReportDao. */
public class JdbcReportDao implements ReportDao {

    private static final String STUDENT_NAME =
            "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido)";
    private static final String REGULAR_PERIODS = "('I', 'II', 'III', 'IV')";

    private static final String AVERAGES_FROM =
            "FROM registros_academicos r "
            + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
            + "JOIN matriculas mt ON mt.id_estudiante = r.id_estudiante AND mt.anio_academico = r.anio_academico "
            + "JOIN grados g ON g.id_grado = mt.id_grado ";

    private static final String AVERAGES_COLUMNS =
            "SELECT e.id_estudiante, e.numero_documento, " + STUDENT_NAME + " AS nombre, g.nombre_grado, "
            + "AVG(r.valor_nota) AS promedio, COUNT(*) AS notas ";

    private static final String AVERAGES_GROUP_BY =
            "GROUP BY e.id_estudiante, e.numero_documento, e.primer_nombre, e.segundo_nombre, "
            + "e.primer_apellido, e.segundo_apellido, g.nombre_grado ";

    @Override
    public List<StudentAverage> studentAverages(int academicYear) {
        String sql = AVERAGES_COLUMNS + AVERAGES_FROM
                + "WHERE r.anio_academico = ? AND r.periodo_academico IN " + REGULAR_PERIODS + " "
                + AVERAGES_GROUP_BY + "ORDER BY nombre";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, academicYear), this::mapAverage);
    }

    @Override
    public List<StudentAverage> lowStudentsOfProfessor(int professorId, int academicYear, BigDecimal limit) {
        String sql = AVERAGES_COLUMNS + AVERAGES_FROM
                + "WHERE r.anio_academico = ? AND r.periodo_academico IN " + REGULAR_PERIODS + " "
                + "AND EXISTS (SELECT 1 FROM asignaciones_profesor a WHERE a.id_profesor = ? "
                + "AND a.id_materia = r.id_materia AND a.anio_academico = r.anio_academico) "
                + AVERAGES_GROUP_BY + "HAVING AVG(r.valor_nota) < ? ORDER BY promedio, nombre";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setInt(1, academicYear);
            statement.setInt(2, professorId);
            statement.setBigDecimal(3, limit);
        }, this::mapAverage);
    }

    @Override
    public List<SubjectPerformance> subjectPerformanceOfProfessor(int professorId, int academicYear) {
        String sql = "SELECT m.nombre_materia, g.nombre_grado, COUNT(DISTINCT r.id_estudiante) AS estudiantes, "
                + "AVG(r.valor_nota) AS promedio, "
                + "COALESCE(SUM(CASE WHEN r.valor_nota < 3.0 THEN 1 ELSE 0 END), 0) AS notas_bajas "
                + "FROM asignaciones_profesor a "
                + "JOIN materias m ON m.id_materia = a.id_materia "
                + "JOIN grados g ON g.id_grado = m.id_grado "
                + "LEFT JOIN registros_academicos r ON r.id_materia = a.id_materia "
                + "AND r.anio_academico = a.anio_academico AND r.periodo_academico IN " + REGULAR_PERIODS + " "
                + "WHERE a.id_profesor = ? AND a.anio_academico = ? "
                + "GROUP BY m.id_materia, m.nombre_materia, g.nombre_grado, g.nivel_numerico "
                + "ORDER BY g.nivel_numerico, m.nombre_materia";
        return JdbcSupport.queryList(sql, statement -> {
            statement.setInt(1, professorId);
            statement.setInt(2, academicYear);
        }, rs -> new SubjectPerformance(rs.getString("nombre_materia"), rs.getString("nombre_grado"),
                rs.getLong("estudiantes"), round(rs.getBigDecimal("promedio")), rs.getLong("notas_bajas")));
    }

    @Override
    public List<ParticipationSummary> participationByStudent() {
        String sql = "SELECT e.id_estudiante, e.numero_documento, " + STUDENT_NAME + " AS nombre, "
                + "(SELECT COUNT(*) FROM membresias_club m WHERE m.id_estudiante = e.id_estudiante "
                + "AND m.estado_membresia = 'Activa') AS clubes, "
                + "(SELECT COUNT(*) FROM participantes_evento p WHERE p.id_estudiante = e.id_estudiante) AS eventos, "
                + "(SELECT COUNT(*) FROM participantes_evento p WHERE p.id_estudiante = e.id_estudiante "
                + "AND p.posicion_obtenida IN ('1er lugar', '2do lugar', '3er lugar')) AS podios "
                + "FROM estudiantes e WHERE e.estado = 'Activo' ORDER BY nombre";
        return JdbcSupport.queryList(sql, statement -> { }, rs -> new ParticipationSummary(
                rs.getInt("id_estudiante"), rs.getString("nombre"), rs.getString("numero_documento"),
                rs.getLong("clubes"), rs.getLong("eventos"), rs.getLong("podios")));
    }

    @Override
    public List<EventSummary> eventSummaries(int year) {
        String sql = "SELECT ev.nombre_evento, ev.fecha_evento, ev.tipo_evento, ev.tiene_competencia, "
                + "COUNT(p.id_participacion) AS participantes "
                + "FROM eventos ev LEFT JOIN participantes_evento p ON p.id_evento = ev.id_evento "
                + "WHERE ev.fecha_evento BETWEEN ? AND ? "
                + "GROUP BY ev.id_evento, ev.nombre_evento, ev.fecha_evento, ev.tipo_evento, ev.tiene_competencia "
                + "ORDER BY ev.fecha_evento";
        return JdbcSupport.queryList(sql, statement -> {
            JdbcSupport.setDate(statement, 1, LocalDate.of(year, 1, 1));
            JdbcSupport.setDate(statement, 2, LocalDate.of(year, 12, 31));
        }, rs -> new EventSummary(rs.getString("nombre_evento"), JdbcSupport.toLocalDate(rs.getDate("fecha_evento")),
                rs.getString("tipo_evento"), rs.getBoolean("tiene_competencia"), rs.getLong("participantes")));
    }

    @Override
    public List<InventoryRow> inventory() {
        String sql = "SELECT c.nombre_categoria, r.estado_recurso, COUNT(*) AS cantidad "
                + "FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria "
                + "GROUP BY c.id_categoria, c.nombre_categoria, r.estado_recurso "
                + "ORDER BY c.nombre_categoria, r.estado_recurso";
        return JdbcSupport.queryList(sql, statement -> { }, rs -> new InventoryRow(
                rs.getString("nombre_categoria"), rs.getString("estado_recurso"), rs.getLong("cantidad")));
    }

    private StudentAverage mapAverage(ResultSet rs) throws SQLException {
        return new StudentAverage(rs.getInt("id_estudiante"), rs.getString("nombre"), rs.getString("numero_documento"),
                rs.getString("nombre_grado"), round(rs.getBigDecimal("promedio")), rs.getLong("notas"));
    }

    private static BigDecimal round(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }
}
