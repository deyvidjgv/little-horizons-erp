package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Assignments of subjects to teachers (asignaciones_profesor table). */
public class AssignmentDAO extends BaseDAO {

    /** Columns and joins of an assignment; ScheduleDAO reuses them (aliases ap, p, m and g). */
    static final String COLUMNS = "ap.id_asignacion, ap.año_academico, ap.seccion, p.id_profesor, p.tipo_documento, "
            + "p.numero_documento, p.primer_nombre, p.segundo_nombre, p.primer_apellido, p.segundo_apellido, "
            + "p.estado, " + SubjectDAO.COLUMNS;
    static final String JOINS = "JOIN profesores p ON p.id_profesor = ap.id_profesor "
            + "JOIN materias m ON m.id_materia = ap.id_materia JOIN grados g ON g.id_grado = m.id_grado";

    private static final String SELECT_ASSIGNMENTS = "SELECT " + COLUMNS + " FROM asignaciones_profesor ap " + JOINS;
    private static final String ORDER_BY_GROUP = " ORDER BY g.nivel_numerico, m.codigo_materia, ap.seccion";
    private static final String INSERT_ASSIGNMENT = "INSERT INTO asignaciones_profesor "
            + "(id_profesor, id_materia, año_academico, seccion) VALUES (?, ?, ?, ?)";

    /** @return the id generated for the assignment */
    public int create(Assignment assignment) throws DataAccessException {
        return executeInsert(INSERT_ASSIGNMENT, statement -> {
            statement.setInt(1, assignment.getTeacher().getId());
            statement.setInt(2, assignment.getSubject().getId());
            statement.setInt(3, assignment.getAcademicYear());
            statement.setString(4, assignment.getSection());
        }, "No se pudo registrar la asignación.");
    }

    public Optional<Assignment> findById(int id) throws DataAccessException {
        return queryOne(SELECT_ASSIGNMENTS + " WHERE ap.id_asignacion = ?", statement -> statement.setInt(1, id),
                AssignmentDAO::mapAssignment, "No se pudo buscar la asignación.");
    }

    public List<Assignment> findByTeacherAndYear(int teacherId, int year) throws DataAccessException {
        return queryList(SELECT_ASSIGNMENTS + " WHERE ap.id_profesor = ? AND ap.año_academico = ?" + ORDER_BY_GROUP,
                statement -> {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, year);
                }, AssignmentDAO::mapAssignment, "No se pudo consultar las materias del profesor.");
    }

    public List<Assignment> findBySubjectAndYear(int subjectId, int year) throws DataAccessException {
        return queryList(SELECT_ASSIGNMENTS + " WHERE ap.id_materia = ? AND ap.año_academico = ?" + ORDER_BY_GROUP,
                statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, year);
                }, AssignmentDAO::mapAssignment, "No se pudo consultar los profesores de la materia.");
    }

    /** @return who gives that subject to that group, if somebody does */
    public Optional<Assignment> findBySubjectYearAndSection(int subjectId, int year, String section)
            throws DataAccessException {
        return queryOne(SELECT_ASSIGNMENTS + " WHERE ap.id_materia = ? AND ap.año_academico = ? AND ap.seccion = ?",
                statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, year);
                    statement.setString(3, section);
                }, AssignmentDAO::mapAssignment, "No se pudo buscar la asignación.");
    }

    /** The schedules of the assignment are deleted by the database (ON DELETE CASCADE). */
    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM asignaciones_profesor WHERE id_asignacion = ?",
                statement -> statement.setInt(1, id), "No se pudo eliminar la asignación.") == 1;
    }

    /** The teacher comes with the identity and the status only; the full record is in TeacherDAO. */
    static Assignment mapAssignment(ResultSet row) throws SQLException {
        Teacher teacher = new Teacher();
        mapIdentity(row, teacher, "id_profesor");
        teacher.setStatus(DatabaseValue.fromDatabaseValue(TeacherStatus.class, row.getString("estado")));

        Assignment assignment = new Assignment();
        assignment.setId(row.getInt("id_asignacion"));
        assignment.setTeacher(teacher);
        assignment.setSubject(SubjectDAO.mapSubject(row));
        assignment.setAcademicYear(row.getInt("año_academico"));
        assignment.setSection(row.getString("seccion"));
        return assignment;
    }
}
