package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClubMembership;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of MembershipDao (table "membresias_club"). */
public class JdbcMembershipDao implements MembershipDao {

    private static final String SELECT =
            "SELECT m.id_membresia, m.id_estudiante, e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, m.id_club, c.nombre_club, m.fecha_ingreso, m.estado_membresia, "
            + "m.fecha_retiro, m.motivo_retiro "
            + "FROM membresias_club m "
            + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante "
            + "JOIN clubes c ON c.id_club = m.id_club ";

    @Override
    public int enrollIfAvailable(int studentId, int clubId, LocalDate joinDate) {
        return JdbcSupport.inTransaction(connection -> {
            int capacity = 0;
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT capacidad_maxima FROM clubes WHERE id_club = ? FOR UPDATE")) {
                lock.setInt(1, clubId);
                try (ResultSet rs = lock.executeQuery()) {
                    if (rs.next()) {
                        capacity = rs.getInt(1);
                    }
                }
            }
            int active = 0;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'")) {
                count.setInt(1, clubId);
                try (ResultSet rs = count.executeQuery()) {
                    if (rs.next()) {
                        active = rs.getInt(1);
                    }
                }
            }
            if (active >= capacity) {
                return -1;
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia) "
                    + "VALUES (?, ?, ?, 'Activa')", Statement.RETURN_GENERATED_KEYS)) {
                insert.setInt(1, studentId);
                insert.setInt(2, clubId);
                JdbcSupport.setDate(insert, 3, joinDate);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<ClubMembership> findActive(int studentId, int clubId) {
        return JdbcSupport.queryList(SELECT + "WHERE m.id_estudiante = ? AND m.id_club = ? "
                + "AND m.estado_membresia = 'Activa'", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, clubId);
                }, this::map).stream().findFirst();
    }

    @Override
    public List<ClubMembership> findByClub(int clubId, boolean activeOnly) {
        String sql = SELECT + "WHERE m.id_club = ? " + (activeOnly ? "AND m.estado_membresia = 'Activa' " : "")
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";
        return JdbcSupport.queryList(sql, statement -> statement.setInt(1, clubId), this::map);
    }

    @Override
    public List<ClubMembership> findByStudent(int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE m.id_estudiante = ? ORDER BY m.fecha_ingreso DESC",
                statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public boolean withdraw(int membershipId, LocalDate withdrawalDate, String reason) {
        String sql = "UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, "
                + "motivo_retiro = ? WHERE id_membresia = ? AND estado_membresia = 'Activa'";
        return JdbcSupport.update(sql, statement -> {
            JdbcSupport.setDate(statement, 1, withdrawalDate);
            JdbcSupport.setNullableString(statement, 2, reason);
            statement.setInt(3, membershipId);
        }) > 0;
    }

    private ClubMembership map(ResultSet rs) throws SQLException {
        return new ClubMembership(rs.getInt("id_membresia"), rs.getInt("id_estudiante"),
                rs.getString("numero_documento"), rs.getString("nombre_estudiante"), rs.getInt("id_club"),
                rs.getString("nombre_club"), JdbcSupport.toLocalDate(rs.getDate("fecha_ingreso")),
                rs.getString("estado_membresia"), JdbcSupport.toLocalDate(rs.getDate("fecha_retiro")),
                rs.getString("motivo_retiro"));
    }
}
