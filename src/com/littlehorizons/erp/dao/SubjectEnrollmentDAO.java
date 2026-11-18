package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.SubjectEnrollment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Inscriptions of students in subjects, by period (inscripciones_materia table). */
public class SubjectEnrollmentDAO extends BaseDAO {

    private static final String SELECT_INSCRIPTIONS = "SELECT i.id_inscripcion, i.año_academico, "
            + "i.periodo_academico, i.seccion, i.fecha_inscripcion, i.estado, " + StudentDAO.SUMMARY_COLUMNS + ", "
            + SubjectDAO.COLUMNS + " FROM inscripciones_materia i "
            + "JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
            + "JOIN materias m ON m.id_materia = i.id_materia JOIN grados g ON g.id_grado = m.id_grado";
    private static final String INSERT_INSCRIPTION = "INSERT INTO inscripciones_materia (id_estudiante, id_materia, "
            + "año_academico, periodo_academico, seccion, fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_GROUP_STUDENTS = "SELECT DISTINCT " + StudentDAO.SUMMARY_COLUMNS
            + " FROM inscripciones_materia i JOIN estudiantes e ON e.id_estudiante = i.id_estudiante "
            + "WHERE i.id_materia = ? AND i.año_academico = ? AND i.seccion = ? AND i.estado <> ?";
    private static final String ORDER_BY_STUDENT = " ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";

    /** Saves every inscription in one transaction. */
    public int createAll(List<SubjectEnrollment> inscriptions) throws DataAccessException {
        return executeBatch(INSERT_INSCRIPTION, inscriptions, (statement, inscription) -> {
            statement.setInt(1, inscription.getStudent().getId());
            statement.setInt(2, inscription.getSubject().getId());
            statement.setInt(3, inscription.getAcademicYear());
            statement.setString(4, inscription.getPeriod().getDatabaseValue());
            statement.setString(5, inscription.getSection());
            statement.setObject(6, inscription.getEnrollmentDate());
            statement.setString(7, inscription.getStatus().getDatabaseValue());
        }, "No se pudo registrar las inscripciones.");
    }

    public Optional<SubjectEnrollment> findById(int id) throws DataAccessException {
        return queryOne(SELECT_INSCRIPTIONS + " WHERE i.id_inscripcion = ?", statement -> statement.setInt(1, id),
                SubjectEnrollmentDAO::mapInscription, "No se pudo buscar la inscripción.");
    }

    public List<SubjectEnrollment> findByStudent(int studentId, int year, AcademicPeriod period)
            throws DataAccessException {
        return queryList(SELECT_INSCRIPTIONS + " WHERE i.id_estudiante = ? AND i.año_academico = ? "
                + "AND i.periodo_academico = ? ORDER BY m.codigo_materia", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getDatabaseValue());
                }, SubjectEnrollmentDAO::mapInscription, "No se pudo consultar las materias del estudiante.");
    }

    public List<SubjectEnrollment> findBySubject(int subjectId, int year, AcademicPeriod period)
            throws DataAccessException {
        return queryList(SELECT_INSCRIPTIONS + " WHERE i.id_materia = ? AND i.año_academico = ? "
                + "AND i.periodo_academico = ?" + ORDER_BY_STUDENT, statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getDatabaseValue());
                }, SubjectEnrollmentDAO::mapInscription, "No se pudo consultar los estudiantes de la materia.");
    }

    /** @return the inscriptions of all the subjects of a grade, to know which ones are missing */
    public List<SubjectEnrollment> findByGrade(int gradeId, int year, AcademicPeriod period)
            throws DataAccessException {
        return queryList(SELECT_INSCRIPTIONS + " WHERE m.id_grado = ? AND i.año_academico = ? "
                + "AND i.periodo_academico = ?", statement -> {
                    statement.setInt(1, gradeId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getDatabaseValue());
                }, SubjectEnrollmentDAO::mapInscription, "No se pudo consultar las inscripciones del grado.");
    }

    /**
     * Students of a group (subject, year and section) whose inscription was not withdrawn.
     *
     * @param period one period, or null for the students inscribed in any period of the year
     */
    public List<Student> findStudentsOfGroup(int subjectId, int year, String section, AcademicPeriod period)
            throws DataAccessException {
        String sql = SELECT_GROUP_STUDENTS + (period == null ? "" : " AND i.periodo_academico = ?") + ORDER_BY_STUDENT;
        return queryList(sql, statement -> {
            statement.setInt(1, subjectId);
            statement.setInt(2, year);
            statement.setString(3, section);
            statement.setString(4, EnrollmentStatus.WITHDRAWN.getDatabaseValue());
            if (period != null) {
                statement.setString(5, period.getDatabaseValue());
            }
        }, StudentDAO::mapStudentSummary, "No se pudo consultar los estudiantes del grupo.");
    }

    public boolean updateStatus(int id, EnrollmentStatus status) throws DataAccessException {
        return executeUpdate("UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?", statement -> {
            statement.setString(1, status.getDatabaseValue());
            statement.setInt(2, id);
        }, "No se pudo actualizar la inscripción.") == 1;
    }

    private static SubjectEnrollment mapInscription(ResultSet row) throws SQLException {
        SubjectEnrollment inscription = new SubjectEnrollment();
        inscription.setId(row.getInt("id_inscripcion"));
        inscription.setStudent(StudentDAO.mapStudentSummary(row));
        inscription.setSubject(SubjectDAO.mapSubject(row));
        inscription.setAcademicYear(row.getInt("año_academico"));
        inscription.setPeriod(DatabaseValue.fromDatabaseValue(AcademicPeriod.class, row.getString("periodo_academico")));
        inscription.setSection(row.getString("seccion"));
        inscription.setEnrollmentDate(row.getObject("fecha_inscripcion", LocalDate.class));
        inscription.setStatus(DatabaseValue.fromDatabaseValue(EnrollmentStatus.class, row.getString("estado")));
        return inscription;
    }
}
