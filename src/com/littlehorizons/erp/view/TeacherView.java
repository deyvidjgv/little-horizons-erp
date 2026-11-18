package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Module 3 of the main menu: teacher directory, and from here their subjects and schedules. */
public class TeacherView implements MenuView {

    private static final int NAME_COLUMN_WIDTH = 32;
    private static final int SPECIALIZATION_COLUMN_WIDTH = 24;

    private final InputReader input;
    private final TeacherController controller;
    private final AssignmentView assignmentView;
    private final PersonForm personForm;
    private final TeacherSelector selector;

    public TeacherView(InputReader input, TeacherController controller, AssignmentView assignmentView) {
        this.input = input;
        this.controller = controller;
        this.assignmentView = assignmentView;
        this.personForm = new PersonForm(input);
        this.selector = new TeacherSelector(input, controller);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Gestión de Profesores");
            System.out.println("1. Registrar profesor");
            System.out.println("2. Consultar ficha de un profesor");
            System.out.println("3. Listar profesores");
            System.out.println("4. Actualizar datos de un profesor");
            System.out.println("5. Eliminar profesor");
            System.out.println("6. Asignación de materias y horarios");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 6);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> showCard();
                    case 3 -> list();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> {
                        if (assignmentView.show() == Navigation.MAIN_MENU) {
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
        Screen.subtitle("Registrar profesor");
        Teacher teacher = new Teacher();
        personForm.readDocument(teacher, TeacherController.DOCUMENT_TYPES, null);
        if (controller.findByDocument(teacher.getDocumentNumber()).isPresent()) {
            Screen.error("Ya existe un profesor con el documento " + teacher.getDocumentNumber() + ".");
            return;
        }
        readTeacherData(teacher, null);
        Screen.result(controller.register(teacher));
    }

    private void showCard() throws DataAccessException {
        Optional<Teacher> found = selector.ask();
        if (found.isEmpty()) {
            return;
        }
        Teacher teacher = found.get();
        Screen.subtitle("Ficha del profesor");
        System.out.println("Id: " + teacher.getId());
        System.out.println("Documento: " + teacher.getDocument());
        System.out.println("Nombre: " + teacher.getFullName());
        System.out.println("Especialización: " + Formats.text(teacher.getSpecialization()));
        System.out.println("Correo: " + teacher.getEmail());
        System.out.println("Teléfono: " + Formats.text(teacher.getPhone()));
        System.out.println("Fecha de contratación: " + Formats.date(teacher.getHireDate()));
        System.out.println("Estado: " + teacher.getStatus().getDatabaseValue());
        assignmentView.printLoad(teacher, AcademicYear.current());
    }

    private void list() throws DataAccessException {
        List<TeacherStatus> filters = new ArrayList<>();
        filters.add(null);
        filters.addAll(List.of(TeacherStatus.values()));
        TeacherStatus status = input.readChoice("Estado de los profesores", filters,
                filter -> filter == null ? "Todos" : filter.getDatabaseValue(), null);
        List<Teacher> teachers = controller.list(status);
        if (teachers.isEmpty()) {
            System.out.println("No se encontraron profesores.");
            return;
        }
        String format = "%-4s %-22s %-" + NAME_COLUMN_WIDTH + "s %-" + SPECIALIZATION_COLUMN_WIDTH + "s %-9s%n";
        System.out.printf(format, "Id", "Documento", "Nombre", "Especialización", "Estado");
        teachers.forEach(teacher -> System.out.printf(format, teacher.getId(), teacher.getDocument(),
                Screen.fit(teacher.getFullName(), NAME_COLUMN_WIDTH),
                Screen.fit(teacher.getSpecialization(), SPECIALIZATION_COLUMN_WIDTH),
                teacher.getStatus().getDatabaseValue()));
        System.out.println("Total: " + teachers.size() + " profesor(es).");
    }

    private void update() throws DataAccessException {
        Optional<Teacher> found = selector.ask();
        if (found.isEmpty()) {
            return;
        }
        Teacher current = found.get();
        Screen.subtitle("Actualizar datos de " + current.getFullName());
        PersonForm.printEditHint();
        Teacher edited = new Teacher();
        edited.setId(current.getId());
        personForm.readDocument(edited, TeacherController.DOCUMENT_TYPES, current);
        readTeacherData(edited, current);
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<Teacher> found = selector.ask();
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar a " + found.get().getFullName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    /** @param current teacher being edited, or null when it is new (new teachers start Activo) */
    private void readTeacherData(Teacher teacher, Teacher current) {
        personForm.readNames(teacher, current);
        teacher.setEmail(input.readText("Correo electrónico", Person.EMAIL_MAX_LENGTH, true, Validator::isEmail,
                Validator.EMAIL_MESSAGE, valueOf(current, Teacher::getEmail)));
        teacher.setPhone(personForm.readPhone("Teléfono (opcional)", false, valueOf(current, Teacher::getPhone)));
        teacher.setSpecialization(input.readText("Especialización (ej: Matemáticas)",
                Teacher.SPECIALIZATION_MAX_LENGTH, true, null, null, valueOf(current, Teacher::getSpecialization)));
        teacher.setHireDate(input.readDate("Fecha de contratación", true, valueOf(current, Teacher::getHireDate)));
        if (current != null) {
            teacher.setStatus(input.readChoice("Estado", List.of(TeacherStatus.values()),
                    TeacherStatus::getDatabaseValue, current.getStatus()));
        }
    }

    private static <T> T valueOf(Teacher current, Function<Teacher, T> getter) {
        return current == null ? null : getter.apply(current);
    }
}
