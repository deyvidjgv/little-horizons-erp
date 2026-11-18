package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.OperationResult;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Gender;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/** Module 1 of the main menu: students, and from here their guardians and enrollments. */
public class StudentView implements MenuView {

    private static final int NAME_COLUMN_WIDTH = 38;
    private static final int MIN_SEARCH_LENGTH = 2;

    private final InputReader input;
    private final StudentController controller;
    private final GuardianView guardianView;
    private final EnrollmentView enrollmentView;
    private final PersonForm personForm;
    private final StudentSelector selector;

    public StudentView(InputReader input, StudentController controller, GuardianView guardianView,
                       EnrollmentView enrollmentView) {
        this.input = input;
        this.controller = controller;
        this.guardianView = guardianView;
        this.enrollmentView = enrollmentView;
        this.personForm = new PersonForm(input);
        this.selector = new StudentSelector(input, controller);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Gestión de Estudiantes");
            System.out.println("1. Registrar estudiante");
            System.out.println("2. Consultar ficha de un estudiante");
            System.out.println("3. Buscar estudiantes por nombre");
            System.out.println("4. Listar estudiantes");
            System.out.println("5. Actualizar datos de un estudiante");
            System.out.println("6. Eliminar estudiante");
            System.out.println("7. Acudientes");
            System.out.println("8. Matrículas");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 8);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> showCard();
                    case 3 -> searchByName();
                    case 4 -> list();
                    case 5 -> update();
                    case 6 -> delete();
                    case 7 -> {
                        if (guardianView.show() == Navigation.MAIN_MENU) {
                            return Navigation.MAIN_MENU;
                        }
                    }
                    case 8 -> {
                        if (enrollmentView.show() == Navigation.MAIN_MENU) {
                            return Navigation.MAIN_MENU;
                        }
                    }
                    default -> {
                    }
                }
            } catch (DataAccessException e) {
                Screen.error(e.getMessage());
            }
        } while (option != 0);
        return Navigation.BACK;
    }

    private void register() throws DataAccessException {
        Screen.subtitle("Registrar estudiante");
        Student student = new Student();
        personForm.readDocument(student, StudentController.DOCUMENT_TYPES, null);
        if (controller.findByDocument(student.getDocumentNumber()).isPresent()) {
            Screen.error("Ya existe un estudiante con el documento " + student.getDocumentNumber() + ".");
            return;
        }
        readStudentData(student, null);
        String rejection = controller.validateNewStudent(student);
        if (rejection != null) {
            Screen.error(rejection);
            return;
        }

        System.out.println();
        System.out.println("Acudiente del estudiante (todo estudiante debe tener al menos uno).");
        Optional<Guardian> guardian = guardianView.findOrRegister();
        if (guardian.isEmpty()) {
            Screen.error("Registro cancelado: el estudiante necesita un acudiente.");
            return;
        }
        boolean primary = input.readYesNo("¿Es el acudiente principal?");
        System.out.println("Este acudiente queda como contacto de emergencia del estudiante.");
        OperationResult result = controller.register(student, guardian.get().getId(), primary, true);
        Screen.result(result);
        if (result.success() && input.readYesNo("¿Desea matricularlo ahora?")) {
            enrollmentView.enroll(student);
        }
    }

    private void showCard() throws DataAccessException {
        Optional<Student> found = selector.ask();
        if (found.isEmpty()) {
            return;
        }
        Student student = found.get();
        Screen.subtitle("Ficha del estudiante");
        System.out.println("Id: " + student.getId());
        System.out.println("Documento: " + student.getDocument());
        System.out.println("Nombre: " + student.getFullName());
        System.out.println("Fecha de nacimiento: " + Formats.date(student.getBirthDate())
                + " (" + student.getAge() + " años)");
        System.out.println("Género: " + student.getGender().getLabel());
        System.out.println("Dirección: " + Formats.text(student.getAddress()));
        System.out.println("Teléfono: " + Formats.text(student.getPhone()));
        System.out.println("Correo: " + Formats.text(student.getEmail()));
        System.out.println("Tipo de sangre: " + Formats.text(student.getBloodType()));
        System.out.println("Alergias: " + Formats.text(student.getAllergies()));
        System.out.println("Condiciones médicas: " + Formats.text(student.getMedicalConditions()));
        System.out.println("Fecha de ingreso: " + Formats.date(student.getEntryDate()));
        System.out.println("Estado: " + student.getStatus().getDatabaseValue());
        System.out.println();
        System.out.println("Acudientes y contactos de emergencia:");
        controller.listGuardians(student.getId()).forEach(StudentView::printGuardianLink);
        enrollmentView.printHistory(student.getId());
    }

    private void searchByName() throws DataAccessException {
        String text = input.readText("Nombre o apellido a buscar", Person.NAME_MAX_LENGTH, true,
                value -> value.length() >= MIN_SEARCH_LENGTH, "Escriba al menos " + MIN_SEARCH_LENGTH + " letras.",
                null);
        printTable(controller.searchByName(text));
    }

    private void list() throws DataAccessException {
        List<StudentStatus> filters = new ArrayList<>();
        filters.add(null);
        filters.addAll(List.of(StudentStatus.values()));
        StudentStatus status = input.readChoice("Estado de los estudiantes", filters,
                filter -> filter == null ? "Todos" : filter.getDatabaseValue(), null);
        printTable(controller.list(status));
    }

    private void update() throws DataAccessException {
        Optional<Student> found = selector.ask();
        if (found.isEmpty()) {
            return;
        }
        Student current = found.get();
        Screen.subtitle("Actualizar datos de " + current.getFullName());
        PersonForm.printEditHint();
        Student edited = new Student();
        edited.setId(current.getId());
        edited.setStatus(current.getStatus());
        personForm.readDocument(edited, StudentController.DOCUMENT_TYPES, current);
        readStudentData(edited, current);
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<Student> found = selector.ask();
        if (found.isEmpty()) {
            return;
        }
        Student student = found.get();
        if (input.readYesNo("¿Seguro que desea eliminar a " + student.getFullName() + "?")) {
            Screen.result(controller.delete(student.getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    /** @param current student being edited, or null when it is new */
    private void readStudentData(Student student, Student current) {
        personForm.readNames(student, current);
        student.setBirthDate(readBirthDate(current));
        student.setGender(input.readChoice("Género", List.of(Gender.values()), Gender::getLabel,
                valueOf(current, Student::getGender)));
        student.setAddress(personForm.readAddress(valueOf(current, Student::getAddress)));
        student.setPhone(personForm.readPhone("Teléfono (opcional)", false, valueOf(current, Student::getPhone)));
        student.setEmail(personForm.readEmail(valueOf(current, Student::getEmail)));
        String bloodType = input.readText("Tipo de sangre (opcional, ej: O+)", Student.BLOOD_TYPE_MAX_LENGTH, false,
                value -> Validator.isBloodType(value.toUpperCase(Locale.ROOT)), Validator.BLOOD_TYPE_MESSAGE,
                valueOf(current, Student::getBloodType));
        student.setBloodType(bloodType == null ? null : bloodType.toUpperCase(Locale.ROOT));
        student.setAllergies(input.readText("Alergias (opcional)", Student.HEALTH_TEXT_MAX_LENGTH, false, null, null,
                valueOf(current, Student::getAllergies)));
        student.setMedicalConditions(input.readText("Condiciones médicas (opcional)", Student.HEALTH_TEXT_MAX_LENGTH,
                false, null, null, valueOf(current, Student::getMedicalConditions)));
        student.setEntryDate(readEntryDate(student.getBirthDate(), current));
    }

    /** Asks again until the date is not future and, for a new student, gives an age between 3 and 25. */
    private LocalDate readBirthDate(Student current) {
        LocalDate today = LocalDate.now();
        while (true) {
            LocalDate birthDate = input.readDate("Fecha de nacimiento", true, valueOf(current, Student::getBirthDate));
            int age = Period.between(birthDate, today).getYears();
            if (birthDate.isAfter(today)) {
                Screen.error("La fecha de nacimiento no puede ser futura.");
            } else if (current == null && (age < StudentController.MIN_AGE || age > StudentController.MAX_AGE)) {
                Screen.error("Un estudiante nuevo debe tener entre " + StudentController.MIN_AGE + " y "
                        + StudentController.MAX_AGE + " años.");
            } else {
                return birthDate;
            }
        }
    }

    /** Asks again until the date is not future nor before the birth date. */
    private LocalDate readEntryDate(LocalDate birthDate, Student current) {
        while (true) {
            LocalDate entryDate = input.readDate("Fecha de ingreso al colegio", true,
                    valueOf(current, Student::getEntryDate));
            if (!entryDate.isAfter(LocalDate.now()) && !entryDate.isBefore(birthDate)) {
                return entryDate;
            }
            Screen.error("La fecha de ingreso no puede ser futura ni anterior a la fecha de nacimiento.");
        }
    }

    private void printTable(List<Student> students) {
        if (students.isEmpty()) {
            System.out.println("No se encontraron estudiantes.");
            return;
        }
        String format = "%-4s %-22s %-" + NAME_COLUMN_WIDTH + "s %-5s %-10s%n";
        System.out.printf(format, "Id", "Documento", "Nombre", "Edad", "Estado");
        students.forEach(student -> System.out.printf(format, student.getId(), student.getDocument(),
                Screen.fit(student.getFullName(), NAME_COLUMN_WIDTH), student.getAge(),
                student.getStatus().getDatabaseValue()));
        System.out.println("Total: " + students.size() + " estudiante(s).");
    }

    private static void printGuardianLink(StudentGuardian link) {
        Guardian guardian = link.getGuardian();
        List<String> roles = new ArrayList<>();
        if (link.isPrimary()) {
            roles.add("principal");
        }
        if (link.isEmergencyContact()) {
            roles.add("contacto de emergencia");
        }
        System.out.println("  - " + guardian.getFullName() + " (" + guardian.getRelationship() + "), tel. "
                + guardian.getMainPhone() + (roles.isEmpty() ? "" : " [" + String.join(", ", roles) + "]"));
    }

    private static <T> T valueOf(Student current, Function<Student, T> getter) {
        return current == null ? null : getter.apply(current);
    }
}
