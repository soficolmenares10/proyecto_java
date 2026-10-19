package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of SubjectDao (table "materias"). */
public class JdbcSubjectDao implements SubjectDao {

    private static final String SELECT =
            "SELECT m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, m.id_grado, "
            + "g.nombre_grado, m.horas_semanales, m.esta_activa "
            + "FROM materias m JOIN grados g ON g.id_grado = m.id_grado ";
    private static final String ORDER_BY = " ORDER BY g.nivel_numerico, m.nombre_materia";

    @Override
    public int insert(Subject subject) {
        String sql = "INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, "
                + "horas_semanales, esta_activa) VALUES (?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, subject.getCode());
                statement.setString(2, subject.getName());
                JdbcSupport.setNullableString(statement, 3, subject.getDescription());
                statement.setInt(4, subject.getGradeId());
                statement.setInt(5, subject.getWeeklyHours());
                statement.setBoolean(6, subject.isActive());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<Subject> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE m.id_materia = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public Optional<Subject> findByCode(String code) {
        return JdbcSupport.queryList(SELECT + "WHERE m.codigo_materia = ?",
                statement -> statement.setString(1, code), this::map).stream().findFirst();
    }

    @Override
    public List<Subject> findAll() {
        return JdbcSupport.queryList(SELECT + ORDER_BY, statement -> { }, this::map);
    }

    @Override
    public List<Subject> findByGrade(int gradeId) {
        return JdbcSupport.queryList(SELECT + "WHERE m.id_grado = ?" + ORDER_BY,
                statement -> statement.setInt(1, gradeId), this::map);
    }

    @Override
    public boolean update(Subject subject) {
        String sql = "UPDATE materias SET nombre_materia = ?, descripcion = ?, horas_semanales = ? "
                + "WHERE id_materia = ?";
        return JdbcSupport.update(sql, statement -> {
            statement.setString(1, subject.getName());
            JdbcSupport.setNullableString(statement, 2, subject.getDescription());
            statement.setInt(3, subject.getWeeklyHours());
            statement.setInt(4, subject.getId());
        }) > 0;
    }

    @Override
    public boolean setActive(int subjectId, boolean active) {
        return JdbcSupport.update("UPDATE materias SET esta_activa = ? WHERE id_materia = ?", statement -> {
            statement.setBoolean(1, active);
            statement.setInt(2, subjectId);
        }) > 0;
    }

    @Override
    public boolean existsByCode(String code) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM materias WHERE codigo_materia = ?",
                statement -> statement.setString(1, code)) > 0;
    }

    private Subject map(ResultSet rs) throws SQLException {
        Subject subject = new Subject();
        subject.setId(rs.getInt("id_materia"));
        subject.setCode(rs.getString("codigo_materia"));
        subject.setName(rs.getString("nombre_materia"));
        subject.setDescription(rs.getString("descripcion"));
        subject.setGradeId(rs.getInt("id_grado"));
        subject.setGradeName(rs.getString("nombre_grado"));
        subject.setWeeklyHours(rs.getInt("horas_semanales"));
        subject.setActive(rs.getBoolean("esta_activa"));
        return subject;
    }
}
