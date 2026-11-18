package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicRecordController;
import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.ReportCard;
import com.littlehorizons.erp.model.ReportCardRow;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.SubjectAverage;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.InputReader;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Grades and averages, inside Gestión Académica. */
public class AcademicRecordView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int SUBJECT_COLUMN_WIDTH = 34;
    private static final int GRADE_MAX_LENGTH = 4;
    private static final String GRADE_RULE = "La nota debe estar entre 0.0 y 5.0 con un decimal (ejemplo: 4.5).";
    private static final String NO_VALUE = "-";

    private final InputReader input;
    private final AcademicRecordController controller;
    private final SubjectController subjectController;
    private final TeacherSelector teacherSelector;
    private final AssignmentSelector assignmentSelector;
    private final StudentSelector studentSelector;

    public AcademicRecordView(InputReader input, AcademicRecordController controller,
                              TeacherController teacherController, AssignmentController assignmentController,
                              StudentController studentController, SubjectController subjectController) {
        this.input = input;
        this.controller = controller;
        this.subjectController = subjectController;
        this.teacherSelector = new TeacherSelector(input, teacherController);
        this.assignmentSelector = new AssignmentSelector(input, assignmentController);
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Calificaciones y Promedios");
            System.out.println("1. Registrar notas de un grupo (profesor)");
            System.out.println("2. Consultar notas de un estudiante en un año");
            System.out.println("3. Historial académico de un estudiante");
            System.out.println("4. Promedio del curso en cada materia");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión Académica");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> registerGrades();
                    case 2 -> showYear();
                    case 3 -> showHistory();
                    case 4 -> showCourseAverages();
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

    /** The teacher chooses one of its groups and types the grade of each student. */
    private void registerGrades() throws DataAccessException {
        Optional<Teacher> teacher = teacherSelector.ask();
        if (teacher.isEmpty()) {
            return;
        }
        Optional<Assignment> assignment = assignmentSelector.choose(teacher.get(), Prompts.readAcademicYear(input));
        if (assignment.isEmpty()) {
            return;
        }
        AcademicPeriod period = input.readChoice("Período", List.of(AcademicPeriod.values()),
                AcademicPeriod::getDatabaseValue, null);
        List<Student> students = controller.listStudentsToGrade(assignment.get(), period);
        if (students.isEmpty()) {
            Screen.error("No hay estudiantes inscritos en " + assignment.get().getDisplayName() + " para ese período.");
            return;
        }
        Map<Integer, BigDecimal> currentGrades = controller.currentGrades(assignment.get(), period);
        Screen.subtitle(assignment.get().getDisplayName() + " - período " + period.getDatabaseValue());
        System.out.println("Escriba la nota (0.0 a 5.0). Enter conserva la nota actual o deja al estudiante sin nota.");
        Map<Integer, BigDecimal> changes = new LinkedHashMap<>();
        for (Student student : students) {
            BigDecimal current = currentGrades.get(student.getId());
            String text = input.readText("  " + student.getFullName(), GRADE_MAX_LENGTH, false,
                    value -> parseGrade(value) != null, GRADE_RULE, current == null ? null : current.toPlainString());
            BigDecimal value = text == null ? null : parseGrade(text);
            if (value != null && (current == null || value.compareTo(current) != 0)) {
                changes.put(student.getId(), value);
            }
        }
        Screen.result(controller.saveGrades(assignment.get().getId(), period, changes));
    }

    private void showYear() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        int year = Prompts.readAcademicYear(input);
        ReportCard reportCard = controller.reportCard(student.get().getId(), year);
        System.out.println();
        System.out.println("Notas de " + student.get().getFullName());
        if (reportCard.rows().isEmpty()) {
            System.out.println("Sin notas registradas en " + year + ".");
            return;
        }
        printReportCard(reportCard);
    }

    /** Every grade since the student entered the school, one table per year. */
    private void showHistory() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<ReportCard> history = controller.history(student.get().getId());
        System.out.println();
        System.out.println("Historial académico de " + student.get().getFullName());
        if (history.isEmpty()) {
            System.out.println("Sin notas registradas.");
            return;
        }
        history.forEach(this::printReportCard);
    }

    /** Also used by Reportes y Estadísticas. */
    void showCourseAverages() throws DataAccessException {
        Grade grade = input.readChoice("Grado", subjectController.listGrades(), Grade::getName, null);
        int year = Prompts.readAcademicYear(input);
        AcademicPeriod period = input.readChoice("Período", List.of(AcademicPeriod.values()),
                AcademicPeriod::getDatabaseValue, null);
        List<SubjectAverage> averages = controller.courseAverages(grade.getId(), year, period);
        System.out.println();
        System.out.println("Promedio del curso " + grade.getName() + " " + year + ", período " + period.getDatabaseValue());
        if (averages.isEmpty()) {
            System.out.println("Sin notas registradas.");
            return;
        }
        String format = "  %-" + SUBJECT_COLUMN_WIDTH + "s %-9s %-6s %-9s%n";
        System.out.printf(format, "Materia", "Promedio", "Notas", "Bajo 3.0");
        averages.forEach(average -> System.out.printf(format,
                Screen.fit(average.subject().getCode() + " " + average.subject().getName(), SUBJECT_COLUMN_WIDTH),
                average.average().toPlainString(), average.grades(), average.belowPassing()));
    }

    private void printReportCard(ReportCard reportCard) {
        System.out.println();
        System.out.println("Año " + reportCard.academicYear() + ":");
        StringBuilder header = new StringBuilder(String.format("  %-" + SUBJECT_COLUMN_WIDTH + "s", "Materia"));
        for (AcademicPeriod period : AcademicPeriod.values()) {
            header.append(String.format(" %-6s", period.getDatabaseValue()));
        }
        System.out.println(header.append(" Promedio"));
        for (ReportCardRow row : reportCard.rows()) {
            StringBuilder line = new StringBuilder(String.format("  %-" + SUBJECT_COLUMN_WIDTH + "s",
                    Screen.fit(row.subject().getCode() + " " + row.subject().getName(), SUBJECT_COLUMN_WIDTH)));
            for (AcademicPeriod period : AcademicPeriod.values()) {
                line.append(String.format(" %-6s", show(row.grades().get(period))));
            }
            System.out.println(line.append(" ").append(show(row.average())));
        }
        StringBuilder averages = new StringBuilder(String.format("  %-" + SUBJECT_COLUMN_WIDTH + "s",
                "Promedio general del período"));
        for (AcademicPeriod period : AcademicPeriod.values()) {
            averages.append(String.format(" %-6s", show(reportCard.periodAverages().get(period))));
        }
        System.out.println(averages);
    }

    /** Accepts a dot or a comma as decimal separator. @return the grade, or null if it is not valid */
    private static BigDecimal parseGrade(String text) {
        try {
            BigDecimal value = new BigDecimal(text.trim().replace(',', '.'));
            return AcademicRecord.isValidValue(value) ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String show(BigDecimal value) {
        return value == null ? NO_VALUE : value.toPlainString();
    }
}
