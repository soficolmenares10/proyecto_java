package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventParticipation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ParticipationDao (table "participantes_evento"). */
public class JdbcParticipationDao implements ParticipationDao {

    private static final String SELECT =
            "SELECT p.id_participacion, p.id_evento, ev.nombre_evento, ev.fecha_evento, p.id_estudiante, "
            + "e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, p.rol_participacion, p.posicion_obtenida, p.observaciones "
            + "FROM participantes_evento p "
            + "JOIN eventos ev ON ev.id_evento = p.id_evento "
            + "JOIN estudiantes e ON e.id_estudiante = p.id_estudiante ";

    @Override
    public int insert(EventParticipation participation) {
        String sql = "INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, "
                + "posicion_obtenida, observaciones) VALUES (?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, participation.eventId());
                statement.setInt(2, participation.studentId());
                statement.setString(3, participation.role());
                JdbcSupport.setNullableString(statement, 4, participation.position());
                JdbcSupport.setNullableString(statement, 5, participation.notes());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<EventParticipation> find(int eventId, int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE p.id_evento = ? AND p.id_estudiante = ?", statement -> {
            statement.setInt(1, eventId);
            statement.setInt(2, studentId);
        }, this::map).stream().findFirst();
    }

    @Override
    public List<EventParticipation> findByEvent(int eventId) {
        return JdbcSupport.queryList(SELECT + "WHERE p.id_evento = ?",
                statement -> statement.setInt(1, eventId), this::map);
    }

    @Override
    public List<EventParticipation> findByStudent(int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE p.id_estudiante = ? ORDER BY ev.fecha_evento DESC",
                statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public boolean isPositionTaken(int eventId, String position) {
        return JdbcSupport.queryCount(
                "SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND posicion_obtenida = ?",
                statement -> {
                    statement.setInt(1, eventId);
                    statement.setString(2, position);
                }) > 0;
    }

    @Override
    public boolean updatePosition(int participationId, String role, String position) {
        return JdbcSupport.update("UPDATE participantes_evento SET rol_participacion = ?, posicion_obtenida = ? "
                + "WHERE id_participacion = ?", statement -> {
                    statement.setString(1, role);
                    JdbcSupport.setNullableString(statement, 2, position);
                    statement.setInt(3, participationId);
                }) > 0;
    }

    @Override
    public boolean delete(int participationId) {
        return JdbcSupport.update("DELETE FROM participantes_evento WHERE id_participacion = ?",
                statement -> statement.setInt(1, participationId)) > 0;
    }

    private EventParticipation map(ResultSet rs) throws SQLException {
        return new EventParticipation(rs.getInt("id_participacion"), rs.getInt("id_evento"),
                rs.getString("nombre_evento"), JdbcSupport.toLocalDate(rs.getDate("fecha_evento")),
                rs.getInt("id_estudiante"), rs.getString("numero_documento"), rs.getString("nombre_estudiante"),
                rs.getString("rol_participacion"), rs.getString("posicion_obtenida"), rs.getString("observaciones"));
    }
}
