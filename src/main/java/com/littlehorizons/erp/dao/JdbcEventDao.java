package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of EventDao (table "eventos"). */
public class JdbcEventDao implements EventDao {

    private static final String SELECT =
            "SELECT ev.id_evento, ev.nombre_evento, ev.descripcion, ev.tipo_evento, ev.fecha_evento, ev.lugar, "
            + "ev.tiene_competencia, ev.id_organizador, "
            + "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) "
            + "AS nombre_organizador, ev.esta_activo "
            + "FROM eventos ev LEFT JOIN profesores p ON p.id_profesor = ev.id_organizador ";

    @Override
    public int insert(SchoolEvent event) {
        String sql = "INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, "
                + "id_organizador, tiene_competencia, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindEditableFields(statement, event);
                statement.setBoolean(7, event.isCompetition());
                statement.setBoolean(8, event.isActive());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<SchoolEvent> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE ev.id_evento = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public List<SchoolEvent> findByYear(int year) {
        return JdbcSupport.queryList(SELECT + "WHERE ev.fecha_evento BETWEEN ? AND ? ORDER BY ev.fecha_evento",
                statement -> {
                    JdbcSupport.setDate(statement, 1, LocalDate.of(year, 1, 1));
                    JdbcSupport.setDate(statement, 2, LocalDate.of(year, 12, 31));
                }, this::map);
    }

    @Override
    public boolean update(SchoolEvent event) {
        String sql = "UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, fecha_evento = ?, "
                + "lugar = ?, id_organizador = ? WHERE id_evento = ?";
        return JdbcSupport.update(sql, statement -> {
            bindEditableFields(statement, event);
            statement.setInt(7, event.getId());
        }) > 0;
    }

    @Override
    public boolean setActive(int eventId, boolean active) {
        return JdbcSupport.update("UPDATE eventos SET esta_activo = ? WHERE id_evento = ?", statement -> {
            statement.setBoolean(1, active);
            statement.setInt(2, eventId);
        }) > 0;
    }

    /** Sets parameters 1 to 6 (the same fields in INSERT and UPDATE). */
    private void bindEditableFields(PreparedStatement statement, SchoolEvent event) throws SQLException {
        statement.setString(1, event.getName());
        JdbcSupport.setNullableString(statement, 2, event.getDescription());
        statement.setString(3, event.getType());
        JdbcSupport.setDate(statement, 4, event.getDate());
        JdbcSupport.setNullableString(statement, 5, event.getPlace());
        if (event.getOrganizerId() == null) {
            statement.setNull(6, Types.INTEGER);
        } else {
            statement.setInt(6, event.getOrganizerId());
        }
    }

    private SchoolEvent map(ResultSet rs) throws SQLException {
        SchoolEvent event = new SchoolEvent();
        event.setId(rs.getInt("id_evento"));
        event.setName(rs.getString("nombre_evento"));
        event.setDescription(rs.getString("descripcion"));
        event.setType(rs.getString("tipo_evento"));
        event.setDate(JdbcSupport.toLocalDate(rs.getDate("fecha_evento")));
        event.setPlace(rs.getString("lugar"));
        event.setCompetition(rs.getBoolean("tiene_competencia"));
        int organizerId = rs.getInt("id_organizador");
        event.setOrganizerId(rs.wasNull() ? null : organizerId);
        event.setOrganizerName(rs.getString("nombre_organizador"));
        event.setActive(rs.getBoolean("esta_activo"));
        return event;
    }
}
