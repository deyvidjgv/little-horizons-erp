package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.DatabaseValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Daily attendance of the students by subject (asistencias table). */
public class AttendanceDAO extends BaseDAO {

    private static final String SELECT_ATTENDANCE = "SELECT a.id_asistencia, a.fecha_asistencia, a.estado_asistencia, "
            + "a.observaciones, a.registrado_por, " + StudentDAO.SUMMARY_COLUMNS + ", " + SubjectDAO.COLUMNS
            + " FROM asistencias a JOIN estudiantes e ON e.id_estudiante = a.id_estudiante "
            + "JOIN materias m ON m.id_materia = a.id_materia JOIN grados g ON g.id_grado = m.id_grado";
    // One attendance per student, subject and day (UNIQUE KEY asistencia_unica): taking it again corrects it.
    private static final String UPSERT_ATTENDANCE = "INSERT INTO asistencias (id_estudiante, id_materia, "
            + "fecha_asistencia, estado_asistencia, registrado_por) VALUES (?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE estado_asistencia = ?, registrado_por = ?";

    /** Saves the attendance of a whole class in one transaction. */
    public int saveAll(List<Attendance> attendances) throws DataAccessException {
        return executeBatch(UPSERT_ATTENDANCE, attendances, (statement, attendance) -> {
            statement.setInt(1, attendance.getStudent().getId());
            statement.setInt(2, attendance.getSubject().getId());
            statement.setObject(3, attendance.getDate());
            statement.setString(4, attendance.getStatus().getDatabaseValue());
            statement.setInt(5, attendance.getRecordedById());
            statement.setString(6, attendance.getStatus().getDatabaseValue());
            statement.setInt(7, attendance.getRecordedById());
        }, "No se pudo guardar la asistencia.");
    }

    public List<Attendance> findBySubjectAndDate(int subjectId, LocalDate date) throws DataAccessException {
        return queryList(SELECT_ATTENDANCE + " WHERE a.id_materia = ? AND a.fecha_asistencia = ?", statement -> {
            statement.setInt(1, subjectId);
            statement.setObject(2, date);
        }, AttendanceDAO::mapAttendance, "No se pudo consultar la asistencia de la clase.");
    }

    public List<Attendance> findByStudent(int studentId, LocalDate from, LocalDate to) throws DataAccessException {
        return queryList(SELECT_ATTENDANCE + " WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ? "
                + "ORDER BY a.fecha_asistencia, m.codigo_materia", statement -> {
                    statement.setInt(1, studentId);
                    statement.setObject(2, from);
                    statement.setObject(3, to);
                }, AttendanceDAO::mapAttendance, "No se pudo consultar la asistencia del estudiante.");
    }

    /** @return the attendance rows with that status between the two dates, both included */
    public List<Attendance> findByStatus(AttendanceStatus status, LocalDate from, LocalDate to)
            throws DataAccessException {
        return queryList(SELECT_ATTENDANCE + " WHERE a.estado_asistencia = ? AND a.fecha_asistencia BETWEEN ? AND ? "
                + "ORDER BY e.primer_apellido, e.primer_nombre, a.fecha_asistencia", statement -> {
                    statement.setString(1, status.getDatabaseValue());
                    statement.setObject(2, from);
                    statement.setObject(3, to);
                }, AttendanceDAO::mapAttendance, "No se pudo consultar las inasistencias.");
    }

    private static Attendance mapAttendance(ResultSet row) throws SQLException {
        Attendance attendance = new Attendance();
        attendance.setId(row.getInt("id_asistencia"));
        attendance.setStudent(StudentDAO.mapStudentSummary(row));
        attendance.setSubject(SubjectDAO.mapSubject(row));
        attendance.setDate(row.getObject("fecha_asistencia", LocalDate.class));
        attendance.setStatus(DatabaseValue.fromDatabaseValue(AttendanceStatus.class,
                row.getString("estado_asistencia")));
        attendance.setNotes(row.getString("observaciones"));
        attendance.setRecordedById(row.getInt("registrado_por"));
        return attendance;
    }
}
