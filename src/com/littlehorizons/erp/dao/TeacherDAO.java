package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** CRUD of the profesores table. */
public class TeacherDAO extends BaseDAO {

    private static final String SELECT_TEACHERS = "SELECT id_profesor, tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, "
            + "especializacion, fecha_contratacion, estado FROM profesores";
    private static final String INSERT_TEACHER = "INSERT INTO profesores (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, correo_electronico, telefono, "
            + "especializacion, fecha_contratacion, estado) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_TEACHER = "UPDATE profesores SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, "
            + "correo_electronico = ?, telefono = ?, especializacion = ?, fecha_contratacion = ?, estado = ? "
            + "WHERE id_profesor = ?";
    // Grades, attendance, clubs and events keep the teacher as history (no ON DELETE CASCADE).
    private static final String HAS_HISTORY = "SELECT EXISTS (SELECT 1 FROM asignaciones_profesor WHERE id_profesor = ?) "
            + "OR EXISTS (SELECT 1 FROM registros_academicos WHERE id_profesor = ?) "
            + "OR EXISTS (SELECT 1 FROM asistencias WHERE registrado_por = ?) "
            + "OR EXISTS (SELECT 1 FROM clubes WHERE id_profesor_supervisor = ?) "
            + "OR EXISTS (SELECT 1 FROM eventos WHERE id_organizador = ?) "
            + "OR EXISTS (SELECT 1 FROM prestamos_profesor WHERE id_profesor = ?)";
    private static final int HISTORY_TABLES = 6;

    /** Id and names of the teacher in charge of a club, an event or a grade (alias p). */
    static final String SUMMARY_COLUMNS = "p.id_profesor, p.primer_nombre AS profesor_nombre, "
            + "p.primer_apellido AS profesor_apellido";

    /** @return the id generated for the teacher */
    public int create(Teacher teacher) throws DataAccessException {
        return executeInsert(INSERT_TEACHER, statement -> fillData(statement, teacher),
                "No se pudo registrar el profesor.");
    }

    public Optional<Teacher> findById(int id) throws DataAccessException {
        return queryOne(SELECT_TEACHERS + " WHERE id_profesor = ?", statement -> statement.setInt(1, id),
                TeacherDAO::mapTeacher, "No se pudo buscar el profesor.");
    }

    public Optional<Teacher> findByDocument(String documentNumber) throws DataAccessException {
        return queryOne(SELECT_TEACHERS + " WHERE numero_documento = ?",
                statement -> statement.setString(1, documentNumber),
                TeacherDAO::mapTeacher, "No se pudo buscar el profesor.");
    }

    public Optional<Teacher> findByEmail(String email) throws DataAccessException {
        return queryOne(SELECT_TEACHERS + " WHERE correo_electronico = ?", statement -> statement.setString(1, email),
                TeacherDAO::mapTeacher, "No se pudo buscar el profesor.");
    }

    public List<Teacher> findAll() throws DataAccessException {
        return queryList(SELECT_TEACHERS + " ORDER BY primer_apellido, segundo_apellido, primer_nombre",
                statement -> { }, TeacherDAO::mapTeacher, "No se pudo consultar los profesores.");
    }

    public boolean update(Teacher teacher) throws DataAccessException {
        return executeUpdate(UPDATE_TEACHER, statement -> {
            fillData(statement, teacher);
            statement.setInt(12, teacher.getId());
        }, "No se pudo actualizar el profesor.") == 1;
    }

    /** Assignments, schedules and loans are deleted by the database (ON DELETE CASCADE). */
    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM profesores WHERE id_profesor = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el profesor.") == 1;
    }

    /** @return true if the teacher has assignments, grades, attendance, clubs, events or loans */
    public boolean hasHistory(int id) throws DataAccessException {
        return queryOne(HAS_HISTORY, statement -> {
            for (int index = 1; index <= HISTORY_TABLES; index++) {
                statement.setInt(index, id);
            }
        }, row -> row.getBoolean(1), "No se pudo revisar el historial del profesor.").orElse(false);
    }

    /** Fills parameters 1 to 11, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, Teacher teacher) throws SQLException {
        fillIdentity(statement, teacher);
        statement.setString(7, teacher.getEmail());
        statement.setString(8, teacher.getPhone());
        statement.setString(9, teacher.getSpecialization());
        statement.setObject(10, teacher.getHireDate());
        statement.setString(11, teacher.getStatus().getDatabaseValue());
    }

    /** @return the teacher with its id and names, or null if the row has none (LEFT JOIN without a teacher) */
    static Teacher mapTeacherSummary(ResultSet row) throws SQLException {
        int id = row.getInt("id_profesor");
        if (row.wasNull()) {
            return null;
        }
        Teacher teacher = new Teacher();
        teacher.setId(id);
        teacher.setFirstName(row.getString("profesor_nombre"));
        teacher.setFirstSurname(row.getString("profesor_apellido"));
        return teacher;
    }

    private static Teacher mapTeacher(ResultSet row) throws SQLException {
        Teacher teacher = new Teacher();
        mapIdentity(row, teacher, "id_profesor");
        teacher.setEmail(row.getString("correo_electronico"));
        teacher.setPhone(row.getString("telefono"));
        teacher.setSpecialization(row.getString("especializacion"));
        teacher.setHireDate(row.getObject("fecha_contratacion", LocalDate.class));
        teacher.setStatus(DatabaseValue.fromDatabaseValue(TeacherStatus.class, row.getString("estado")));
        return teacher;
    }
}
