package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.Weekday;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/** Subjects and class schedules of the teachers, inside Gestión de Profesores. */
public class AssignmentView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int MINUTES_PER_HOUR = 60;

    private final InputReader input;
    private final AssignmentController controller;
    private final TeacherSelector teacherSelector;
    private final SubjectSelector subjectSelector;
    private final AssignmentSelector assignmentSelector;

    public AssignmentView(InputReader input, AssignmentController controller, TeacherController teacherController,
                          SubjectController subjectController) {
        this.input = input;
        this.controller = controller;
        this.teacherSelector = new TeacherSelector(input, teacherController);
        this.subjectSelector = new SubjectSelector(input, subjectController);
        this.assignmentSelector = new AssignmentSelector(input, controller);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Asignación de Materias y Horarios");
            System.out.println("1. Asignar materia a un profesor");
            System.out.println("2. Ver materias y horario de un profesor");
            System.out.println("3. Ver profesores de una materia");
            System.out.println("4. Quitar una asignación");
            System.out.println("5. Agregar horario de clase");
            System.out.println("6. Quitar horario de clase");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión de Profesores");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> assign();
                    case 2 -> showLoad();
                    case 3 -> showTeachersOfSubject();
                    case 4 -> removeAssignment();
                    case 5 -> addSchedule();
                    case 6 -> removeSchedule();
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

    /** Subjects and weekly schedule of the teacher. Also used by the teacher card. */
    void printLoad(Teacher teacher, int year) throws DataAccessException {
        List<Assignment> assignments = controller.listAssignments(teacher.getId(), year);
        System.out.println();
        System.out.println("Materias asignadas en " + year + ":");
        if (assignments.isEmpty()) {
            System.out.println("  Ninguna.");
            return;
        }
        assignments.forEach(assignment -> System.out.println("  - " + assignment.getDisplayName() + " ("
                + assignment.getSubject().getWeeklyHours() + " h semanales)"));

        List<ClassSchedule> schedules = controller.weeklySchedule(teacher.getId(), year);
        System.out.println();
        System.out.println("Horario semanal " + year + ":");
        if (schedules.isEmpty()) {
            System.out.println("  Sin horarios registrados.");
            return;
        }
        schedules.forEach(schedule -> System.out.printf("  %-10s %s-%s  %s%n", schedule.getDay().getDatabaseValue(),
                Formats.time(schedule.getStartTime()), Formats.time(schedule.getEndTime()),
                schedule.getAssignment().getDisplayName()));
        long minutes = controller.weeklyMinutes(schedules);
        System.out.printf("Total: %d h %02d min de clase a la semana.%n", minutes / MINUTES_PER_HOUR,
                minutes % MINUTES_PER_HOUR);
    }

    private void assign() throws DataAccessException {
        Optional<Teacher> teacher = teacherSelector.ask();
        if (teacher.isEmpty()) {
            return;
        }
        Optional<Subject> subject = subjectSelector.ask();
        if (subject.isEmpty()) {
            return;
        }
        int year = Prompts.readAcademicYear(input);
        String section = input.readText("Sección o grupo (ej: A)", Assignment.SECTION_MAX_LENGTH, true,
                value -> value.matches("[A-Za-z0-9]+"), "Use solo letras o números (ejemplo: A).", null);
        Screen.result(controller.assign(teacher.get().getId(), subject.get().getId(), year, section));
    }

    private void showLoad() throws DataAccessException {
        Optional<Teacher> teacher = teacherSelector.ask();
        if (teacher.isPresent()) {
            int year = Prompts.readAcademicYear(input);
            System.out.println("Profesor: " + teacher.get().getFullName() + " - "
                    + teacher.get().getStatus().getDatabaseValue());
            printLoad(teacher.get(), year);
        }
    }

    private void showTeachersOfSubject() throws DataAccessException {
        Optional<Subject> subject = subjectSelector.ask();
        if (subject.isEmpty()) {
            return;
        }
        int year = Prompts.readAcademicYear(input);
        List<Assignment> assignments = controller.listTeachersOfSubject(subject.get().getId(), year);
        System.out.println();
        System.out.println(subject.get().getDisplayName() + " en " + year + ":");
        if (assignments.isEmpty()) {
            System.out.println("  Ningún profesor asignado.");
        }
        assignments.forEach(assignment -> System.out.println("  - Sección " + assignment.getSection() + ": "
                + assignment.getTeacher().getFullName() + " ("
                + assignment.getTeacher().getStatus().getDatabaseValue() + ")"));
    }

    private void removeAssignment() throws DataAccessException {
        Optional<Assignment> assignment = chooseAssignment();
        if (assignment.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Quitar " + assignment.get().getDisplayName() + " y sus horarios?")) {
            Screen.result(controller.removeAssignment(assignment.get().getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private void addSchedule() throws DataAccessException {
        Optional<Assignment> assignment = chooseAssignment();
        if (assignment.isEmpty()) {
            return;
        }
        Weekday day = input.readChoice("Día", List.of(Weekday.values()), Weekday::getDatabaseValue, null);
        LocalTime start = input.readTime("Hora de inicio");
        LocalTime end = input.readTime("Hora de fin");
        Screen.result(controller.addSchedule(assignment.get().getId(), day, start, end));
    }

    private void removeSchedule() throws DataAccessException {
        Optional<Assignment> assignment = chooseAssignment();
        if (assignment.isEmpty()) {
            return;
        }
        List<ClassSchedule> schedules = controller.listSchedules(assignment.get().getId());
        if (schedules.isEmpty()) {
            Screen.error(assignment.get().getDisplayName() + " no tiene horarios registrados.");
            return;
        }
        ClassSchedule chosen = input.readChoice("Horario a quitar", schedules,
                schedule -> schedule.getDay().getDatabaseValue() + " " + Formats.time(schedule.getStartTime())
                        + "-" + Formats.time(schedule.getEndTime()), null);
        if (input.readYesNo("¿Quitar este horario?")) {
            Screen.result(controller.removeSchedule(chosen.getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    /** Asks for the teacher and the year, and lets the user choose one of the teacher's assignments. */
    private Optional<Assignment> chooseAssignment() throws DataAccessException {
        Optional<Teacher> teacher = teacherSelector.ask();
        if (teacher.isEmpty()) {
            return Optional.empty();
        }
        return assignmentSelector.choose(teacher.get(), Prompts.readAcademicYear(input));
    }
}
