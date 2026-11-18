package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Enrollments submenu, inside Gestión de Estudiantes. */
public class EnrollmentView implements MenuView {

    private static final int NAME_COLUMN_WIDTH = 38;
    private static final int MAIN_MENU_OPTION = 9;

    private final InputReader input;
    private final EnrollmentController controller;
    private final StudentSelector studentSelector;

    public EnrollmentView(InputReader input, EnrollmentController controller, StudentController studentController) {
        this.input = input;
        this.controller = controller;
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Matrículas");
            System.out.println("1. Matricular estudiante");
            System.out.println("2. Ver historial de matrículas de un estudiante");
            System.out.println("3. Listar estudiantes por grado y año");
            System.out.println("4. Retirar estudiante");
            System.out.println("5. Graduar estudiante (grado Once)");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión de Estudiantes");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> enroll();
                    case 2 -> showHistory();
                    case 3 -> listByGradeAndYear();
                    case 4 -> withdraw();
                    case 5 -> graduate();
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

    /** Also used right after registering a student. */
    void enroll(Student student) throws DataAccessException {
        Screen.subtitle("Matricular a " + student.getFullName());
        Grade grade = input.readChoice("Grado", controller.listGrades(), Grade::getName, null);
        int year = readYear();
        LocalDate date = input.readDate("Fecha de matrícula", true, null);
        String notes = input.readOptionalText("Observaciones (opcional)", Enrollment.NOTES_MAX_LENGTH);
        Screen.result(controller.enroll(student.getId(), grade.getId(), year, date, notes));
    }

    /** Also used by the student card. */
    void printHistory(int studentId) throws DataAccessException {
        List<Enrollment> history = controller.history(studentId);
        System.out.println();
        System.out.println("Historial de matrículas:");
        if (history.isEmpty()) {
            System.out.println("  Sin matrículas registradas.");
            return;
        }
        String format = "  %-5s %-11s %-11s %-11s %-11s %s%n";
        System.out.printf(format, "Año", "Grado", "Estado", "Matrícula", "Retiro", "Observaciones");
        history.forEach(enrollment -> System.out.printf(format, enrollment.getAcademicYear(),
                enrollment.getGrade().getName(), enrollment.getStatus().getDatabaseValue(),
                Formats.date(enrollment.getEnrollmentDate()), Formats.date(enrollment.getWithdrawalDate()),
                Formats.text(enrollment.getNotes())));
    }

    private void enroll() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isPresent()) {
            enroll(student.get());
        }
    }

    private void showHistory() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isPresent()) {
            System.out.println("Estudiante: " + student.get().getFullName() + " - "
                    + student.get().getStatus().getDatabaseValue());
            printHistory(student.get().getId());
        }
    }

    private void listByGradeAndYear() throws DataAccessException {
        Grade grade = input.readChoice("Grado", controller.listGrades(), Grade::getName, null);
        int year = readYear();
        List<Enrollment> enrollments = controller.listByGradeAndYear(grade.getId(), year);
        if (enrollments.isEmpty()) {
            System.out.println("No hay estudiantes matriculados en " + grade.getName() + " " + year + ".");
            return;
        }
        Screen.subtitle(grade.getName() + " " + year);
        String format = "%-4s %-22s %-" + NAME_COLUMN_WIDTH + "s %-11s%n";
        System.out.printf(format, "Id", "Documento", "Nombre", "Matrícula");
        enrollments.forEach(enrollment -> System.out.printf(format, enrollment.getStudent().getId(),
                enrollment.getStudent().getDocument(),
                Screen.fit(enrollment.getStudent().getFullName(), NAME_COLUMN_WIDTH),
                enrollment.getStatus().getDatabaseValue()));
        String summary = controller.countByStatus(enrollments).entrySet().stream()
                .map(entry -> entry.getKey().getDatabaseValue() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        System.out.println("Total: " + enrollments.size() + " (" + summary + ").");
    }

    private void withdraw() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        Optional<Enrollment> active = controller.findActive(student.get().getId());
        if (active.isEmpty()) {
            Screen.error(student.get().getFullName() + " no tiene una matrícula activa.");
            return;
        }
        System.out.println("Matrícula activa: " + active.get().getGrade().getName() + " "
                + active.get().getAcademicYear());
        LocalDate date = input.readDate("Fecha de retiro", true, null);
        String reason = input.readText("Motivo del retiro", Enrollment.NOTES_MAX_LENGTH);
        if (input.readYesNo("¿Confirma el retiro de " + student.get().getFullName() + "?")) {
            Screen.result(controller.withdraw(student.get().getId(), date, reason));
        } else {
            System.out.println("Retiro cancelado.");
        }
    }

    private void graduate() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Confirma el grado de " + student.get().getFullName() + "?")) {
            Screen.result(controller.graduate(student.get().getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private int readYear() {
        return Prompts.readAcademicYear(input);
    }
}
