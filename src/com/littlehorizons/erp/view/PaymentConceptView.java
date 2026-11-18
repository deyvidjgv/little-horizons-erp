package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentConceptValue;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Catalog of payment concepts and special values by student, inside Gestión Financiera. */
public class PaymentConceptView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int NAME_COLUMN_WIDTH = 28;

    private final InputReader input;
    private final PaymentConceptController controller;
    private final StudentSelector studentSelector;

    public PaymentConceptView(InputReader input, PaymentConceptController controller,
                              StudentController studentController) {
        this.input = input;
        this.controller = controller;
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Conceptos de Pago");
            System.out.println("1. Registrar concepto");
            System.out.println("2. Listar conceptos");
            System.out.println("3. Actualizar concepto");
            System.out.println("4. Eliminar concepto");
            System.out.println("5. Asignar valor especial a un estudiante");
            System.out.println("6. Ver valores especiales de un estudiante");
            System.out.println("7. Quitar valor especial de un estudiante");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión Financiera");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> list();
                    case 3 -> update();
                    case 4 -> delete();
                    case 5 -> setStudentValue();
                    case 6 -> showStudentValues();
                    case 7 -> removeStudentValue();
                    case MAIN_MENU_OPTION -> {
                        return Navigation.MAIN_MENU;
                    }
                    case 0 -> {
                    }
                    default -> Screen.error("Opción no válida.");
                }
            } catch (DataAccessException e) {
                Screen.error(e.getMessage());
            }
        } while (option != 0);
        return Navigation.BACK;
    }

    private void register() throws DataAccessException {
        Screen.subtitle("Registrar concepto de pago");
        PaymentConcept concept = new PaymentConcept();
        readConceptData(concept, null);
        Screen.result(controller.register(concept));
    }

    private void list() throws DataAccessException {
        List<PaymentConcept> concepts = controller.list();
        if (concepts.isEmpty()) {
            System.out.println("No hay conceptos registrados.");
            return;
        }
        String format = "%-14s %-" + NAME_COLUMN_WIDTH + "s %14s  %-10s %-6s%n";
        System.out.printf(format, "Código", "Nombre", "Valor base", "Recurrente", "Activo");
        concepts.forEach(concept -> System.out.printf(format, concept.getCode(),
                Screen.fit(concept.getName(), NAME_COLUMN_WIDTH), Formats.money(concept.getBaseAmount()),
                concept.isRecurring() ? "Sí" : "No", concept.isActive() ? "Sí" : "No"));
    }

    private void update() throws DataAccessException {
        Optional<PaymentConcept> found = chooseConcept(controller.list());
        if (found.isEmpty()) {
            return;
        }
        PaymentConcept current = found.get();
        Screen.subtitle("Actualizar " + current.getName());
        PersonForm.printEditHint();
        PaymentConcept edited = new PaymentConcept();
        edited.setId(current.getId());
        readConceptData(edited, current);
        edited.setActive(input.readYesNo("¿Está activo?", current.isActive()));
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<PaymentConcept> found = chooseConcept(controller.list());
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar " + found.get().getName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    private void setStudentValue() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        Optional<PaymentConcept> concept = chooseConcept(controller.listActive());
        if (concept.isEmpty()) {
            return;
        }
        Optional<StudentConceptValue> current = controller.listStudentValues(student.get().getId()).stream()
                .filter(value -> value.getConcept().getId() == concept.get().getId())
                .findFirst();
        current.ifPresent(value -> System.out.println("Valor especial actual: " + Formats.money(value.getAmount())
                + " (" + value.getReason() + ")"));
        BigDecimal amount = Prompts.readPesos(input, "Valor que pagará el estudiante", true,
                current.map(StudentConceptValue::getAmount).orElse(concept.get().getBaseAmount()));
        String reason = input.readText("Motivo (ej: Descuento por hermanos (10 %))",
                StudentConceptValue.REASON_MAX_LENGTH, true, null, null,
                current.map(StudentConceptValue::getReason).orElse(null));
        Screen.result(controller.setStudentValue(student.get().getId(), concept.get().getId(), amount, reason));
    }

    private void showStudentValues() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<StudentConceptValue> values = controller.listStudentValues(student.get().getId());
        System.out.println();
        System.out.println("Valores especiales de " + student.get().getFullName() + ":");
        if (values.isEmpty()) {
            System.out.println("  Ninguno: paga el valor base de cada concepto.");
        }
        values.forEach(value -> System.out.println("  - " + value.getConcept().getName() + ": "
                + Formats.money(value.getAmount()) + " en vez de " + Formats.money(value.getConcept().getBaseAmount())
                + " (" + value.getReason() + ")"));
    }

    private void removeStudentValue() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<StudentConceptValue> values = controller.listStudentValues(student.get().getId());
        if (values.isEmpty()) {
            Screen.error(student.get().getFullName() + " no tiene valores especiales.");
            return;
        }
        StudentConceptValue chosen = input.readChoice("Valor especial", values,
                value -> value.getConcept().getName() + ": " + Formats.money(value.getAmount()), null);
        if (input.readYesNo("¿Quitar este valor especial?")) {
            Screen.result(controller.removeStudentValue(chosen.getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private Optional<PaymentConcept> chooseConcept(List<PaymentConcept> concepts) {
        if (concepts.isEmpty()) {
            Screen.error("No hay conceptos para elegir.");
            return Optional.empty();
        }
        return Optional.of(input.readChoice("Concepto", concepts,
                concept -> concept.getName() + " - " + Formats.money(concept.getBaseAmount()), null));
    }

    /** @param current concept being edited, or null when it is new */
    private void readConceptData(PaymentConcept concept, PaymentConcept current) {
        String code = input.readText("Código (ej: PENSION)", PaymentConcept.CODE_MAX_LENGTH, true,
                value -> PaymentConceptController.isValidCode(PaymentConceptController.normalizeCode(value)),
                PaymentConceptController.CODE_RULE, current == null ? null : current.getCode());
        concept.setCode(PaymentConceptController.normalizeCode(code));
        concept.setName(input.readText("Nombre del concepto", PaymentConcept.NAME_MAX_LENGTH, true, null, null,
                current == null ? null : current.getName()));
        concept.setDescription(input.readText("Descripción (opcional)", PaymentConcept.DESCRIPTION_MAX_LENGTH, false,
                null, null, current == null ? null : current.getDescription()));
        concept.setBaseAmount(Prompts.readPesos(input, "Valor base en pesos", false,
                current == null ? null : current.getBaseAmount()));
        concept.setRecurring(input.readYesNo("¿Es un pago recurrente (mensual)?",
                current == null ? null : current.isRecurring()));
    }
}
