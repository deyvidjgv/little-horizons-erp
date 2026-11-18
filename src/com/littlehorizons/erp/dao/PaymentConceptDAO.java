package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** CRUD of the conceptos_pago table. */
public class PaymentConceptDAO extends BaseDAO {

    /** Columns of a concept (alias c); other DAOs use them in their JOIN. */
    static final String COLUMNS = "c.id_concepto, c.codigo_concepto, c.nombre_concepto, "
            + "c.descripcion AS concepto_descripcion, c.monto_base, c.es_recurrente, c.esta_activo AS concepto_activo";

    private static final String SELECT_CONCEPTS = "SELECT " + COLUMNS + " FROM conceptos_pago c";
    private static final String INSERT_CONCEPT = "INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, "
            + "descripcion, monto_base, es_recurrente, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_CONCEPT = "UPDATE conceptos_pago SET codigo_concepto = ?, nombre_concepto = ?, "
            + "descripcion = ?, monto_base = ?, es_recurrente = ?, esta_activo = ? WHERE id_concepto = ?";
    // pagos and valores_concepto_estudiante keep the concept (no ON DELETE CASCADE).
    private static final String HAS_HISTORY = "SELECT EXISTS (SELECT 1 FROM pagos WHERE id_concepto = ?) "
            + "OR EXISTS (SELECT 1 FROM valores_concepto_estudiante WHERE id_concepto = ?)";

    /** @return the id generated for the concept */
    public int create(PaymentConcept concept) throws DataAccessException {
        return executeInsert(INSERT_CONCEPT, statement -> fillData(statement, concept),
                "No se pudo registrar el concepto de pago.");
    }

    public Optional<PaymentConcept> findById(int id) throws DataAccessException {
        return queryOne(SELECT_CONCEPTS + " WHERE c.id_concepto = ?", statement -> statement.setInt(1, id),
                PaymentConceptDAO::mapConcept, "No se pudo buscar el concepto de pago.");
    }

    public Optional<PaymentConcept> findByCode(String code) throws DataAccessException {
        return queryOne(SELECT_CONCEPTS + " WHERE c.codigo_concepto = ?", statement -> statement.setString(1, code),
                PaymentConceptDAO::mapConcept, "No se pudo buscar el concepto de pago.");
    }

    public List<PaymentConcept> findAll() throws DataAccessException {
        return queryList(SELECT_CONCEPTS + " ORDER BY c.nombre_concepto", statement -> { },
                PaymentConceptDAO::mapConcept, "No se pudo consultar los conceptos de pago.");
    }

    public boolean update(PaymentConcept concept) throws DataAccessException {
        return executeUpdate(UPDATE_CONCEPT, statement -> {
            fillData(statement, concept);
            statement.setInt(7, concept.getId());
        }, "No se pudo actualizar el concepto de pago.") == 1;
    }

    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM conceptos_pago WHERE id_concepto = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el concepto de pago.") == 1;
    }

    /** @return true if the concept has payments or special values for students */
    public boolean hasHistory(int id) throws DataAccessException {
        return queryOne(HAS_HISTORY, statement -> {
            statement.setInt(1, id);
            statement.setInt(2, id);
        }, row -> row.getBoolean(1), "No se pudo revisar el historial del concepto.").orElse(false);
    }

    /** Fills parameters 1 to 6, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, PaymentConcept concept) throws SQLException {
        statement.setString(1, concept.getCode());
        statement.setString(2, concept.getName());
        statement.setString(3, concept.getDescription());
        statement.setBigDecimal(4, concept.getBaseAmount());
        statement.setBoolean(5, concept.isRecurring());
        statement.setBoolean(6, concept.isActive());
    }

    static PaymentConcept mapConcept(ResultSet row) throws SQLException {
        PaymentConcept concept = new PaymentConcept();
        concept.setId(row.getInt("id_concepto"));
        concept.setCode(row.getString("codigo_concepto"));
        concept.setName(row.getString("nombre_concepto"));
        concept.setDescription(row.getString("concepto_descripcion"));
        concept.setBaseAmount(row.getBigDecimal("monto_base"));
        concept.setRecurring(row.getBoolean("es_recurrente"));
        concept.setActive(row.getBoolean("concepto_activo"));
        return concept;
    }
}
