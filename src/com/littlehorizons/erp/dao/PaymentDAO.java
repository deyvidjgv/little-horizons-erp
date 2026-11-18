package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.PaymentPeriod;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Payments (pagos table). Registering a payment also creates its invoice row, in the same transaction. */
public class PaymentDAO extends BaseDAO {

    /** Columns of a payment (alias pg); InvoiceDAO uses them too. */
    static final String COLUMNS = "pg.id_pago, pg.monto_pagado, pg.fecha_pago, pg.metodo_pago, pg.numero_recibo, "
            + "pg.año_academico, pg.periodo_academico, pg.notas";

    private static final String SELECT_PAYMENTS = "SELECT " + COLUMNS + ", " + PaymentConceptDAO.COLUMNS + ", "
            + StudentDAO.SUMMARY_COLUMNS + ", a.id_acudiente, a.primer_nombre AS acudiente_nombre, "
            + "a.primer_apellido AS acudiente_apellido, f.numero_factura FROM pagos pg "
            + "JOIN estudiantes e ON e.id_estudiante = pg.id_estudiante "
            + "JOIN conceptos_pago c ON c.id_concepto = pg.id_concepto "
            + "LEFT JOIN acudientes a ON a.id_acudiente = pg.id_acudiente "
            + "LEFT JOIN facturas f ON f.id_pago = pg.id_pago";
    private static final String ORDER_BY_DATE = " ORDER BY pg.fecha_pago DESC, pg.numero_recibo DESC";
    private static final String INSERT_PAYMENT = "INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, "
            + "monto_pagado, fecha_pago, metodo_pago, numero_recibo, año_academico, periodo_academico, notas) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    // Receipts look like RC-2026-0415: the consecutive is the number after the last hyphen, by year.
    // FOR UPDATE locks the rows it reads, so two payments at the same time cannot take the same number.
    private static final String RECEIPT_PREFIX = "RC-";
    private static final String LAST_RECEIPT = "SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(numero_recibo, '-', -1) "
            + "AS UNSIGNED)), 0) FROM pagos WHERE numero_recibo LIKE ? FOR UPDATE";
    private static final String LAST_INVOICE = "SELECT COALESCE(MAX(CAST(numero_factura AS UNSIGNED)), 0) "
            + "FROM facturas FOR UPDATE";

    /**
     * Saves the payment with the next receipt number of its year and creates its invoice with the next
     * consecutive number. Both rows are saved together or none.
     *
     * @return the id generated for the payment (the receipt number is left in the payment)
     */
    public int registerWithInvoice(Payment payment, LocalDate issueDate) throws DataAccessException {
        return inTransaction(connection -> {
            String yearPrefix = RECEIPT_PREFIX + payment.getPaymentDate().getYear() + "-";
            int lastReceipt = queryInt(connection, LAST_RECEIPT, statement -> statement.setString(1, yearPrefix + "%"));
            payment.setReceiptNumber(yearPrefix + String.format("%04d", lastReceipt + 1));
            int paymentId = insert(connection, INSERT_PAYMENT, statement -> {
                statement.setInt(1, payment.getStudent().getId());
                statement.setInt(2, payment.getPayer().getId());
                statement.setInt(3, payment.getConcept().getId());
                statement.setBigDecimal(4, payment.getAmount());
                statement.setObject(5, payment.getPaymentDate());
                statement.setString(6, payment.getMethod().getDatabaseValue());
                statement.setString(7, payment.getReceiptNumber());
                statement.setInt(8, payment.getAcademicYear());
                statement.setString(9, payment.getPeriod().getDatabaseValue());
                statement.setString(10, payment.getNotes());
            });
            int lastInvoice = queryInt(connection, LAST_INVOICE, statement -> { });
            InvoiceDAO.insert(connection, Invoice.formatNumber(lastInvoice + 1), paymentId, issueDate);
            return paymentId;
        }, "No se pudo registrar el pago.");
    }

    public List<Payment> findByStudent(int studentId) throws DataAccessException {
        return queryList(SELECT_PAYMENTS + " WHERE pg.id_estudiante = ?" + ORDER_BY_DATE,
                statement -> statement.setInt(1, studentId), PaymentDAO::mapPayment,
                "No se pudo consultar los pagos del estudiante.");
    }

    /** @return the payments of every student in the care of the guardian (for example, siblings) */
    public List<Payment> findByGuardian(int guardianId) throws DataAccessException {
        return queryList(SELECT_PAYMENTS + " WHERE pg.id_estudiante IN "
                        + "(SELECT ea.id_estudiante FROM estudiante_acudiente ea WHERE ea.id_acudiente = ?)"
                        + ORDER_BY_DATE, statement -> statement.setInt(1, guardianId), PaymentDAO::mapPayment,
                "No se pudo consultar los pagos del acudiente.");
    }

    public List<Payment> findByConcept(int conceptId) throws DataAccessException {
        return queryList(SELECT_PAYMENTS + " WHERE pg.id_concepto = ?" + ORDER_BY_DATE,
                statement -> statement.setInt(1, conceptId), PaymentDAO::mapPayment,
                "No se pudo consultar los pagos del concepto.");
    }

    private static Payment mapPayment(ResultSet row) throws SQLException {
        Payment payment = mapPaymentData(row);
        payment.setStudent(StudentDAO.mapStudentSummary(row));
        payment.setConcept(PaymentConceptDAO.mapConcept(row));
        int payerId = row.getInt("id_acudiente");
        if (!row.wasNull()) {
            Guardian payer = new Guardian();
            payer.setId(payerId);
            payer.setFirstName(row.getString("acudiente_nombre"));
            payer.setFirstSurname(row.getString("acudiente_apellido"));
            payment.setPayer(payer);
        }
        payment.setInvoiceNumber(row.getString("numero_factura"));
        return payment;
    }

    /** Maps the COLUMNS of the payment itself. */
    static Payment mapPaymentData(ResultSet row) throws SQLException {
        String period = row.getString("periodo_academico");
        Payment payment = new Payment();
        payment.setId(row.getInt("id_pago"));
        payment.setAmount(row.getBigDecimal("monto_pagado"));
        payment.setPaymentDate(row.getObject("fecha_pago", LocalDate.class));
        payment.setMethod(DatabaseValue.fromDatabaseValue(PaymentMethod.class, row.getString("metodo_pago")));
        payment.setReceiptNumber(row.getString("numero_recibo"));
        payment.setAcademicYear(row.getInt("año_academico"));
        payment.setPeriod(period == null ? null : DatabaseValue.fromDatabaseValue(PaymentPeriod.class, period));
        payment.setNotes(row.getString("notas"));
        return payment;
    }
}
