package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

/** Electronic invoices (facturas table), with every piece of data the invoice file shows. */
public class InvoiceDAO extends BaseDAO {

    // The guardian columns get their own names because the student columns have the same ones.
    private static final String SELECT_INVOICES = "SELECT f.id_factura, f.numero_factura, f.fecha_emision, f.cufe, "
            + "f.nombre_archivo_pdf, f.ruta_archivo, " + PaymentDAO.COLUMNS + ", " + PaymentConceptDAO.COLUMNS + ", "
            + StudentDAO.SUMMARY_COLUMNS + ", a.id_acudiente, a.tipo_documento AS acudiente_tipo_documento, "
            + "a.numero_documento AS acudiente_documento, a.primer_nombre AS acudiente_nombre, "
            + "a.segundo_nombre AS acudiente_segundo_nombre, a.primer_apellido AS acudiente_apellido, "
            + "a.segundo_apellido AS acudiente_segundo_apellido, a.direccion AS acudiente_direccion "
            + "FROM facturas f JOIN pagos pg ON pg.id_pago = f.id_pago "
            + "JOIN estudiantes e ON e.id_estudiante = pg.id_estudiante "
            + "JOIN conceptos_pago c ON c.id_concepto = pg.id_concepto "
            + "JOIN acudientes a ON a.id_acudiente = pg.id_acudiente";
    private static final String INSERT_INVOICE = "INSERT INTO facturas (numero_factura, id_pago, fecha_emision, "
            + "nombre_archivo_pdf) VALUES (?, ?, ?, ?)";

    /** Creates the invoice row inside the transaction of the payment. The CUFE and the path come later. */
    static void insert(Connection connection, String number, int paymentId, LocalDate issueDate) throws SQLException {
        update(connection, INSERT_INVOICE, statement -> {
            statement.setString(1, number);
            statement.setInt(2, paymentId);
            statement.setObject(3, issueDate);
            // The column is called nombre_archivo_pdf in the script; it keeps the name of the invoice file.
            statement.setString(4, Invoice.fileNameFor(number));
        });
    }

    public Optional<Invoice> findByPayment(int paymentId) throws DataAccessException {
        return queryOne(SELECT_INVOICES + " WHERE f.id_pago = ?", statement -> statement.setInt(1, paymentId),
                InvoiceDAO::mapInvoice, "No se pudo consultar la factura del pago.");
    }

    public boolean saveCufeAndPath(int invoiceId, String cufe, String filePath) throws DataAccessException {
        return executeUpdate("UPDATE facturas SET cufe = ?, ruta_archivo = ? WHERE id_factura = ?", statement -> {
            statement.setString(1, cufe);
            statement.setString(2, filePath);
            statement.setInt(3, invoiceId);
        }, "No se pudo guardar el CUFE de la factura.") == 1;
    }

    private static Invoice mapInvoice(ResultSet row) throws SQLException {
        Guardian payer = new Guardian();
        payer.setId(row.getInt("id_acudiente"));
        payer.setDocumentType(DatabaseValue.fromDatabaseValue(DocumentType.class,
                row.getString("acudiente_tipo_documento")));
        payer.setDocumentNumber(row.getString("acudiente_documento"));
        payer.setFirstName(row.getString("acudiente_nombre"));
        payer.setMiddleName(row.getString("acudiente_segundo_nombre"));
        payer.setFirstSurname(row.getString("acudiente_apellido"));
        payer.setSecondSurname(row.getString("acudiente_segundo_apellido"));
        payer.setAddress(row.getString("acudiente_direccion"));

        Payment payment = PaymentDAO.mapPaymentData(row);
        payment.setStudent(StudentDAO.mapStudentSummary(row));
        payment.setConcept(PaymentConceptDAO.mapConcept(row));
        payment.setPayer(payer);

        Invoice invoice = new Invoice();
        invoice.setId(row.getInt("id_factura"));
        invoice.setNumber(row.getString("numero_factura"));
        invoice.setPayment(payment);
        invoice.setIssueDate(row.getObject("fecha_emision", LocalDate.class));
        invoice.setCufe(row.getString("cufe"));
        invoice.setFileName(row.getString("nombre_archivo_pdf"));
        invoice.setFilePath(row.getString("ruta_archivo"));
        return invoice;
    }
}
