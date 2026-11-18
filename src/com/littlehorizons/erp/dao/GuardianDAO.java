package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** CRUD of the acudientes table. */
public class GuardianDAO extends BaseDAO {

    private static final List<String> COLUMNS = List.of("id_acudiente", "tipo_documento", "numero_documento",
            "primer_nombre", "segundo_nombre", "primer_apellido", "segundo_apellido", "parentesco",
            "telefono_principal", "telefono_secundario", "correo_electronico", "direccion", "ocupacion",
            "es_responsable_financiero");

    private static final String SELECT_GUARDIANS = "SELECT " + columns("a") + " FROM acudientes a";
    private static final String ORDER_BY_NAME = " ORDER BY a.primer_apellido, a.segundo_apellido, a.primer_nombre";
    private static final String INSERT_GUARDIAN = "INSERT INTO acudientes (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, parentesco, telefono_principal, "
            + "telefono_secundario, correo_electronico, direccion, ocupacion, es_responsable_financiero) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_GUARDIAN = "UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, parentesco = ?, "
            + "telefono_principal = ?, telefono_secundario = ?, correo_electronico = ?, direccion = ?, "
            + "ocupacion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?";

    /** @return the guardian columns with the table alias in front, for queries with JOIN */
    static String columns(String alias) {
        return COLUMNS.stream().map(column -> alias + "." + column).collect(Collectors.joining(", "));
    }

    /** @return the id generated for the guardian */
    public int create(Guardian guardian) throws DataAccessException {
        return executeInsert(INSERT_GUARDIAN, statement -> fillData(statement, guardian),
                "No se pudo registrar el acudiente.");
    }

    public Optional<Guardian> findById(int id) throws DataAccessException {
        return queryOne(SELECT_GUARDIANS + " WHERE a.id_acudiente = ?", statement -> statement.setInt(1, id),
                GuardianDAO::mapGuardian, "No se pudo buscar el acudiente.");
    }

    public Optional<Guardian> findByDocument(String documentNumber) throws DataAccessException {
        return queryOne(SELECT_GUARDIANS + " WHERE a.numero_documento = ?",
                statement -> statement.setString(1, documentNumber),
                GuardianDAO::mapGuardian, "No se pudo buscar el acudiente.");
    }

    public List<Guardian> findAll() throws DataAccessException {
        return queryList(SELECT_GUARDIANS + ORDER_BY_NAME, statement -> { },
                GuardianDAO::mapGuardian, "No se pudo consultar los acudientes.");
    }

    public boolean update(Guardian guardian) throws DataAccessException {
        return executeUpdate(UPDATE_GUARDIAN, statement -> {
            fillData(statement, guardian);
            statement.setInt(14, guardian.getId());
        }, "No se pudo actualizar el acudiente.") == 1;
    }

    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM acudientes WHERE id_acudiente = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el acudiente.") == 1;
    }

    /** pagos.id_acudiente has no ON DELETE CASCADE, so a guardian with payments cannot be deleted. */
    public boolean hasPayments(int id) throws DataAccessException {
        return queryOne("SELECT EXISTS (SELECT 1 FROM pagos WHERE id_acudiente = ?)",
                statement -> statement.setInt(1, id), row -> row.getBoolean(1),
                "No se pudo revisar los pagos del acudiente.").orElse(false);
    }

    /** Fills parameters 1 to 13, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, Guardian guardian) throws SQLException {
        fillIdentity(statement, guardian);
        statement.setString(7, guardian.getRelationship());
        statement.setString(8, guardian.getMainPhone());
        statement.setString(9, guardian.getSecondaryPhone());
        statement.setString(10, guardian.getEmail());
        statement.setString(11, guardian.getAddress());
        statement.setString(12, guardian.getOccupation());
        statement.setBoolean(13, guardian.isFinanciallyResponsible());
    }

    static Guardian mapGuardian(ResultSet row) throws SQLException {
        Guardian guardian = new Guardian();
        mapIdentity(row, guardian, "id_acudiente");
        guardian.setRelationship(row.getString("parentesco"));
        guardian.setMainPhone(row.getString("telefono_principal"));
        guardian.setSecondaryPhone(row.getString("telefono_secundario"));
        guardian.setEmail(row.getString("correo_electronico"));
        guardian.setAddress(row.getString("direccion"));
        guardian.setOccupation(row.getString("ocupacion"));
        guardian.setFinanciallyResponsible(row.getBoolean("es_responsable_financiero"));
        return guardian;
    }
}
