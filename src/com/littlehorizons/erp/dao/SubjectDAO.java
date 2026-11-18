package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** CRUD of the materias table; every query brings the grade of the subject. */
public class SubjectDAO extends BaseDAO {

    /** Columns of the subject and its grade; other DAOs use them in their JOIN (aliases m and g). */
    static final String COLUMNS = "m.id_materia, m.codigo_materia, m.nombre_materia, m.descripcion, "
            + "m.horas_semanales, m.esta_activa, g.id_grado, g.nombre_grado, g.nivel_numerico";

    private static final String SELECT_SUBJECTS = "SELECT " + COLUMNS
            + " FROM materias m JOIN grados g ON g.id_grado = m.id_grado";
    private static final String INSERT_SUBJECT = "INSERT INTO materias (codigo_materia, nombre_materia, descripcion, "
            + "id_grado, horas_semanales, esta_activa) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_SUBJECT = "UPDATE materias SET codigo_materia = ?, nombre_materia = ?, "
            + "descripcion = ?, id_grado = ?, horas_semanales = ?, esta_activa = ? WHERE id_materia = ?";
    // Deleting a subject would delete these rows too (ON DELETE CASCADE), so they block the deletion.
    private static final String HAS_HISTORY = "SELECT EXISTS (SELECT 1 FROM asignaciones_profesor WHERE id_materia = ?) "
            + "OR EXISTS (SELECT 1 FROM inscripciones_materia WHERE id_materia = ?) "
            + "OR EXISTS (SELECT 1 FROM registros_academicos WHERE id_materia = ?) "
            + "OR EXISTS (SELECT 1 FROM asistencias WHERE id_materia = ?)";
    private static final int HISTORY_TABLES = 4;

    /** @return the id generated for the subject */
    public int create(Subject subject) throws DataAccessException {
        return executeInsert(INSERT_SUBJECT, statement -> fillData(statement, subject),
                "No se pudo registrar la materia.");
    }

    public Optional<Subject> findById(int id) throws DataAccessException {
        return queryOne(SELECT_SUBJECTS + " WHERE m.id_materia = ?", statement -> statement.setInt(1, id),
                SubjectDAO::mapSubject, "No se pudo buscar la materia.");
    }

    public Optional<Subject> findByCode(String code) throws DataAccessException {
        return queryOne(SELECT_SUBJECTS + " WHERE m.codigo_materia = ?", statement -> statement.setString(1, code),
                SubjectDAO::mapSubject, "No se pudo buscar la materia.");
    }

    public List<Subject> findByGrade(int gradeId) throws DataAccessException {
        return queryList(SELECT_SUBJECTS + " WHERE m.id_grado = ? ORDER BY m.codigo_materia",
                statement -> statement.setInt(1, gradeId), SubjectDAO::mapSubject,
                "No se pudo consultar las materias del grado.");
    }

    public boolean update(Subject subject) throws DataAccessException {
        return executeUpdate(UPDATE_SUBJECT, statement -> {
            fillData(statement, subject);
            statement.setInt(7, subject.getId());
        }, "No se pudo actualizar la materia.") == 1;
    }

    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM materias WHERE id_materia = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar la materia.") == 1;
    }

    /** @return true if the subject has assignments, inscriptions, grades or attendance */
    public boolean hasHistory(int id) throws DataAccessException {
        return queryOne(HAS_HISTORY, statement -> {
            for (int index = 1; index <= HISTORY_TABLES; index++) {
                statement.setInt(index, id);
            }
        }, row -> row.getBoolean(1), "No se pudo revisar el historial de la materia.").orElse(false);
    }

    /** Fills parameters 1 to 6, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, Subject subject) throws SQLException {
        statement.setString(1, subject.getCode());
        statement.setString(2, subject.getName());
        statement.setString(3, subject.getDescription());
        statement.setInt(4, subject.getGrade().getId());
        statement.setInt(5, subject.getWeeklyHours());
        statement.setBoolean(6, subject.isActive());
    }

    static Subject mapSubject(ResultSet row) throws SQLException {
        Subject subject = new Subject();
        subject.setId(row.getInt("id_materia"));
        subject.setCode(row.getString("codigo_materia"));
        subject.setName(row.getString("nombre_materia"));
        subject.setDescription(row.getString("descripcion"));
        subject.setGrade(GradeDAO.mapGrade(row));
        subject.setWeeklyHours(row.getInt("horas_semanales"));
        subject.setActive(row.getBoolean("esta_activa"));
        return subject;
    }
}
