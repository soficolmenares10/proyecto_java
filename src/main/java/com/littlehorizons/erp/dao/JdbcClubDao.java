package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ClubDao (table "clubes"). */
public class JdbcClubDao implements ClubDao {

    private static final String SELECT =
            "SELECT c.id_club, c.nombre_club, c.descripcion, c.id_profesor_supervisor, "
            + "CONCAT_WS(' ', p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido) "
            + "AS nombre_supervisor, c.capacidad_maxima, c.horario_reunion, c.esta_activo, "
            + "(SELECT COUNT(*) FROM membresias_club m WHERE m.id_club = c.id_club "
            + "AND m.estado_membresia = 'Activa') AS miembros_activos "
            + "FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor ";

    @Override
    public int insert(Club club) {
        String sql = "INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, "
                + "horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                bindEditableFields(statement, club);
                statement.setBoolean(6, club.isActive());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<Club> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE c.id_club = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public List<Club> findAll() {
        return JdbcSupport.queryList(SELECT + "ORDER BY c.nombre_club", statement -> { }, this::map);
    }

    @Override
    public boolean update(Club club) {
        String sql = "UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, "
                + "capacidad_maxima = ?, horario_reunion = ? WHERE id_club = ?";
        return JdbcSupport.update(sql, statement -> {
            bindEditableFields(statement, club);
            statement.setInt(6, club.getId());
        }) > 0;
    }

    @Override
    public boolean setActive(int clubId, boolean active) {
        return JdbcSupport.update("UPDATE clubes SET esta_activo = ? WHERE id_club = ?", statement -> {
            statement.setBoolean(1, active);
            statement.setInt(2, clubId);
        }) > 0;
    }

    @Override
    public boolean existsByName(String name, int excludeId) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM clubes WHERE nombre_club = ? AND id_club <> ?",
                statement -> {
                    statement.setString(1, name);
                    statement.setInt(2, excludeId);
                }) > 0;
    }

    /** Sets parameters 1 to 5 (the same fields in INSERT and UPDATE). */
    private void bindEditableFields(PreparedStatement statement, Club club) throws SQLException {
        statement.setString(1, club.getName());
        JdbcSupport.setNullableString(statement, 2, club.getDescription());
        if (club.getSupervisorId() == null) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, club.getSupervisorId());
        }
        statement.setInt(4, club.getCapacity());
        JdbcSupport.setNullableString(statement, 5, club.getMeetingSchedule());
    }

    private Club map(ResultSet rs) throws SQLException {
        Club club = new Club();
        club.setId(rs.getInt("id_club"));
        club.setName(rs.getString("nombre_club"));
        club.setDescription(rs.getString("descripcion"));
        int supervisorId = rs.getInt("id_profesor_supervisor");
        club.setSupervisorId(rs.wasNull() ? null : supervisorId);
        club.setSupervisorName(rs.getString("nombre_supervisor"));
        club.setCapacity(rs.getInt("capacidad_maxima"));
        club.setMeetingSchedule(rs.getString("horario_reunion"));
        club.setActive(rs.getBoolean("esta_activo"));
        club.setActiveMembers(rs.getInt("miembros_activos"));
        return club;
    }
}
