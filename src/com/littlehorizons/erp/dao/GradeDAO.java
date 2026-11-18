package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Reads the grados catalog (it is loaded by database.sql and does not change from the program). */
public class GradeDAO extends BaseDAO {

    private static final String SELECT_GRADES = "SELECT id_grado, nombre_grado, nivel_numerico FROM grados";

    public List<Grade> findAll() throws DataAccessException {
        return queryList(SELECT_GRADES + " ORDER BY nivel_numerico", statement -> { },
                GradeDAO::mapGrade, "No se pudo consultar los grados.");
    }

    public Optional<Grade> findById(int id) throws DataAccessException {
        return queryOne(SELECT_GRADES + " WHERE id_grado = ?", statement -> statement.setInt(1, id),
                GradeDAO::mapGrade, "No se pudo buscar el grado.");
    }

    static Grade mapGrade(ResultSet row) throws SQLException {
        return new Grade(row.getInt("id_grado"), row.getString("nombre_grado"), row.getInt("nivel_numerico"));
    }
}
