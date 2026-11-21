package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicReportController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.SubjectPerformance;
import com.littlehorizons.erp.util.InputReader;

import java.util.ArrayList;
import java.util.List;

/** Academic performance report by subject, inside Gestión Académica. It is shown and saved in a text file. */
public class AcademicReportView implements MenuView {

    private static final String TITLE = "REPORTE DE RENDIMIENTO ACADÉMICO POR MATERIA";
    private static final int TITLE_PADDING = 5;
    private static final String LINE = "═".repeat(60);

    private final InputReader input;
    private final AcademicReportController controller;

    public AcademicReportView(InputReader input, AcademicReportController controller) {
        this.input = input;
        this.controller = controller;
    }

    @Override
    public Navigation show() {
        List<SubjectPerformance> averages;
        try {
            averages = controller.subjectAverages();
        } catch (DataAccessException e) {
            Screen.error(e.getMessage());
            return Navigation.BACK;
        }
        if (averages.isEmpty()) {
            printEmptyMessage();
            return Navigation.BACK;
        }
        List<String> report = new ArrayList<>();
        addTitle(report);
        addAverages(report, averages);
        addBestSubjects(report, controller.bestSubjects(averages));
        System.out.println();
        report.forEach(System.out::println);
        System.out.println(LINE);
        System.out.println("Reporte generado exitosamente.");
        Screen.result(controller.saveReport(report));
        System.out.println("Presione Enter para volver al menú principal...");
        System.out.println(LINE);
        input.waitForEnter();
        return Navigation.MAIN_MENU;
    }

    private static void addTitle(List<String> report) {
        int width = TITLE.length() + TITLE_PADDING * 2;
        String padding = " ".repeat(TITLE_PADDING);
        report.add("╔" + "═".repeat(width) + "╗");
        report.add("║" + padding + TITLE + padding + "║");
        report.add("╚" + "═".repeat(width) + "╝");
    }

    /** Part 1: code, name and average of every subject. */
    private static void addAverages(List<String> report, List<SubjectPerformance> averages) {
        report.add("");
        report.add(LINE);
        report.add("PROMEDIO GENERAL POR MATERIA (Ordenado de Mayor a Menor)");
        report.add(LINE);
        report.add("");
        averages.forEach(performance -> report.add(String.format("Código: %-9s | Materia: %-35s | Promedio: %s",
                performance.subject().getCode(), performance.subject().getName(), performance.average())));
    }

    /** Part 2: the best subject, or all of them if there is a tie. */
    private static void addBestSubjects(List<String> report, List<SubjectPerformance> best) {
        report.add("");
        report.add(LINE);
        report.add("  MATERIA(S) CON MEJOR RENDIMIENTO");
        report.add(LINE);
        if (best.size() == 1) {
            SubjectPerformance subject = best.get(0);
            report.add("🏆 MATERIA DESTACADA: " + subject.subject().getName());
            report.add("   Promedio General: " + subject.average());
            report.add("   Esta materia presenta el mejor rendimiento académico del colegio.");
        } else {
            report.add("");
            report.add("🏆 MATERIAS DESTACADAS (Empate):");
            report.add("");
            best.forEach(subject -> report.add(String.format("   - %-35s | Promedio: %s",
                    subject.subject().getName(), subject.average())));
            report.add("");
            report.add("   Estas materias comparten el mejor rendimiento académico del colegio.");
        }
        report.add("");
    }

    /** Without academic records there is nothing to calculate. */
    private static void printEmptyMessage() {
        System.out.println();
        System.out.println(LINE);
        System.out.println("No hay registros académicos suficientes para generar estadísticas.");
        System.out.println("Por favor, asegúrese de que existan notas registradas en el sistema.");
        System.out.println(LINE);
    }
}
