package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.DelinquentStudent;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.PaymentPeriod;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentConceptValue;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Module 5 of the main menu: payment concepts, payments with their invoice, history and delinquency. */
public class FinanceView implements MenuView {

    private static final int CONCEPT_COLUMN_WIDTH = 26;
    private static final int NAME_COLUMN_WIDTH = 36;

    private final InputReader input;
    private final PaymentController controller;
    private final PaymentConceptController conceptController;
    private final GuardianController guardianController;
    private final PaymentConceptView conceptView;
    private final StudentSelector studentSelector;

    public FinanceView(InputReader input, PaymentController controller, PaymentConceptController conceptController,
                       GuardianController guardianController, StudentController studentController,
                       PaymentConceptView conceptView) {
        this.input = input;
        this.controller = controller;
        this.conceptController = conceptController;
        this.guardianController = guardianController;
        this.conceptView = conceptView;
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Gestión Financiera");
            System.out.println("1. Conceptos de pago y valores por estudiante");
            System.out.println("2. Registrar pago y generar factura");
            System.out.println("3. Historial de pagos de un estudiante");
            System.out.println("4. Historial de pagos de un acudiente");
            System.out.println("5. Reporte de morosos");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 5);
            try {
                switch (option) {
                    case 1 -> {
                        if (conceptView.show() == Navigation.MAIN_MENU) {
                            return Navigation.MAIN_MENU;
                        }
                    }
                    case 2 -> registerPayment();
                    case 3 -> showStudentHistory();
                    case 4 -> showGuardianHistory();
                    case 5 -> delinquencyReport();
                    default -> {
                    }
                }
            } catch (DataAccessException e) {
                Screen.error(e.getMessage());
            }
        } while (option != 0);
        return Navigation.BACK;
    }

    private void registerPayment() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<StudentGuardian> payers = controller.listPayers(student.get().getId());
        if (payers.isEmpty()) {
            Screen.error(student.get().getFullName() + " no tiene acudientes; el pago necesita uno.");
            return;
        }
        StudentGuardian payer = input.readChoice("Acudiente que paga (cliente de la factura)", payers,
                link -> link.getGuardian().getFullName() + " (" + link.getGuardian().getRelationship() + ")"
                        + (link.getGuardian().isFinanciallyResponsible() ? " - responsable de pagos" : ""),
                payers.get(0));
        List<PaymentConcept> concepts = conceptController.listActive();
        if (concepts.isEmpty()) {
            Screen.error("No hay conceptos de pago activos.");
            return;
        }
        PaymentConcept concept = input.readChoice("Concepto", concepts,
                item -> item.getName() + " - " + Formats.money(item.getBaseAmount()), null);
        Optional<StudentConceptValue> specialValue = controller.findStudentValue(student.get().getId(), concept.getId());
        specialValue.ifPresent(value -> System.out.println("Valor especial de este estudiante: "
                + Formats.money(value.getAmount()) + " (" + value.getReason() + ")"));

        Payment payment = new Payment();
        payment.setStudent(student.get());
        payment.setPayer(payer.getGuardian());
        payment.setConcept(concept);
        payment.setAmount(Prompts.readPesos(input, "Monto pagado en pesos", false,
                specialValue.map(StudentConceptValue::getAmount).orElse(concept.getBaseAmount())));
        payment.setPaymentDate(input.readDate("Fecha de pago", true, LocalDate.now()));
        payment.setMethod(input.readChoice("Método de pago", List.of(PaymentMethod.values()),
                PaymentMethod::getDatabaseValue, null));
        payment.setAcademicYear(Prompts.readAcademicYear(input));
        payment.setPeriod(input.readChoice("Período al que aplica", List.of(PaymentPeriod.values()),
                PaymentPeriod::getDatabaseValue, null));
        payment.setNotes(input.readOptionalText("Notas (opcional, ej: Pensión de noviembre)", Payment.NOTES_MAX_LENGTH));

        Screen.subtitle("Resumen del pago");
        System.out.println("Estudiante: " + student.get().getFullName());
        System.out.println("Paga: " + payer.getGuardian().getFullName());
        System.out.println("Concepto: " + concept.getName() + " - " + Formats.money(payment.getAmount()));
        System.out.println("Fecha: " + Formats.date(payment.getPaymentDate()) + " - "
                + payment.getMethod().getDatabaseValue());
        if (input.readYesNo("¿Confirma el pago?")) {
            Screen.result(controller.registerPayment(payment));
        } else {
            System.out.println("Pago cancelado.");
        }
    }

    private void showStudentHistory() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<Payment> payments = controller.historyByStudent(student.get().getId());
        Screen.subtitle("Pagos de " + student.get().getFullName());
        printPayments(payments);
    }

    /** A guardian can have several children in the school: their payments are shown by student. */
    private void showGuardianHistory() throws DataAccessException {
        String document = input.readText("Número de documento del acudiente", Person.DOCUMENT_MAX_LENGTH);
        Optional<Guardian> guardian = guardianController.findByDocument(document);
        if (guardian.isEmpty()) {
            Screen.error("No existe un acudiente con el documento " + document + ".");
            return;
        }
        List<Payment> payments = controller.historyByGuardian(guardian.get().getId());
        Screen.subtitle("Pagos de los estudiantes a cargo de " + guardian.get().getFullName());
        if (payments.isEmpty()) {
            System.out.println("Sin pagos registrados.");
            return;
        }
        Map<String, List<Payment>> byStudent = payments.stream()
                .collect(Collectors.groupingBy(payment -> payment.getStudent().getFullName(), LinkedHashMap::new,
                        Collectors.toList()));
        byStudent.forEach((name, studentPayments) -> {
            System.out.println();
            System.out.println(name + ":");
            printPayments(studentPayments);
        });
        System.out.println();
        System.out.println("Total pagado por los " + byStudent.size() + " estudiante(s): "
                + Formats.money(controller.total(payments)));
    }

    /** Also used by Reportes y Estadísticas. */
    void delinquencyReport() throws DataAccessException {
        List<PaymentConcept> recurring = controller.listRecurringConcepts();
        if (recurring.isEmpty()) {
            Screen.error("No hay conceptos recurrentes activos.");
            return;
        }
        PaymentConcept concept = input.readChoice("Concepto mensual a revisar", recurring, PaymentConcept::getName, null);
        List<DelinquentStudent> delinquents = controller.delinquents(concept.getId());
        Screen.subtitle("Morosos: " + concept.getName() + " sin pagar hace más de un mes");
        if (delinquents.isEmpty()) {
            System.out.println("Ningún estudiante con matrícula activa está atrasado.");
            return;
        }
        LocalDate today = LocalDate.now();
        String format = "%-22s %-" + NAME_COLUMN_WIDTH + "s %-11s %-12s %s%n";
        System.out.printf(format, "Documento", "Nombre", "Grado", "Último pago", "Días sin pagar");
        delinquents.forEach(row -> System.out.printf(format, row.student().getDocument(),
                Screen.fit(row.student().getFullName(), NAME_COLUMN_WIDTH), row.grade().getName(),
                row.lastPayment() == null ? "nunca" : Formats.date(row.lastPayment()),
                row.lastPayment() == null ? "-" : ChronoUnit.DAYS.between(row.lastPayment(), today)));
        System.out.println("Total: " + delinquents.size() + " estudiante(s).");
    }

    private void printPayments(List<Payment> payments) {
        if (payments.isEmpty()) {
            System.out.println("Sin pagos registrados.");
            return;
        }
        String format = "  %-11s %-13s %-" + CONCEPT_COLUMN_WIDTH + "s %12s  %-15s %s%n";
        System.out.printf(format, "Fecha", "Recibo", "Concepto", "Monto", "Método", "Factura");
        payments.forEach(payment -> System.out.printf(format, Formats.date(payment.getPaymentDate()),
                Formats.text(payment.getReceiptNumber()),
                Screen.fit(payment.getConcept().getName() + notesOf(payment), CONCEPT_COLUMN_WIDTH),
                Formats.money(payment.getAmount()), payment.getMethod().getDatabaseValue(),
                Formats.text(payment.getInvoiceNumber())));
        BigDecimal total = controller.total(payments);
        System.out.println("  Total: " + Formats.money(total) + " en " + payments.size() + " pago(s).");
    }

    private static String notesOf(Payment payment) {
        return payment.getNotes() == null ? "" : " - " + payment.getNotes();
    }
}
