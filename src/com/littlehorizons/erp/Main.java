package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicRecordController;
import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.controller.AttendanceController;
import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.EnrollmentController;
import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.GuardianController;
import com.littlehorizons.erp.controller.LoanController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.PaymentConceptController;
import com.littlehorizons.erp.controller.PaymentController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.controller.SubjectEnrollmentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.AcademicRecordDAO;
import com.littlehorizons.erp.dao.AssignmentDAO;
import com.littlehorizons.erp.dao.AttendanceDAO;
import com.littlehorizons.erp.dao.ClubDAO;
import com.littlehorizons.erp.dao.EnrollmentDAO;
import com.littlehorizons.erp.dao.EventDAO;
import com.littlehorizons.erp.dao.EventParticipantDAO;
import com.littlehorizons.erp.dao.GradeDAO;
import com.littlehorizons.erp.dao.GuardianDAO;
import com.littlehorizons.erp.dao.InvoiceDAO;
import com.littlehorizons.erp.dao.LoanDAO;
import com.littlehorizons.erp.dao.ResourceDAO;
import com.littlehorizons.erp.dao.MembershipDAO;
import com.littlehorizons.erp.dao.PaymentConceptDAO;
import com.littlehorizons.erp.dao.PaymentDAO;
import com.littlehorizons.erp.dao.ScheduleDAO;
import com.littlehorizons.erp.dao.StudentConceptValueDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.StudentGuardianDAO;
import com.littlehorizons.erp.dao.SubjectDAO;
import com.littlehorizons.erp.dao.SubjectEnrollmentDAO;
import com.littlehorizons.erp.dao.TeacherDAO;
import com.littlehorizons.erp.service.InvoiceService;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.view.AcademicRecordView;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.AssignmentView;
import com.littlehorizons.erp.view.AttendanceView;
import com.littlehorizons.erp.view.ClubView;
import com.littlehorizons.erp.view.EnrollmentView;
import com.littlehorizons.erp.view.EventView;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.GuardianView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.MenuView;
import com.littlehorizons.erp.view.PaymentConceptView;
import com.littlehorizons.erp.view.ReportsView;
import com.littlehorizons.erp.view.ResourceView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.SubjectEnrollmentView;
import com.littlehorizons.erp.view.SubjectView;
import com.littlehorizons.erp.view.TeacherView;

import java.sql.SQLException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Entry point: checks the database connection, builds the modules and opens the main menu. */
public class Main {

    public static void main(String[] args) {
        if (!isDatabaseAvailable()) {
            return;
        }
        try (Scanner scanner = new Scanner(System.in)) {
            InputReader input = new InputReader(scanner);
            new MainMenuView(input, buildModules(input)).show();
        } catch (NoSuchElementException e) {
            // The input was closed (Ctrl+Z or the end of a redirected file): there is nothing left to read.
            System.out.println("\nEntrada finalizada. Hasta luego.");
        }
    }

