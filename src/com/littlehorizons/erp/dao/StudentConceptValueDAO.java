package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentConceptValue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Special values of the concepts for specific students (valores_concepto_estudiante table). */
public class StudentConceptValueDAO extends BaseDAO {

    private static final String SELECT_VALUES = "SELECT v.id_valor, v.monto_ajustado, v.motivo, "
            + PaymentConceptDAO.COLUMNS + ", " + StudentDAO.SUMMARY_COLUMNS
            + " FROM valores_concepto_estudiante v JOIN conceptos_pago c ON c.id_concepto = v.id_concepto "
            + "JOIN estudiantes e ON e.id_estudiante = v.id_estudiante";
    // One value per student and concept (UNIQUE KEY valor_unico): saving it again changes it.
    private static final String UPSERT_VALUE = "INSERT INTO valores_concepto_estudiante "
            + "(id_estudiante, id_concepto, monto_ajustado, motivo) VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE monto_ajustado = ?, motivo = ?";

    public void save(StudentConceptValue value) throws DataAccessException {
        executeUpdate(UPSERT_VALUE, statement -> {
            statement.setInt(1, value.getStudent().getId());
            statement.setInt(2, value.getConcept().getId());
            statement.setBigDecimal(3, value.getAmount());
            statement.setString(4, value.getReason());
            statement.setBigDecimal(5, value.getAmount());
            statement.setString(6, value.getReason());
        }, "No se pudo guardar el valor especial.");
    }

    public List<StudentConceptValue> findByStudent(int studentId) throws DataAccessException {
        return queryList(SELECT_VALUES + " WHERE v.id_estudiante = ? ORDER BY c.nombre_concepto",
                statement -> statement.setInt(1, studentId), StudentConceptValueDAO::mapValue,
                "No se pudo consultar los valores especiales del estudiante.");
    }

    public Optional<StudentConceptValue> find(int studentId, int conceptId) throws DataAccessException {
        return queryOne(SELECT_VALUES + " WHERE v.id_estudiante = ? AND v.id_concepto = ?", statement -> {
            statement.setInt(1, studentId);
            statement.setInt(2, conceptId);
        }, StudentConceptValueDAO::mapValue, "No se pudo buscar el valor especial.");
    }

    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM valores_concepto_estudiante WHERE id_valor = ?",
                statement -> statement.setInt(1, id), "No se pudo quitar el valor especial.") == 1;
    }

    private static StudentConceptValue mapValue(ResultSet row) throws SQLException {
        StudentConceptValue value = new StudentConceptValue();
        value.setId(row.getInt("id_valor"));
        value.setStudent(StudentDAO.mapStudentSummary(row));
        value.setConcept(PaymentConceptDAO.mapConcept(row));
        value.setAmount(row.getBigDecimal("monto_ajustado"));
        value.setReason(row.getString("motivo"));
        return value;
    }
}
