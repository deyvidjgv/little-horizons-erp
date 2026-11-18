package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.Validator;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Guardians submenu, inside Gestión de Estudiantes. */
public class GuardianView implements MenuView {

    private static final int NAME_COLUMN_WIDTH = 34;
    private static final int MAIN_MENU_OPTION = 9;

    private final InputReader input;
    private final GuardianController controller;
    private final StudentController studentController;
    private final PersonForm personForm;
    private final StudentSelector studentSelector;

    public GuardianView(InputReader input, GuardianController controller, StudentController studentController) {
        this.input = input;
        this.controller = controller;
        this.studentController = studentController;
        this.personForm = new PersonForm(input);
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Acudientes");
            System.out.println("1. Registrar acudiente");
            System.out.println("2. Consultar ficha de un acudiente");
            System.out.println("3. Listar acudientes");
            System.out.println("4. Actualizar datos de un acudiente");
            System.out.println("5. Eliminar acudiente");
            System.out.println("6. Vincular acudiente a un estudiante");
            System.out.println("7. Desvincular acudiente de un estudiante");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión de Estudiantes");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> showCard();
                    case 3 -> list();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> link();
                    case 7 -> unlink();
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

    /**
     * Used when registering a student: looks for the guardian by document and, if it does not exist,
     * offers to register it right there.
     */
    Optional<Guardian> findOrRegister() throws DataAccessException {
        String document = input.readText("Número de documento del acudiente", Person.DOCUMENT_MAX_LENGTH);
        Optional<Guardian> found = controller.findByDocument(document);
        if (found.isPresent()) {
            System.out.println("Acudiente: " + found.get().getFullName() + " (" + found.get().getRelationship() + ")");
            return found;
        }
        if (!input.readYesNo("No existe un acudiente con ese documento. ¿Desea registrarlo ahora?")) {
            return Optional.empty();
        }
        Guardian guardian = new Guardian();
        guardian.setDocumentType(input.readChoice("Tipo de documento", GuardianController.DOCUMENT_TYPES,
                DocumentType::getLabel, null));
        guardian.setDocumentNumber(document);
        return saveNew(guardian);
    }

    private void register() throws DataAccessException {
        Screen.subtitle("Registrar acudiente");
        Guardian guardian = new Guardian();
        personForm.readDocument(guardian, GuardianController.DOCUMENT_TYPES, null);
        if (controller.findByDocument(guardian.getDocumentNumber()).isPresent()) {
            Screen.error("Ya existe un acudiente con el documento " + guardian.getDocumentNumber() + ".");
            return;
        }
        saveNew(guardian);
    }

    private Optional<Guardian> saveNew(Guardian guardian) throws DataAccessException {
        readGuardianData(guardian, null);
        OperationResult result = controller.register(guardian);
        Screen.result(result);
        return result.success() ? Optional.of(guardian) : Optional.empty();
    }

    private void showCard() throws DataAccessException {
        Optional<Guardian> found = askGuardian();
        if (found.isEmpty()) {
            return;
        }
        Guardian guardian = found.get();
        Screen.subtitle("Ficha del acudiente");
        System.out.println("Id: " + guardian.getId());
        System.out.println("Documento: " + guardian.getDocument());
        System.out.println("Nombre: " + guardian.getFullName());
        System.out.println("Parentesco: " + guardian.getRelationship());
        System.out.println("Teléfono principal: " + guardian.getMainPhone());
        System.out.println("Teléfono secundario: " + Formats.text(guardian.getSecondaryPhone()));
        System.out.println("Correo: " + Formats.text(guardian.getEmail()));
        System.out.println("Dirección: " + Formats.text(guardian.getAddress()));
        System.out.println("Ocupación: " + Formats.text(guardian.getOccupation()));
        System.out.println("Responsable de pagos: " + (guardian.isFinanciallyResponsible() ? "Sí" : "No"));
        System.out.println();
        System.out.println("Estudiantes a cargo:");
        List<Student> students = controller.listStudents(guardian.getId());
        if (students.isEmpty()) {
            System.out.println("  Ninguno.");
        }
        students.forEach(student -> System.out.println("  - " + student.getFullName() + " ("
                + student.getDocument() + ") - " + student.getStatus().getDatabaseValue()));
    }

    private void list() throws DataAccessException {
        List<Guardian> guardians = controller.list();
        if (guardians.isEmpty()) {
            System.out.println("No hay acudientes registrados.");
            return;
        }
        String format = "%-4s %-22s %-" + NAME_COLUMN_WIDTH + "s %-14s %-12s%n";
        System.out.printf(format, "Id", "Documento", "Nombre", "Parentesco", "Teléfono");
        guardians.forEach(guardian -> System.out.printf(format, guardian.getId(), guardian.getDocument(),
                Screen.fit(guardian.getFullName(), NAME_COLUMN_WIDTH), Screen.fit(guardian.getRelationship(), 14),
                guardian.getMainPhone()));
        System.out.println("Total: " + guardians.size() + " acudiente(s).");
    }

    private void update() throws DataAccessException {
        Optional<Guardian> found = askGuardian();
        if (found.isEmpty()) {
            return;
        }
        Guardian current = found.get();
        Screen.subtitle("Actualizar datos de " + current.getFullName());
        PersonForm.printEditHint();
        Guardian edited = new Guardian();
        edited.setId(current.getId());
        personForm.readDocument(edited, GuardianController.DOCUMENT_TYPES, current);
        readGuardianData(edited, current);
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<Guardian> found = askGuardian();
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar a " + found.get().getFullName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    private void link() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        Optional<Guardian> guardian = askGuardian();
        if (guardian.isEmpty()) {
            return;
        }
        boolean primary = input.readYesNo("¿Es el acudiente principal de " + student.get().getFirstName() + "?");
        boolean emergencyContact = input.readYesNo("¿Es contacto de emergencia?");
        Screen.result(controller.link(student.get().getId(), guardian.get().getId(), primary, emergencyContact));
    }

    private void unlink() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<StudentGuardian> links = studentController.listGuardians(student.get().getId());
        if (links.isEmpty()) {
            Screen.error(student.get().getFullName() + " no tiene acudientes vinculados.");
            return;
        }
        StudentGuardian chosen = input.readChoice("Acudiente a desvincular", links,
                link -> link.getGuardian().getFullName() + " (" + link.getGuardian().getRelationship() + ")", null);
        String question = "¿Desvincular a " + chosen.getGuardian().getFullName() + " de "
                + student.get().getFullName() + "?";
        if (input.readYesNo(question)) {
            Screen.result(controller.unlink(student.get().getId(), chosen.getGuardian().getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private Optional<Guardian> askGuardian() throws DataAccessException {
        String document = input.readText("Número de documento del acudiente", Person.DOCUMENT_MAX_LENGTH);
        Optional<Guardian> guardian = controller.findByDocument(document);
        if (guardian.isEmpty()) {
            Screen.error("No existe un acudiente con el documento " + document + ".");
        }
        return guardian;
    }

    /** @param current guardian being edited, or null when it is new */
    private void readGuardianData(Guardian guardian, Guardian current) {
        personForm.readNames(guardian, current);
        guardian.setRelationship(input.readText("Parentesco (Padre, Madre, Abuelo, Tío, Tutor Legal...)",
                Guardian.RELATIONSHIP_MAX_LENGTH, true, Validator::isName, Validator.NAME_MESSAGE,
                valueOf(current, Guardian::getRelationship)));
        guardian.setMainPhone(personForm.readPhone("Teléfono principal", true,
                valueOf(current, Guardian::getMainPhone)));
        guardian.setSecondaryPhone(personForm.readPhone("Teléfono secundario (opcional)", false,
                valueOf(current, Guardian::getSecondaryPhone)));
        guardian.setEmail(personForm.readEmail(valueOf(current, Guardian::getEmail)));
        guardian.setAddress(personForm.readAddress(valueOf(current, Guardian::getAddress)));
        guardian.setOccupation(input.readText("Ocupación (opcional)", Guardian.OCCUPATION_MAX_LENGTH, false, null,
                null, valueOf(current, Guardian::getOccupation)));
        guardian.setFinanciallyResponsible(input.readYesNo("¿Es responsable de los pagos?",
                valueOf(current, Guardian::isFinanciallyResponsible)));
    }

    private static <T> T valueOf(Guardian current, Function<Guardian, T> getter) {
        return current == null ? null : getter.apply(current);
    }
}
