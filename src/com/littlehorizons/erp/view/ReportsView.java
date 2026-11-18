package com.littlehorizons.erp.view;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.util.InputReader;

/**
 * Module 7 of the main menu: brings together the reports the statement asks for in the other modules.
 * Each report is shown by the view of its module, so it is written only once.
 */
public class ReportsView implements MenuView {

    private final InputReader input;
    private final AttendanceView attendanceView;
    private final AcademicRecordView academicRecordView;
    private final FinanceView financeView;
    private final ResourceView resourceView;
    private final EventView eventView;

    public ReportsView(InputReader input, AttendanceView attendanceView, AcademicRecordView academicRecordView,
                       FinanceView financeView, ResourceView resourceView, EventView eventView) {
        this.input = input;
        this.attendanceView = attendanceView;
        this.academicRecordView = academicRecordView;
        this.financeView = financeView;
        this.resourceView = resourceView;
        this.eventView = eventView;
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Reportes y Estadísticas");
            System.out.println("1. Inasistencias de una semana");
            System.out.println("2. Promedio del curso en cada materia");
            System.out.println("3. Estudiantes morosos");
            System.out.println("4. Préstamos vencidos");
            System.out.println("5. Participación en eventos");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 5);
            try {
                switch (option) {
                    case 1 -> attendanceView.absenceReport();
                    case 2 -> academicRecordView.showCourseAverages();
                    case 3 -> financeView.delinquencyReport();
                    case 4 -> resourceView.overdueReport();
                    case 5 -> eventView.participationReport();
                    default -> {
                    }
                }
            } catch (DataAccessException e) {
                Screen.error(e.getMessage());
            }
        } while (option != 0);
        return Navigation.BACK;
    }
}
