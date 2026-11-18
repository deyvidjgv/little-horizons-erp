package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.SubjectEnrollmentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.util.InputReader;

import java.util.List;
import java.util.Optional;

/** Inscription of students in the subjects of their grade, by period, inside Gestión Académica. */
public class SubjectEnrollmentView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int NAME_COLUMN_WIDTH = 38;
    private static final String DEFAULT_SECTION = "A";

    private final InputReader input;
    private final SubjectEnrollmentController controller;
    private final SubjectController subjectController;
    private final StudentSelector studentSelector;
    private final SubjectSelector subjectSelector;

    public SubjectEnrollmentView(InputReader input, SubjectEnrollmentController controller,
                                 StudentController studentController, SubjectController subjectController) {
        this.input = input;
        this.controller = controller;
        this.subjectController = subjectController;
        this.studentSelector = new StudentSelector(input, studentController);
        this.subjectSelector = new SubjectSelector(input, subjectController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Inscripción en Materias");
            System.out.println("1. Inscribir a un estudiante en las materias de su grado");
            System.out.println("2. Inscribir a todos los estudiantes de un grado");
            System.out.println("3. Ver materias inscritas de un estudiante");
            System.out.println("4. Ver estudiantes inscritos en una materia");
            System.out.println("5. Retirar a un estudiante de una materia");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Gestión Académica");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> enrollStudent();
                    case 2 -> enrollGrade();
                    case 3 -> listOfStudent();
                    case 4 -> listOfSubject();
                    case 5 -> withdraw();
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

    /** The grade and the year are those of the student's active enrollment. */
    private void enrollStudent() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        AcademicPeriod period = readPeriod();
        String section = readSection();
        Screen.result(controller.enrollStudent(student.get().getId(), period, section));
    }

    private void enrollGrade() throws DataAccessException {
        Grade grade = input.readChoice("Grado", subjectController.listGrades(), Grade::getName, null);
        int year = Prompts.readAcademicYear(input);
        AcademicPeriod period = readPeriod();
        String section = readSection();
        String question = "¿Inscribir a todos los estudiantes de " + grade.getName() + " " + year
                + " en sus materias del período " + period.getDatabaseValue() + "?";
        if (input.readYesNo(question)) {
            Screen.result(controller.enrollGrade(grade.getId(), year, period, section));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private void listOfStudent() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        int year = Prompts.readAcademicYear(input);
        AcademicPeriod period = readPeriod();
        List<SubjectEnrollment> inscriptions = controller.listByStudent(student.get().getId(), year, period);
        System.out.println();
        System.out.println(student.get().getFullName() + " - período " + period.getDatabaseValue() + " de " + year + ":");
        if (inscriptions.isEmpty()) {
            System.out.println("  Sin materias inscritas.");
            return;
        }
        inscriptions.forEach(inscription -> System.out.printf("  %-36s sección %-4s %s%n",
                inscription.getSubject().getDisplayName(), inscription.getSection(),
                inscription.getStatus().getDatabaseValue()));
    }

    private void listOfSubject() throws DataAccessException {
        Optional<Subject> subject = subjectSelector.ask();
        if (subject.isEmpty()) {
            return;
        }
        int year = Prompts.readAcademicYear(input);
        AcademicPeriod period = readPeriod();
        List<SubjectEnrollment> inscriptions = controller.listBySubject(subject.get().getId(), year, period);
        System.out.println();
        System.out.println(subject.get().getDisplayName() + " - período " + period.getDatabaseValue() + " de " + year + ":");
        if (inscriptions.isEmpty()) {
            System.out.println("  Sin estudiantes inscritos.");
            return;
        }
        String format = "  %-22s %-" + NAME_COLUMN_WIDTH + "s %-8s %-10s%n";
        System.out.printf(format, "Documento", "Nombre", "Sección", "Estado");
        inscriptions.forEach(inscription -> System.out.printf(format, inscription.getStudent().getDocument(),
                Screen.fit(inscription.getStudent().getFullName(), NAME_COLUMN_WIDTH), inscription.getSection(),
                inscription.getStatus().getDatabaseValue()));
        System.out.println("Total: " + inscriptions.size() + " estudiante(s).");
    }

    private void withdraw() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        int year = Prompts.readAcademicYear(input);
        AcademicPeriod period = readPeriod();
        List<SubjectEnrollment> inscriptions = controller.listByStudent(student.get().getId(), year, period).stream()
                .filter(inscription -> inscription.getStatus() != EnrollmentStatus.WITHDRAWN)
                .toList();
        if (inscriptions.isEmpty()) {
            Screen.error(student.get().getFullName() + " no tiene materias inscritas en ese período.");
            return;
        }
        SubjectEnrollment chosen = input.readChoice("Materia", inscriptions,
                inscription -> inscription.getSubject().getDisplayName(), null);
        if (input.readYesNo("¿Retirar a " + student.get().getFullName() + " de " + chosen.getSubject().getName() + "?")) {
            Screen.result(controller.withdraw(chosen.getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private AcademicPeriod readPeriod() {
        return input.readChoice("Período", AcademicPeriod.terms(), AcademicPeriod::getDatabaseValue, null);
    }

    /** Most groups are section A, so Enter takes it. */
    private String readSection() {
        return input.readText("Sección o grupo", Assignment.SECTION_MAX_LENGTH, true,
                value -> Assignment.isValidSection(Assignment.normalizeSection(value)), Assignment.SECTION_RULE,
                DEFAULT_SECTION);
    }
}
