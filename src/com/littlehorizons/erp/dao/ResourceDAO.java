package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** CRUD of the recursos table and reading of the categorias_recurso catalog. */
public class ResourceDAO extends BaseDAO {

    private static final String SELECT_RESOURCES = "SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, "
            + "r.descripcion, r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso, cr.id_categoria, "
            + "cr.nombre_categoria FROM recursos r JOIN categorias_recurso cr ON cr.id_categoria = r.id_categoria";
    private static final String INSERT_RESOURCE = "INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, "
            + "descripcion, fecha_adquisicion, ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_RESOURCE = "UPDATE recursos SET codigo_recurso = ?, nombre_recurso = ?, "
            + "id_categoria = ?, descripcion = ?, fecha_adquisicion = ?, ubicacion_fisica = ?, estado_recurso = ? "
            + "WHERE id_recurso = ?";
    private static final String HAS_LOANS = "SELECT EXISTS (SELECT 1 FROM prestamos_estudiante WHERE id_recurso = ?) "
            + "OR EXISTS (SELECT 1 FROM prestamos_profesor WHERE id_recurso = ?)";

    public List<ResourceCategory> findCategories() throws DataAccessException {
        return queryList("SELECT id_categoria, nombre_categoria FROM categorias_recurso ORDER BY nombre_categoria",
                statement -> { }, row -> new ResourceCategory(row.getInt("id_categoria"),
                        row.getString("nombre_categoria")), "No se pudo consultar las categorías de recursos.");
    }

    /** @return the id generated for the resource */
    public int create(Resource resource) throws DataAccessException {
        return executeInsert(INSERT_RESOURCE, statement -> fillData(statement, resource),
                "No se pudo registrar el recurso.");
    }

    public Optional<Resource> findById(int id) throws DataAccessException {
        return queryOne(SELECT_RESOURCES + " WHERE r.id_recurso = ?", statement -> statement.setInt(1, id),
                ResourceDAO::mapResource, "No se pudo buscar el recurso.");
    }

    public Optional<Resource> findByCode(String code) throws DataAccessException {
        return queryOne(SELECT_RESOURCES + " WHERE r.codigo_recurso = ?", statement -> statement.setString(1, code),
                ResourceDAO::mapResource, "No se pudo buscar el recurso.");
    }

    public List<Resource> findAll() throws DataAccessException {
        return queryList(SELECT_RESOURCES + " ORDER BY r.codigo_recurso", statement -> { },
                ResourceDAO::mapResource, "No se pudo consultar el inventario.");
    }

    public boolean update(Resource resource) throws DataAccessException {
        return executeUpdate(UPDATE_RESOURCE, statement -> {
            fillData(statement, resource);
            statement.setInt(8, resource.getId());
        }, "No se pudo actualizar el recurso.") == 1;
    }

    /** The loans would be deleted too (ON DELETE CASCADE), so a resource with loans is not deleted. */
    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM recursos WHERE id_recurso = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el recurso.") == 1;
    }

    /** @return true if the resource was ever lent, to a student or to a teacher */
    public boolean hasLoans(int id) throws DataAccessException {
        return queryOne(HAS_LOANS, statement -> {
            statement.setInt(1, id);
            statement.setInt(2, id);
        }, row -> row.getBoolean(1), "No se pudo revisar los préstamos del recurso.").orElse(false);
    }

    /** Fills parameters 1 to 7, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, Resource resource) throws SQLException {
        statement.setString(1, resource.getCode());
        statement.setString(2, resource.getName());
        statement.setInt(3, resource.getCategory().getId());
        statement.setString(4, resource.getDescription());
        statement.setObject(5, resource.getAcquisitionDate());
        statement.setString(6, resource.getLocation());
        statement.setString(7, resource.getStatus().getDatabaseValue());
    }

    private static Resource mapResource(ResultSet row) throws SQLException {
        Resource resource = new Resource();
        resource.setId(row.getInt("id_recurso"));
        resource.setCode(row.getString("codigo_recurso"));
        resource.setName(row.getString("nombre_recurso"));
        resource.setCategory(new ResourceCategory(row.getInt("id_categoria"), row.getString("nombre_categoria")));
        resource.setDescription(row.getString("descripcion"));
        resource.setAcquisitionDate(row.getObject("fecha_adquisicion", LocalDate.class));
        resource.setLocation(row.getString("ubicacion_fisica"));
        resource.setStatus(DatabaseValue.fromDatabaseValue(ResourceStatus.class, row.getString("estado_recurso")));
        return resource;
    }
}
