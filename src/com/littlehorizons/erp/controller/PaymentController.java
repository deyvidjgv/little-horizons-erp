package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.EnrollmentDAO;
import com.littlehorizons.erp.dao.InvoiceDAO;
import com.littlehorizons.erp.dao.PaymentConceptDAO;
import com.littlehorizons.erp.dao.PaymentDAO;
import com.littlehorizons.erp.dao.StudentConceptValueDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.StudentGuardianDAO;
import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.StudentConceptValue;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.service.InvoiceService;
import com.littlehorizons.erp.util.ErrorLogger;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Payments: registering one also creates its electronic invoice; the history can be seen by student
 * or by guardian, and the delinquency report shows who has not paid a recurring concept for more
 * than a month.
 */
public class PaymentController {

    private final PaymentDAO paymentDAO;
    private final InvoiceDAO invoiceDAO;
    private final PaymentConceptDAO conceptDAO;
    private final StudentConceptValueDAO valueDAO;
    private final StudentDAO studentDAO;
    private final StudentGuardianDAO studentGuardianDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final InvoiceService invoiceService;

    public PaymentController(PaymentDAO paymentDAO, InvoiceDAO invoiceDAO, PaymentConceptDAO conceptDAO,
                             StudentConceptValueDAO valueDAO, StudentDAO studentDAO,
                             StudentGuardianDAO studentGuardianDAO, EnrollmentDAO enrollmentDAO,
                             InvoiceService invoiceService) {
        this.paymentDAO = paymentDAO;
        this.invoiceDAO = invoiceDAO;
        this.conceptDAO = conceptDAO;
        this.valueDAO = valueDAO;
        this.studentDAO = studentDAO;
        this.studentGuardianDAO = studentGuardianDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.invoiceService = invoiceService;
    }

    /** @return the guardians who can pay for the student, the one responsible for payments first */
    public List<StudentGuardian> listPayers(int studentId) throws DataAccessException {
        return studentGuardianDAO.findGuardiansByStudent(studentId).stream()
                .sorted(Comparator.comparing((StudentGuardian link) -> !link.getGuardian().isFinanciallyResponsible()))
                .toList();
    }

    /** @return the special value of the concept for the student, if it has one */
    public Optional<StudentConceptValue> findStudentValue(int studentId, int conceptId) throws DataAccessException {
        return valueDAO.find(studentId, conceptId);
    }

    public List<Payment> historyByStudent(int studentId) throws DataAccessException {
        return paymentDAO.findByStudent(studentId);
    }

    public List<Payment> historyByGuardian(int guardianId) throws DataAccessException {
        return paymentDAO.findByGuardian(guardianId);
    }

