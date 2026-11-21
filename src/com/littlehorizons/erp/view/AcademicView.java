package com.littlehorizons.erp.view;

import com.littlehorizons.erp.util.InputReader;

/** Module 2 of the main menu: subjects, inscriptions, grades and attendance. */
public class AcademicView implements MenuView {

    private final InputReader input;
    private final SubjectView subjectView;
    private final SubjectEnrollmentView subjectEnrollmentView;
    private final AcademicRecordView academicRecordView;
    private final AttendanceView attendanceView;
    private final AcademicReportView academicReportView;

    public AcademicView(InputReader input, SubjectView subjectView, SubjectEnrollmentView subjectEnrollmentView,
                        AcademicRecordView academicRecordView, AttendanceView attendanceView,
                        AcademicReportView academicReportView) {
        this.input = input;
        this.subjectView = subjectView;
        this.subjectEnrollmentView = subjectEnrollmentView;
        this.academicRecordView = academicRecordView;
        this.attendanceView = attendanceView;
        this.academicReportView = academicReportView;
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Gestión Académica");
            System.out.println("1. Materias");
            System.out.println("2. Inscripción en materias");
            System.out.println("3. Calificaciones y promedios");
            System.out.println("4. Asistencia");
            System.out.println("5. Generar Reporte de Rendimiento Académico por Materia");
            System.out.println("0. Volver al menú principal");
            option = input.readInt("Seleccione una opción: ", 0, 5);
            MenuView submenu = switch (option) {
                case 1 -> subjectView;
                case 2 -> subjectEnrollmentView;
                case 3 -> academicRecordView;
                case 4 -> attendanceView;
                case 5 -> academicReportView;
                default -> null;
            };
            if (submenu != null && submenu.show() == Navigation.MAIN_MENU) {
                return Navigation.MAIN_MENU;
            }
        } while (option != 0);
        return Navigation.BACK;
    }
}
