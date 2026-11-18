package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.AbsenceSummary;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Daily attendance by subject and the absence report, inside Gestión Académica. */
public class AttendanceView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int NAME_COLUMN_WIDTH = 38;
    private static final int MAX_ABSENCES_LIMIT = 50;

    private final InputReader input;
    private final AttendanceController controller;
    private final TeacherSelector teacherSelector;
    private final AssignmentSelector assignmentSelector;
    private final StudentSelector studentSelector;

    public AttendanceView(InputReader input, AttendanceController controller, TeacherController teacherController,
                          AssignmentController assignmentController, StudentController studentController) {
        this.input = input;
        this.controller = controller;
        this.teacherSelector = new TeacherSelector(input, teacherController);
        this.assignmentSelector = new AssignmentSelector(input, assignmentController);
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Asistencia");
            System.out.println("1. Tomar asistencia de una clase (profesor)");
            System.out.println("2. Consultar asistencia de un estudiante");
            System.out.println("3. Reporte de inasistencias de una semana");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión Académica");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> takeAttendance();
                    case 2 -> showStudentAttendance();
                    case 3 -> absenceReport();
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

    /** Asks for the week and the limit, and prints the report. Also used by Reportes y Estadísticas. */
    void absenceReport() throws DataAccessException {
        LocalDate date = input.readDate("Una fecha de la semana a revisar", true, null);
        int maxAbsences = input.readInt("Mostrar estudiantes con más de cuántas ausencias en la semana (ej: 3): ",
                0, MAX_ABSENCES_LIMIT);
        List<AbsenceSummary> report = controller.absenceReport(date, maxAbsences);
        System.out.println();
        System.out.println("Semana del " + Formats.date(AttendanceController.weekStart(date)) + " al "
                + Formats.date(AttendanceController.weekEnd(date)) + ": estudiantes con más de " + maxAbsences
                + " ausencias.");
        if (report.isEmpty()) {
            System.out.println("  Ninguno.");
            return;
        }
        String format = "  %-22s %-" + NAME_COLUMN_WIDTH + "s %-10s %s%n";
        System.out.printf(format, "Documento", "Nombre", "Ausencias", "Materias");
        report.forEach(row -> System.out.printf(format, row.student().getDocument(),
                Screen.fit(row.student().getFullName(), NAME_COLUMN_WIDTH), row.absences(),
                String.join(", ", row.subjects())));
    }

    /** The teacher chooses the date and one of its groups, and marks each student. */
    private void takeAttendance() throws DataAccessException {
        Optional<Teacher> teacher = teacherSelector.ask();
        if (teacher.isEmpty()) {
            return;
        }
        LocalDate date = input.readDate("Fecha de la clase", true, null);
        Optional<Assignment> assignment = assignmentSelector.choose(teacher.get(), date.getYear());
        if (assignment.isEmpty()) {
            return;
        }
        List<Student> students = controller.listStudents(assignment.get(), date);
        if (students.isEmpty()) {
            Screen.error("No hay estudiantes inscritos en " + assignment.get().getDisplayName() + ".");
            return;
        }
        Map<Integer, AttendanceStatus> taken = controller.currentAttendance(assignment.get(), date);
        Screen.subtitle(assignment.get().getDisplayName() + " - " + Formats.date(date));
        System.out.println("P = Presente, A = Ausente, T = Tardanza, E = Excusa Médica. "
                + "Enter conserva el estado ya tomado o marca Presente.");
        Map<Integer, AttendanceStatus> statuses = new LinkedHashMap<>();
        for (Student student : students) {
            AttendanceStatus current = taken.getOrDefault(student.getId(), AttendanceStatus.PRESENT);
            String letter = input.readText("  " + student.getFullName(), 1, true,
                    value -> AttendanceStatus.fromShortcut(value).isPresent(), "Escriba P, A, T o E.",
                    current.getShortcut());
            statuses.put(student.getId(), AttendanceStatus.fromShortcut(letter).orElse(current));
        }
        Screen.result(controller.saveAttendance(assignment.get().getId(), date, statuses));
    }

    private void showStudentAttendance() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        LocalDate from = input.readDate("Desde", true, null);
        LocalDate to = input.readDate("Hasta", true, null);
        while (to.isBefore(from)) {
            Screen.error("La fecha final no puede ser anterior a la inicial (" + Formats.date(from) + ").");
            to = input.readDate("Hasta", true, null);
        }
        List<Attendance> attendances = controller.studentAttendance(student.get().getId(), from, to);
        System.out.println();
        System.out.println("Asistencia de " + student.get().getFullName() + " del " + Formats.date(from) + " al "
                + Formats.date(to) + ":");
        if (attendances.isEmpty()) {
            System.out.println("  Sin registros.");
            return;
        }
        attendances.forEach(attendance -> System.out.printf("  %s  %-36s %s%n", Formats.date(attendance.getDate()),
                attendance.getSubject().getCode() + " " + attendance.getSubject().getName(),
                attendance.getStatus().getDatabaseValue()));
        String summary = controller.countByStatus(attendances).entrySet().stream()
                .map(entry -> entry.getKey().getDatabaseValue() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        System.out.println("Resumen: " + summary + ".");
    }
}
