package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.util.List;
import java.util.Optional;

/** Catalog of subjects by grade, inside Gestión Académica. */
public class SubjectView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int NAME_COLUMN_WIDTH = 30;
    private static final int HOURS_MAX_LENGTH = 2;

    private final InputReader input;
    private final SubjectController controller;

    public SubjectView(InputReader input, SubjectController controller) {
        this.input = input;
        this.controller = controller;
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Materias");
            System.out.println("1. Registrar materia");
            System.out.println("2. Listar materias de un grado");
            System.out.println("3. Consultar materia por código");
            System.out.println("4. Actualizar materia");
            System.out.println("5. Eliminar materia");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión Académica");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> listByGrade();
                    case 3 -> showSubject();
                    case 4 -> update();
                    case 5 -> delete();
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
        Screen.subtitle("Registrar materia");
        Subject subject = new Subject();
        subject.setGrade(input.readChoice("Grado", controller.listGrades(), Grade::getName, null));
        subject.setCode(readCode(null));
        if (controller.findByCode(subject.getCode()).isPresent()) {
            Screen.error("Ya existe una materia con el código " + subject.getCode() + ".");
            return;
        }
        readSubjectData(subject, null);
        Screen.result(controller.register(subject));
    }

    private void listByGrade() throws DataAccessException {
        Grade grade = input.readChoice("Grado", controller.listGrades(), Grade::getName, null);
        List<Subject> subjects = controller.listByGrade(grade.getId());
        if (subjects.isEmpty()) {
            System.out.println("El grado " + grade.getName() + " no tiene materias.");
            return;
        }
        String format = "%-10s %-" + NAME_COLUMN_WIDTH + "s %-6s %-6s%n";
        System.out.printf(format, "Código", "Nombre", "Horas", "Activa");
        subjects.forEach(subject -> System.out.printf(format, subject.getCode(),
                Screen.fit(subject.getName(), NAME_COLUMN_WIDTH), subject.getWeeklyHours(),
                subject.isActive() ? "Sí" : "No"));
        System.out.println("Total: " + subjects.size() + " materia(s).");
    }

    private void showSubject() throws DataAccessException {
        Optional<Subject> found = askSubject();
        if (found.isEmpty()) {
            return;
        }
        Subject subject = found.get();
        Screen.subtitle("Materia " + subject.getCode());
        System.out.println("Nombre: " + subject.getName());
        System.out.println("Grado: " + subject.getGrade().getName());
        System.out.println("Horas semanales: " + subject.getWeeklyHours());
        System.out.println("Activa: " + (subject.isActive() ? "Sí" : "No"));
        System.out.println("Descripción: " + Formats.text(subject.getDescription()));
    }

    private void update() throws DataAccessException {
        Optional<Subject> found = askSubject();
        if (found.isEmpty()) {
            return;
        }
        Subject current = found.get();
        Screen.subtitle("Actualizar " + current.getDisplayName());
        PersonForm.printEditHint();
        Subject edited = new Subject();
        edited.setId(current.getId());
        edited.setCode(readCode(current.getCode()));
        edited.setGrade(input.readChoice("Grado", controller.listGrades(), Grade::getName, current.getGrade()));
        readSubjectData(edited, current);
        edited.setActive(input.readYesNo("¿Está activa?", current.isActive()));
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<Subject> found = askSubject();
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar " + found.get().getDisplayName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    private Optional<Subject> askSubject() throws DataAccessException {
        String code = input.readText("Código de la materia (ej: MAT-5)", Subject.CODE_MAX_LENGTH);
        Optional<Subject> subject = controller.findByCode(code);
        if (subject.isEmpty()) {
            Screen.error("No existe una materia con el código " + SubjectController.normalizeCode(code) + ".");
        }
        return subject;
    }

    private String readCode(String current) {
        String code = input.readText("Código (ej: MAT-5)", Subject.CODE_MAX_LENGTH, true,
                value -> SubjectController.isValidCode(SubjectController.normalizeCode(value)),
                SubjectController.CODE_RULE, current);
        return SubjectController.normalizeCode(code);
    }

    /** @param current subject being edited, or null when it is new */
    private void readSubjectData(Subject subject, Subject current) {
        subject.setName(input.readText("Nombre", Subject.NAME_MAX_LENGTH, true, null, null,
                current == null ? null : current.getName()));
        subject.setDescription(input.readText("Descripción (opcional)", Subject.DESCRIPTION_MAX_LENGTH, false,
                null, null, current == null ? null : current.getDescription()));
        String hoursRule = "Escriba un número entre " + SubjectController.MIN_WEEKLY_HOURS + " y "
                + SubjectController.MAX_WEEKLY_HOURS + ".";
        String hours = input.readText("Horas semanales", HOURS_MAX_LENGTH, true, value -> isValidHours(value),
                hoursRule, current == null ? null : String.valueOf(current.getWeeklyHours()));
        subject.setWeeklyHours(Integer.parseInt(hours));
    }

    private static boolean isValidHours(String value) {
        if (!value.matches("\\d+")) {
            return false;
        }
        int hours = Integer.parseInt(value);
        return hours >= SubjectController.MIN_WEEKLY_HOURS && hours <= SubjectController.MAX_WEEKLY_HOURS;
    }
}
