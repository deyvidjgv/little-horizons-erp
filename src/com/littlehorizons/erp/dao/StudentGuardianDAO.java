package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;

import java.util.List;

/** Links between students and guardians (estudiante_acudiente table, many to many). */
public class StudentGuardianDAO extends BaseDAO {

    private static final String SELECT_GUARDIANS_OF_STUDENT = "SELECT " + GuardianDAO.columns("a")
            + ", ea.es_principal, ea.es_contacto_emergencia FROM estudiante_acudiente ea "
            + "JOIN acudientes a ON a.id_acudiente = ea.id_acudiente WHERE ea.id_estudiante = ? "
            + "ORDER BY ea.es_principal DESC, a.primer_apellido, a.primer_nombre";
    private static final String SELECT_STUDENTS_OF_GUARDIAN = "SELECT " + StudentDAO.columns("e")
            + " FROM estudiante_acudiente ea JOIN estudiantes e ON e.id_estudiante = ea.id_estudiante "
            + "WHERE ea.id_acudiente = ? ORDER BY e.primer_apellido, e.primer_nombre";
    private static final String CLEAR_PRIMARY = "UPDATE estudiante_acudiente SET es_principal = FALSE "
            + "WHERE id_estudiante = ?";
    // If the link already exists, only its flags change.
    private static final String UPSERT_LINK = "INSERT INTO estudiante_acudiente "
            + "(id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE es_principal = ?, es_contacto_emergencia = ?";

    public List<StudentGuardian> findGuardiansByStudent(int studentId) throws DataAccessException {
        return queryList(SELECT_GUARDIANS_OF_STUDENT, statement -> statement.setInt(1, studentId),
                row -> new StudentGuardian(GuardianDAO.mapGuardian(row),
                        row.getBoolean("es_principal"), row.getBoolean("es_contacto_emergencia")),
                "No se pudo consultar los acudientes del estudiante.");
    }

    public List<Student> findStudentsByGuardian(int guardianId) throws DataAccessException {
        return queryList(SELECT_STUDENTS_OF_GUARDIAN, statement -> statement.setInt(1, guardianId),
                StudentDAO::mapStudent, "No se pudo consultar los estudiantes del acudiente.");
    }

    /** A student has only one primary guardian: marking a new one unmarks the previous one in the same transaction. */
    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact)
            throws DataAccessException {
        inTransaction(connection -> {
            if (primary) {
                update(connection, CLEAR_PRIMARY, statement -> statement.setInt(1, studentId));
            }
            return update(connection, UPSERT_LINK, statement -> {
                statement.setInt(1, studentId);
                statement.setInt(2, guardianId);
                statement.setBoolean(3, primary);
                statement.setBoolean(4, emergencyContact);
                statement.setBoolean(5, primary);
                statement.setBoolean(6, emergencyContact);
            });
        }, "No se pudo vincular el acudiente.");
    }

    public boolean unlink(int studentId, int guardianId) throws DataAccessException {
        return executeUpdate("DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?",
                statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, guardianId);
                }, "No se pudo desvincular el acudiente.") == 1;
    }
}
