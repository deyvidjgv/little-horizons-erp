package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.LoanStatus;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceStatus;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Loans of resources to students (prestamos_estudiante) and to teachers (prestamos_profesor).
 * The table names come from BorrowerType, never from what the user types; the values always go as
 * PreparedStatement parameters.
 */
public class LoanDAO extends BaseDAO {

    private static final String LOCK_AVAILABLE = "SELECT COUNT(*) FROM recursos "
            + "WHERE id_recurso = ? AND estado_recurso = ? FOR UPDATE";
    private static final String UPDATE_RESOURCE_STATUS = "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?";

    /**
     * Saves the loan and marks the resource as Prestado. The resource row is locked first, so the same
     * resource cannot be lent twice at the same time.
     *
     * @return false if the resource was not Disponible
     */
    public boolean lend(Loan loan) throws DataAccessException {
        BorrowerType type = loan.getBorrowerType();
        int resourceId = loan.getResource().getId();
        return inTransaction(connection -> {
            int available = queryInt(connection, LOCK_AVAILABLE, statement -> {
                statement.setInt(1, resourceId);
                statement.setString(2, ResourceStatus.AVAILABLE.getDatabaseValue());
            });
            if (available == 0) {
                return false;
            }
            update(connection, "INSERT INTO " + type.getLoanTable() + " (id_recurso, " + type.getPersonIdColumn()
                    + ", fecha_prestamo, fecha_devolucion_esperada, estado_prestamo, observaciones) "
                    + "VALUES (?, ?, ?, ?, ?, ?)", statement -> {
                        statement.setInt(1, resourceId);
                        statement.setInt(2, loan.getBorrower().getId());
                        statement.setObject(3, loan.getLoanDate());
                        statement.setObject(4, loan.getDueDate());
                        statement.setString(5, LoanStatus.ACTIVE.getDatabaseValue());
                        statement.setString(6, loan.getNotes());
                    });
            changeResourceStatus(connection, resourceId, ResourceStatus.LOANED);
            return true;
        }, "No se pudo registrar el préstamo.");
    }

    /** Marks the loan as Devuelto with its real date and the resource as Disponible again. */
    public void registerReturn(Loan loan, LocalDate returnDate) throws DataAccessException {
        BorrowerType type = loan.getBorrowerType();
        inTransaction(connection -> {
            update(connection, "UPDATE " + type.getLoanTable() + " SET estado_prestamo = ?, fecha_devolucion_real = ? "
                    + "WHERE id_prestamo = ?", statement -> {
                        statement.setString(1, LoanStatus.RETURNED.getDatabaseValue());
                        statement.setObject(2, returnDate);
                        statement.setInt(3, loan.getId());
                    });
            return changeResourceStatus(connection, loan.getResource().getId(), ResourceStatus.AVAILABLE);
        }, "No se pudo registrar la devolución.");
    }

    /** @return the loan of the resource that has not been returned, to a student or to a teacher */
    public Optional<Loan> findOpenByResource(int resourceId) throws DataAccessException {
        for (BorrowerType type : BorrowerType.values()) {
            Optional<Loan> loan = queryOne(select(type) + " WHERE l.id_recurso = ? AND l.estado_prestamo <> ?",
                    statement -> {
                        statement.setInt(1, resourceId);
                        statement.setString(2, LoanStatus.RETURNED.getDatabaseValue());
                    }, row -> mapLoan(row, type), "No se pudo buscar el préstamo del recurso.");
            if (loan.isPresent()) {
                return loan;
            }
        }
        return Optional.empty();
    }

    /**
     * Changes to Vencido the active loans whose due date already passed, so the status column shows it.
     *
     * @return how many loans changed
     */
    public int markOverdue(LocalDate today) throws DataAccessException {
        return inTransaction(connection -> {
            int changed = 0;
            for (BorrowerType type : BorrowerType.values()) {
                changed += update(connection, "UPDATE " + type.getLoanTable() + " SET estado_prestamo = ? "
                        + "WHERE estado_prestamo = ? AND fecha_devolucion_esperada < ?", statement -> {
                            statement.setString(1, LoanStatus.OVERDUE.getDatabaseValue());
                            statement.setString(2, LoanStatus.ACTIVE.getDatabaseValue());
                            statement.setObject(3, today);
                        });
            }
            return changed;
        }, "No se pudo actualizar los préstamos vencidos.");
    }

    /** @return the loans of both tables not returned on their due date, the oldest first */
    public List<Loan> findOverdue(LocalDate today) throws DataAccessException {
        List<Loan> overdue = new ArrayList<>();
        for (BorrowerType type : BorrowerType.values()) {
            overdue.addAll(queryList(select(type) + " WHERE l.estado_prestamo <> ? AND l.fecha_devolucion_esperada < ?",
                    statement -> {
                        statement.setString(1, LoanStatus.RETURNED.getDatabaseValue());
                        statement.setObject(2, today);
                    }, row -> mapLoan(row, type), "No se pudo consultar los préstamos vencidos."));
        }
        return overdue;
    }

    private static String select(BorrowerType type) {
        return "SELECT l.id_prestamo, l.fecha_prestamo, l.fecha_devolucion_esperada, l.fecha_devolucion_real, "
                + "l.estado_prestamo, l.observaciones, r.id_recurso, r.codigo_recurso, r.nombre_recurso, "
                + "x." + type.getPersonIdColumn() + " AS id_persona, x.tipo_documento, x.numero_documento, "
                + "x.primer_nombre, x.segundo_nombre, x.primer_apellido, x.segundo_apellido "
                + "FROM " + type.getLoanTable() + " l JOIN recursos r ON r.id_recurso = l.id_recurso "
                + "JOIN " + type.getPersonTable() + " x ON x." + type.getPersonIdColumn() + " = l."
                + type.getPersonIdColumn();
    }

    private static int changeResourceStatus(Connection connection, int resourceId, ResourceStatus status)
            throws SQLException {
        return update(connection, UPDATE_RESOURCE_STATUS, statement -> {
            statement.setString(1, status.getDatabaseValue());
            statement.setInt(2, resourceId);
        });
    }

    private static Loan mapLoan(ResultSet row, BorrowerType type) throws SQLException {
        Person borrower = type.newPerson();
        mapIdentity(row, borrower, "id_persona");
        Resource resource = new Resource();
        resource.setId(row.getInt("id_recurso"));
        resource.setCode(row.getString("codigo_recurso"));
        resource.setName(row.getString("nombre_recurso"));

        Loan loan = new Loan();
        loan.setId(row.getInt("id_prestamo"));
        loan.setBorrowerType(type);
        loan.setBorrower(borrower);
        loan.setResource(resource);
        loan.setLoanDate(row.getObject("fecha_prestamo", LocalDate.class));
        loan.setDueDate(row.getObject("fecha_devolucion_esperada", LocalDate.class));
        loan.setReturnDate(row.getObject("fecha_devolucion_real", LocalDate.class));
        loan.setStatus(DatabaseValue.fromDatabaseValue(LoanStatus.class, row.getString("estado_prestamo")));
        loan.setNotes(row.getString("observaciones"));
        return loan;
    }
}