    /** Each view receives its controllers, and each controller receives its DAOs. */
    private static Map<Integer, MenuView> buildModules(InputReader input) {
        StudentDAO studentDAO = new StudentDAO();
        GuardianDAO guardianDAO = new GuardianDAO();
        StudentGuardianDAO studentGuardianDAO = new StudentGuardianDAO();

        StudentController studentController = new StudentController(studentDAO, guardianDAO, studentGuardianDAO);
        GuardianController guardianController = new GuardianController(guardianDAO, studentDAO, studentGuardianDAO);
        EnrollmentController enrollmentController =
                new EnrollmentController(new EnrollmentDAO(), studentDAO, new GradeDAO());

        GuardianView guardianView = new GuardianView(input, guardianController, studentController);
        EnrollmentView enrollmentView = new EnrollmentView(input, enrollmentController, studentController);
        StudentView studentView = new StudentView(input, studentController, guardianView, enrollmentView);

        TeacherDAO teacherDAO = new TeacherDAO();
        SubjectDAO subjectDAO = new SubjectDAO();
        TeacherController teacherController = new TeacherController(teacherDAO);
        SubjectController subjectController = new SubjectController(subjectDAO, new GradeDAO());
        AssignmentController assignmentController =
                new AssignmentController(new AssignmentDAO(), new ScheduleDAO(), teacherDAO, subjectDAO);
        AssignmentView assignmentView =
                new AssignmentView(input, assignmentController, teacherController, subjectController);
        TeacherView teacherView = new TeacherView(input, teacherController, assignmentView);

        SubjectEnrollmentDAO subjectEnrollmentDAO = new SubjectEnrollmentDAO();
        AssignmentDAO assignmentDAO = new AssignmentDAO();
        SubjectEnrollmentController subjectEnrollmentController = new SubjectEnrollmentController(
                subjectEnrollmentDAO, new EnrollmentDAO(), subjectDAO, new GradeDAO());
        AcademicRecordController academicRecordController =
                new AcademicRecordController(new AcademicRecordDAO(), assignmentDAO, subjectEnrollmentDAO);
        AttendanceController attendanceController =
                new AttendanceController(new AttendanceDAO(), assignmentDAO, subjectEnrollmentDAO, new EnrollmentDAO());
        AcademicRecordView academicRecordView = new AcademicRecordView(input, academicRecordController,
                teacherController, assignmentController, studentController, subjectController);
        AttendanceView attendanceView = new AttendanceView(input, attendanceController, teacherController,
                assignmentController, studentController);
        AcademicView academicView = new AcademicView(input,
                new SubjectView(input, subjectController),
                new SubjectEnrollmentView(input, subjectEnrollmentController, studentController, subjectController),
                academicRecordView, attendanceView);

        ClubController clubController = new ClubController(new ClubDAO(), new MembershipDAO(), teacherDAO, studentDAO);
        EventController eventController =
                new EventController(new EventDAO(), new EventParticipantDAO(), teacherDAO, studentDAO);
        EventView eventView = new EventView(input, eventController, teacherController, studentController);
        ExtracurricularView extracurricularView = new ExtracurricularView(input,
                new ClubView(input, clubController, teacherController, studentController), eventView);

        PaymentConceptDAO paymentConceptDAO = new PaymentConceptDAO();
        StudentConceptValueDAO studentConceptValueDAO = new StudentConceptValueDAO();
        PaymentConceptController paymentConceptController =
                new PaymentConceptController(paymentConceptDAO, studentConceptValueDAO, studentDAO);
        PaymentController paymentController = new PaymentController(new PaymentDAO(), new InvoiceDAO(),
                paymentConceptDAO, studentConceptValueDAO, studentDAO, studentGuardianDAO, new EnrollmentDAO(),
                new InvoiceService());
        FinanceView financeView = new FinanceView(input, paymentController, paymentConceptController,
                guardianController, studentController,
                new PaymentConceptView(input, paymentConceptController, studentController));

        ResourceDAO resourceDAO = new ResourceDAO();
        ResourceView resourceView = new ResourceView(input, new ResourceController(resourceDAO),
                new LoanController(new LoanDAO(), resourceDAO, studentDAO, teacherDAO), studentController,
                teacherController);

        ReportsView reportsView = new ReportsView(input, attendanceView, academicRecordView, financeView, resourceView,
                eventView);

        return Map.of(1, studentView, 2, academicView, 3, teacherView, 4, extracurricularView, 5, financeView,
                6, resourceView, 7, reportsView);
    }

    private static boolean isDatabaseAvailable() {
        try {
            DatabaseConnection.getInstance().testConnection();
            return true;
        } catch (SQLException e) {
            ErrorLogger.log("Main.isDatabaseAvailable", e);
            System.out.println("No se pudo conectar a la base de datos: " + e.getMessage());
            System.out.println("El detalle quedó guardado en " + ErrorLogger.LOG_FILE + ".");
            return false;
        }
    }
}
