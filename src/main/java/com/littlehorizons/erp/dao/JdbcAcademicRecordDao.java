package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of AcademicRecordDao (table "registros_academicos"). */
public class JdbcAcademicRecordDao implements AcademicRecordDao {

    private static final String SELECT =
            "SELECT r.id_registro, r.id_estudiante, e.numero_documento, "
            + "CONCAT_WS(' ', e.primer_nombre, e.segundo_nombre, e.primer_apellido, e.segundo_apellido) "
            + "AS nombre_estudiante, r.id_materia, m.nombre_materia, r.id_profesor, r.anio_academico, "
            + "r.periodo_academico, r.valor_nota, r.observaciones, r.fecha_registro "
            + "FROM registros_academicos r "
            + "JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
            + "JOIN materias m ON m.id_materia = r.id_materia ";

    @Override
    public int insert(AcademicRecord record) {
        String sql = "INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, "
                + "anio_academico, periodo_academico, valor_nota, observaciones, fecha_registro) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setInt(1, record.studentId());
                statement.setInt(2, record.subjectId());
                statement.setInt(3, record.professorId());
                statement.setInt(4, record.academicYear());
                statement.setString(5, record.period());
                statement.setBigDecimal(6, record.value());
                JdbcSupport.setNullableString(statement, 7, record.notes());
                JdbcSupport.setDate(statement, 8, record.recordDate());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public boolean update(int recordId, BigDecimal value, String notes, int professorId, LocalDate date) {
        String sql = "UPDATE registros_academicos SET valor_nota = ?, observaciones = ?, id_profesor = ?, "
                + "fecha_registro = ? WHERE id_registro = ?";
        return JdbcSupport.update(sql, statement -> {
            statement.setBigDecimal(1, value);
            JdbcSupport.setNullableString(statement, 2, notes);
            statement.setInt(3, professorId);
            JdbcSupport.setDate(statement, 4, date);
            statement.setInt(5, recordId);
        }) > 0;
    }

    @Override
    public Optional<AcademicRecord> find(int studentId, int subjectId, int academicYear, String period) {
        return JdbcSupport.queryList(SELECT + "WHERE r.id_estudiante = ? AND r.id_materia = ? "
                + "AND r.anio_academico = ? AND r.periodo_academico = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, academicYear);
                    statement.setString(4, period);
                }, this::map).stream().findFirst();
    }

    @Override
    public List<AcademicRecord> findByStudent(int studentId) {
        return JdbcSupport.queryList(SELECT + "WHERE r.id_estudiante = ?",
                statement -> statement.setInt(1, studentId), this::map);
    }

    @Override
    public List<AcademicRecord> findByStudentAndYear(int studentId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE r.id_estudiante = ? AND r.anio_academico = ?", statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, academicYear);
        }, this::map);
    }

    @Override
    public List<AcademicRecord> findBySubjectAndYear(int subjectId, int academicYear) {
        return JdbcSupport.queryList(SELECT + "WHERE r.id_materia = ? AND r.anio_academico = ?", statement -> {
            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
        }, this::map);
    }

    private AcademicRecord map(ResultSet rs) throws SQLException {
        return new AcademicRecord(rs.getInt("id_registro"), rs.getInt("id_estudiante"),
                rs.getString("numero_documento"), rs.getString("nombre_estudiante"), rs.getInt("id_materia"),
                rs.getString("nombre_materia"), rs.getInt("id_profesor"), rs.getInt("anio_academico"),
                rs.getString("periodo_academico"), rs.getBigDecimal("valor_nota"), rs.getString("observaciones"),
                JdbcSupport.toLocalDate(rs.getDate("fecha_registro")));
    }
}
