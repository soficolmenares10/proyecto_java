package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of ResourceDao (tables "recursos" and "categorias_recurso"). */
public class JdbcResourceDao implements ResourceDao {

    private static final String SELECT =
            "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.id_categoria, c.nombre_categoria, "
            + "r.descripcion, r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso "
            + "FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria ";
    private static final String ORDER_BY = " ORDER BY c.nombre_categoria, r.codigo_recurso";

    @Override
    public int insert(Resource resource) {
        String sql = "INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, "
                + "fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)";
        return JdbcSupport.inTransaction(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, resource.getCode());
                statement.setString(2, resource.getName());
                statement.setInt(3, resource.getCategoryId());
                JdbcSupport.setNullableString(statement, 4, resource.getDescription());
                JdbcSupport.setDate(statement, 5, resource.getAcquisitionDate());
                JdbcSupport.setNullableString(statement, 6, resource.getLocation());
                statement.setString(7, resource.getStatus());
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        });
    }

    @Override
    public Optional<Resource> findById(int id) {
        return JdbcSupport.queryList(SELECT + "WHERE r.id_recurso = ?",
                statement -> statement.setInt(1, id), this::map).stream().findFirst();
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return JdbcSupport.queryList(SELECT + "WHERE r.codigo_recurso = ?",
                statement -> statement.setString(1, code), this::map).stream().findFirst();
    }

    @Override
    public List<Resource> findAll() {
        return JdbcSupport.queryList(SELECT + ORDER_BY, statement -> { }, this::map);
    }

    @Override
    public List<Resource> search(String text) {
        String pattern = "%" + text + "%";
        return JdbcSupport.queryList(SELECT + "WHERE r.codigo_recurso LIKE ? OR r.nombre_recurso LIKE ? "
                + "OR r.ubicacion_fisica LIKE ?" + ORDER_BY, statement -> {
                    statement.setString(1, pattern);
                    statement.setString(2, pattern);
                    statement.setString(3, pattern);
                }, this::map);
    }

    @Override
    public List<ResourceCategory> findCategories() {
        return JdbcSupport.queryList("SELECT id_categoria, nombre_categoria FROM categorias_recurso "
                + "ORDER BY nombre_categoria", statement -> { },
                rs -> new ResourceCategory(rs.getInt("id_categoria"), rs.getString("nombre_categoria")));
    }

    @Override
    public boolean update(Resource resource) {
        String sql = "UPDATE recursos SET nombre_recurso = ?, id_categoria = ?, descripcion = ?, "
                + "fecha_adquisicion = ?, ubicacion_fisica = ? WHERE id_recurso = ?";
        return JdbcSupport.update(sql, statement -> {
            statement.setString(1, resource.getName());
            statement.setInt(2, resource.getCategoryId());
            JdbcSupport.setNullableString(statement, 3, resource.getDescription());
            JdbcSupport.setDate(statement, 4, resource.getAcquisitionDate());
            JdbcSupport.setNullableString(statement, 5, resource.getLocation());
            statement.setInt(6, resource.getId());
        }) > 0;
    }

    @Override
    public boolean updateStatus(int resourceId, String status) {
        return JdbcSupport.update("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", statement -> {
            statement.setString(1, status);
            statement.setInt(2, resourceId);
        }) > 0;
    }

    @Override
    public boolean existsByCode(String code) {
        return JdbcSupport.queryCount("SELECT COUNT(*) FROM recursos WHERE codigo_recurso = ?",
                statement -> statement.setString(1, code)) > 0;
    }

    private Resource map(ResultSet rs) throws SQLException {
        Resource resource = new Resource();
        resource.setId(rs.getInt("id_recurso"));
        resource.setCode(rs.getString("codigo_recurso"));
        resource.setName(rs.getString("nombre_recurso"));
        resource.setCategoryId(rs.getInt("id_categoria"));
        resource.setCategoryName(rs.getString("nombre_categoria"));
        resource.setDescription(rs.getString("descripcion"));
        resource.setAcquisitionDate(JdbcSupport.toLocalDate(rs.getDate("fecha_adquisicion")));
        resource.setLocation(rs.getString("ubicacion_fisica"));
        resource.setStatus(rs.getString("estado_recurso"));
        return resource;
    }
}
