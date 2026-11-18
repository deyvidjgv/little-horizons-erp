package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Enrollments (matriculas table). Enrolling, withdrawing and graduating also change the student's
 * status, so each one runs in a transaction.
 */
public class EnrollmentDAO extends BaseDAO {

    private static final String SELECT_ENROLLMENTS = "SELECT m.id_matricula, m.año_academico, m.estado_matricula, "
            + "m.fecha_matricula, m.fecha_retiro, m.observaciones, g.id_grado, g.nombre_grado, g.nivel_numerico, "
            + "e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, e.segundo_nombre, "
            + "e.primer_apellido, e.segundo_apellido, e.estado "
            + "FROM matriculas m JOIN grados g ON g.id_grado = m.id_grado "
            + "JOIN estudiantes e ON e.id_estudiante = m.id_estudiante";
    private static final String INSERT_ENROLLMENT = "INSERT INTO matriculas (id_estudiante, id_grado, año_academico, "
            + "estado_matricula, fecha_matricula, observaciones) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_ENROLLMENT_STATUS = "UPDATE matriculas SET estado_matricula = ?, "
            + "fecha_retiro = ?, observaciones = ? WHERE id_matricula = ?";
    private static final String UPDATE_STUDENT_STATUS = "UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?";

    public List<Enrollment> findByStudent(int studentId) throws DataAccessException {
        return queryList(SELECT_ENROLLMENTS + " WHERE m.id_estudiante = ? ORDER BY m.año_academico DESC",
                statement -> statement.setInt(1, studentId), EnrollmentDAO::mapEnrollment,
                "No se pudo consultar las matrículas del estudiante.");
    }

    public Optional<Enrollment> findByStudentAndYear(int studentId, int year) throws DataAccessException {
        return queryOne(SELECT_ENROLLMENTS + " WHERE m.id_estudiante = ? AND m.año_academico = ?", statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, year);
        }, EnrollmentDAO::mapEnrollment, "No se pudo buscar la matrícula.");
    }

    /** @return the most recent enrollment in Activa status, if there is one */
    public Optional<Enrollment> findActiveByStudent(int studentId) throws DataAccessException {
        return queryOne(SELECT_ENROLLMENTS + " WHERE m.id_estudiante = ? AND m.estado_matricula = ? "
                + "ORDER BY m.año_academico DESC LIMIT 1", statement -> {
                    statement.setInt(1, studentId);
                    statement.setString(2, EnrollmentStatus.ACTIVE.getDatabaseValue());
                }, EnrollmentDAO::mapEnrollment, "No se pudo buscar la matrícula activa.");
    }

    /** @return the active enrollments of the year, by grade and surname */
    public List<Enrollment> findActiveByYear(int year) throws DataAccessException {
        return queryList(SELECT_ENROLLMENTS + " WHERE m.año_academico = ? AND m.estado_matricula = ? "
                + "ORDER BY g.nivel_numerico, e.primer_apellido, e.primer_nombre", statement -> {
                    statement.setInt(1, year);
                    statement.setString(2, EnrollmentStatus.ACTIVE.getDatabaseValue());
                }, EnrollmentDAO::mapEnrollment, "No se pudo consultar las matrículas activas.");
    }

    public List<Enrollment> findByGradeAndYear(int gradeId, int year) throws DataAccessException {
        return queryList(SELECT_ENROLLMENTS + " WHERE m.id_grado = ? AND m.año_academico = ? "
                + "ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre", statement -> {
                    statement.setInt(1, gradeId);
                    statement.setInt(2, year);
                }, EnrollmentDAO::mapEnrollment, "No se pudo consultar las matrículas del grado.");
    }

    /**
     * Saves the new enrollment and leaves the student Activo. If the student had an active enrollment
     * from an earlier year, it is closed as Completada in the same transaction.
     *
     * @param previousToComplete active enrollment of an earlier year, or null
     * @return the id generated for the enrollment
     */
    public int enroll(Enrollment enrollment, Enrollment previousToComplete) throws DataAccessException {
        return inTransaction(connection -> {
            if (previousToComplete != null) {
                changeEnrollmentStatus(connection, previousToComplete, EnrollmentStatus.COMPLETED, null,
                        previousToComplete.getNotes());
            }
            int id = insert(connection, INSERT_ENROLLMENT, statement -> {
                statement.setInt(1, enrollment.getStudent().getId());
                statement.setInt(2, enrollment.getGrade().getId());
                statement.setInt(3, enrollment.getAcademicYear());
                statement.setString(4, EnrollmentStatus.ACTIVE.getDatabaseValue());
                statement.setObject(5, enrollment.getEnrollmentDate());
                statement.setString(6, enrollment.getNotes());
            });
            changeStudentStatus(connection, enrollment.getStudent().getId(), StudentStatus.ACTIVE);
            return id;
        }, "No se pudo registrar la matrícula.");
    }

    /** The enrollment becomes Retirada and the student Retirado. */
    public void withdraw(Enrollment enrollment, LocalDate withdrawalDate, String notes) throws DataAccessException {
        inTransaction(connection -> {
            changeEnrollmentStatus(connection, enrollment, EnrollmentStatus.WITHDRAWN, withdrawalDate, notes);
            return changeStudentStatus(connection, enrollment.getStudent().getId(), StudentStatus.RETIRED);
        }, "No se pudo registrar el retiro.");
    }

    /** The enrollment becomes Completada and the student Graduado. */
    public void graduate(Enrollment enrollment, String notes) throws DataAccessException {
        inTransaction(connection -> {
            changeEnrollmentStatus(connection, enrollment, EnrollmentStatus.COMPLETED, null, notes);
            return changeStudentStatus(connection, enrollment.getStudent().getId(), StudentStatus.GRADUATED);
        }, "No se pudo registrar el grado.");
    }

    private static int changeEnrollmentStatus(Connection connection, Enrollment enrollment, EnrollmentStatus status,
                                              LocalDate withdrawalDate, String notes) throws SQLException {
        return update(connection, UPDATE_ENROLLMENT_STATUS, statement -> {
            statement.setString(1, status.getDatabaseValue());
            statement.setObject(2, withdrawalDate);
            statement.setString(3, notes);
            statement.setInt(4, enrollment.getId());
        });
    }

    private static int changeStudentStatus(Connection connection, int studentId, StudentStatus status)
            throws SQLException {
        return update(connection, UPDATE_STUDENT_STATUS, statement -> {
            statement.setString(1, status.getDatabaseValue());
            statement.setInt(2, studentId);
        });
    }

    /** The student comes with the identity and the status only; the full record is in StudentDAO. */
    private static Enrollment mapEnrollment(ResultSet row) throws SQLException {
        Student student = new Student();
        mapIdentity(row, student, "id_estudiante");
        student.setStatus(DatabaseValue.fromDatabaseValue(StudentStatus.class, row.getString("estado")));

        Enrollment enrollment = new Enrollment();
        enrollment.setId(row.getInt("id_matricula"));
        enrollment.setStudent(student);
        enrollment.setGrade(GradeDAO.mapGrade(row));
        enrollment.setAcademicYear(row.getInt("año_academico"));
        enrollment.setStatus(DatabaseValue.fromDatabaseValue(EnrollmentStatus.class,
                row.getString("estado_matricula")));
        enrollment.setEnrollmentDate(row.getObject("fecha_matricula", LocalDate.class));
        enrollment.setWithdrawalDate(row.getObject("fecha_retiro", LocalDate.class));
        enrollment.setNotes(row.getString("observaciones"));
        return enrollment;
    }
}