    public BigDecimal total(List<Payment> payments) {
        return payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Saves the payment and creates its invoice: the data are read back from the database, the
     * simulated CUFE is calculated and the invoice file is saved in the facturas folder.
     */
    public OperationResult registerPayment(Payment payment) throws DataAccessException {
        String rejection = validate(payment);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        int paymentId = paymentDAO.registerWithInvoice(payment, LocalDate.now());
        String paid = "Pago registrado con el recibo " + payment.getReceiptNumber() + ".";
        // From here on the payment is already saved: if the invoice fails, the user must still see the receipt.
        try {
            Optional<Invoice> found = invoiceDAO.findByPayment(paymentId);
            if (found.isEmpty()) {
                return OperationResult.error(paid + " Pero no se encontró la factura del pago.");
            }
            Invoice invoice = found.get();
            invoice.setCufe(invoiceService.calculateCufe(invoice));
            return writeInvoice(invoice, paid);
        } catch (DataAccessException e) {
            return OperationResult.error(paid + " Pero no se pudo generar la factura: " + e.getMessage());
        }
    }

    private OperationResult writeInvoice(Invoice invoice, String paid) throws DataAccessException {
        try {
            Path file = invoiceService.write(invoice);
            invoiceDAO.saveCufeAndPath(invoice.getId(), invoice.getCufe(), file.toString());
            return OperationResult.ok(paid + " Factura No. " + invoice.getNumber() + " guardada en: " + file);
        } catch (IOException e) {
            ErrorLogger.log("PaymentController.registerPayment", e);
            invoiceDAO.saveCufeAndPath(invoice.getId(), invoice.getCufe(), null);
            return OperationResult.error(paid + " Pero no se pudo crear el archivo de la factura No. "
                    + invoice.getNumber() + ": " + e.getMessage());
        }
    }

    /**
     * Students with an active enrollment this year whose last payment of the recurring concept is more
     * than a month old. A student who never paid it counts only when its enrollment is more than a month
     * old: a student enrolled today has nothing pending yet.
     */
    public List<DelinquentStudent> delinquents(int conceptId) throws DataAccessException {
        LocalDate limit = LocalDate.now().minusMonths(1);
        int year = AcademicYear.current();
        // Only the payments of this academic year count: a payment of a previous year does not cover this one.
        Map<Integer, LocalDate> lastPayments = paymentDAO.findByConcept(conceptId).stream()
                .filter(payment -> payment.getAcademicYear() == year)
                .collect(Collectors.toMap(payment -> payment.getStudent().getId(), Payment::getPaymentDate,
                        BinaryOperator.maxBy(Comparator.naturalOrder())));
        return enrollmentDAO.findActiveByYear(year).stream()
                .filter(enrollment -> {
                    LocalDate last = lastPayments.get(enrollment.getStudent().getId());
                    LocalDate pendingSince = last == null ? enrollment.getEnrollmentDate() : last;
                    return pendingSince.isBefore(limit);
                })
                .map(enrollment -> new DelinquentStudent(enrollment.getStudent(), enrollment.getGrade(),
                        lastPayments.get(enrollment.getStudent().getId())))
                .toList();
    }

    public List<PaymentConcept> listRecurringConcepts() throws DataAccessException {
        return conceptDAO.findAll().stream()
                .filter(concept -> concept.isActive() && concept.isRecurring())
                .toList();
    }

    private String validate(Payment payment) throws DataAccessException {
        if (payment.getStudent() == null || studentDAO.findById(payment.getStudent().getId()).isEmpty()) {
            return "El estudiante no existe.";
        }
        boolean payerIsGuardian = payment.getPayer() != null
                && studentGuardianDAO.findGuardiansByStudent(payment.getStudent().getId()).stream()
                .anyMatch(link -> link.getGuardian().getId() == payment.getPayer().getId());
        if (!payerIsGuardian) {
            return "Quien paga debe ser un acudiente del estudiante (es el cliente de la factura).";
        }
        Optional<PaymentConcept> concept = payment.getConcept() == null
                ? Optional.empty() : conceptDAO.findById(payment.getConcept().getId());
        if (concept.isEmpty() || !concept.get().isActive()) {
            return "El concepto no existe o no está activo.";
        }
        BigDecimal amount = payment.getAmount();
        if (!PaymentConceptController.isValidAmount(amount) || amount.signum() == 0) {
            return "El monto debe ser un número entero de pesos mayor que 0.";
        }
        if (payment.getPaymentDate() == null || payment.getPaymentDate().isAfter(LocalDate.now())) {
            return "La fecha de pago es obligatoria y no puede ser futura.";
        }
        if (payment.getMethod() == null || payment.getPeriod() == null) {
            return "El método de pago y el período son obligatorios.";
        }
        if (!AcademicYear.isValid(payment.getAcademicYear())) {
            return "El año académico debe estar entre " + AcademicYear.FIRST + " y " + AcademicYear.last() + ".";
        }
        String notes = payment.getNotes();
        if (notes != null && notes.length() > Payment.NOTES_MAX_LENGTH) {
            return "Las notas admiten máximo " + Payment.NOTES_MAX_LENGTH + " caracteres.";
        }
        return null;
    }
}
