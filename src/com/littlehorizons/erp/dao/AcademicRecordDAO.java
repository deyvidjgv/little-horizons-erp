package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.DatabaseValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Grades of the students (registros_academicos table). */
public class AcademicRecordDAO extends BaseDAO {

    private static final String SELECT_RECORDS = "SELECT r.id_registro, r.año_academico, r.periodo_academico, "
            + "r.valor_nota, r.observaciones, r.fecha_registro, " + TeacherDAO.SUMMARY_COLUMNS + ", "
            + StudentDAO.SUMMARY_COLUMNS + ", " + SubjectDAO.COLUMNS
            + " FROM registros_academicos r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
            + "JOIN profesores p ON p.id_profesor = r.id_profesor "
            + "JOIN materias m ON m.id_materia = r.id_materia JOIN grados g ON g.id_grado = m.id_grado";
    // A student has one grade per subject, year and period (UNIQUE KEY registro_unico): saving it again corrects it.
    private static final String UPSERT_RECORD = "INSERT INTO registros_academicos (id_estudiante, id_materia, "
            + "id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE id_profesor = ?, valor_nota = ?, fecha_registro = ?";

    /** Saves new grades and corrects existing ones, all in one transaction. */
    public int saveAll(List<AcademicRecord> records) throws DataAccessException {
        return executeBatch(UPSERT_RECORD, records, (statement, record) -> {
            statement.setInt(1, record.getStudent().getId());
            statement.setInt(2, record.getSubject().getId());
            statement.setInt(3, record.getTeacher().getId());
            statement.setInt(4, record.getAcademicYear());
            statement.setString(5, record.getPeriod().getDatabaseValue());
            statement.setBigDecimal(6, record.getValue());
            statement.setObject(7, record.getRecordDate());
            statement.setInt(8, record.getTeacher().getId());
            statement.setBigDecimal(9, record.getValue());
            statement.setObject(10, record.getRecordDate());
        }, "No se pudo guardar las notas.");
    }

    /** @return every grade of the student since entering the school, most recent year first */
    public List<AcademicRecord> findByStudent(int studentId) throws DataAccessException {
        return queryList(SELECT_RECORDS + " WHERE r.id_estudiante = ? ORDER BY r.año_academico DESC, m.codigo_materia",
                statement -> statement.setInt(1, studentId), AcademicRecordDAO::mapRecord,
                "No se pudo consultar las notas del estudiante.");
    }

    public List<AcademicRecord> findBySubject(int subjectId, int year, AcademicPeriod period)
            throws DataAccessException {
        return queryList(SELECT_RECORDS + " WHERE r.id_materia = ? AND r.año_academico = ? AND r.periodo_academico = ?",
                statement -> {
                    statement.setInt(1, subjectId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getDatabaseValue());
                }, AcademicRecordDAO::mapRecord, "No se pudo consultar las notas de la materia.");
    }

    /** @return the grades of every subject of a grade, to calculate the course averages */
    public List<AcademicRecord> findByGrade(int gradeId, int year, AcademicPeriod period) throws DataAccessException {
        return queryList(SELECT_RECORDS + " WHERE m.id_grado = ? AND r.año_academico = ? AND r.periodo_academico = ? "
                + "ORDER BY m.codigo_materia", statement -> {
                    statement.setInt(1, gradeId);
                    statement.setInt(2, year);
                    statement.setString(3, period.getDatabaseValue());
                }, AcademicRecordDAO::mapRecord, "No se pudo consultar las notas del grado.");
    }

    private static AcademicRecord mapRecord(ResultSet row) throws SQLException {
        AcademicRecord record = new AcademicRecord();
        record.setId(row.getInt("id_registro"));
        record.setStudent(StudentDAO.mapStudentSummary(row));
        record.setSubject(SubjectDAO.mapSubject(row));
        record.setTeacher(TeacherDAO.mapTeacherSummary(row));
        record.setAcademicYear(row.getInt("año_academico"));
        record.setPeriod(DatabaseValue.fromDatabaseValue(AcademicPeriod.class, row.getString("periodo_academico")));
        record.setValue(row.getBigDecimal("valor_nota"));
        record.setNotes(row.getString("observaciones"));
        record.setRecordDate(row.getObject("fecha_registro", LocalDate.class));
        return record;
    }
}
