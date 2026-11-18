package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.EnrollmentDAO;
import com.littlehorizons.erp.dao.GradeDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.Formats;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rules of the enrollments: one enrollment per student and year, a graduated student is not enrolled
 * again, and only a student of grade Once can graduate.
 */
public class EnrollmentController {

    private final EnrollmentDAO enrollmentDAO;
    private final StudentDAO studentDAO;
    private final GradeDAO gradeDAO;

    public EnrollmentController(EnrollmentDAO enrollmentDAO, StudentDAO studentDAO, GradeDAO gradeDAO) {
        this.enrollmentDAO = enrollmentDAO;
        this.studentDAO = studentDAO;
        this.gradeDAO = gradeDAO;
    }

    public List<Grade> listGrades() throws DataAccessException {
        return gradeDAO.findAll();
    }

    public List<Enrollment> history(int studentId) throws DataAccessException {
        return enrollmentDAO.findByStudent(studentId);
    }

    public List<Enrollment> listByGradeAndYear(int gradeId, int year) throws DataAccessException {
        return enrollmentDAO.findByGradeAndYear(gradeId, year);
    }

    public Optional<Enrollment> findActive(int studentId) throws DataAccessException {
        return enrollmentDAO.findActiveByStudent(studentId);
    }

    /** Counts the enrollments of a list by status, for the summary under the list. */
    public Map<EnrollmentStatus, Long> countByStatus(List<Enrollment> enrollments) {
        return enrollments.stream().collect(Collectors.groupingBy(Enrollment::getStatus,
                () -> new EnumMap<>(EnrollmentStatus.class), Collectors.counting()));
    }

    public OperationResult enroll(int studentId, int gradeId, int year, LocalDate date, String notes)
            throws DataAccessException {
        Optional<Student> student = studentDAO.findById(studentId);
        Optional<Grade> grade = gradeDAO.findById(gradeId);
        if (student.isEmpty() || grade.isEmpty()) {
            return OperationResult.error("El estudiante o el grado no existe.");
        }
        String name = student.get().getFullName();
        if (student.get().getStatus() == StudentStatus.GRADUATED) {
            return OperationResult.error(name + " ya se graduó; no se puede matricular de nuevo.");
        }
        if (!AcademicYear.isValid(year)) {
            return OperationResult.error("El año académico debe estar entre " + AcademicYear.FIRST + " y "
                    + AcademicYear.last() + ".");
        }
        if (date == null || date.isAfter(LocalDate.now())) {
            return OperationResult.error("La fecha de matrícula es obligatoria y no puede ser futura.");
        }
        if (notes != null && notes.length() > Enrollment.NOTES_MAX_LENGTH) {
            return OperationResult.error("Las observaciones admiten máximo " + Enrollment.NOTES_MAX_LENGTH
                    + " caracteres.");
        }
        if (enrollmentDAO.findByStudentAndYear(studentId, year).isPresent()) {
            return OperationResult.error(name + " ya tiene una matrícula en el año " + year + ".");
        }
        Optional<Enrollment> active = enrollmentDAO.findActiveByStudent(studentId);
        if (active.isPresent() && active.get().getAcademicYear() > year) {
            return OperationResult.error(name + " tiene activa la matrícula de " + active.get().getAcademicYear()
                    + "; no se puede matricular en un año anterior.");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student.get());
        enrollment.setGrade(grade.get());
        enrollment.setAcademicYear(year);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrollmentDate(date);
        enrollment.setNotes(notes);
        enrollment.setId(enrollmentDAO.enroll(enrollment, active.orElse(null)));

        String message = "Matrícula registrada: " + name + " en " + grade.get().getName() + " " + year + ".";
        if (active.isPresent()) {
            message += " La matrícula de " + active.get().getAcademicYear() + " quedó Completada.";
        }
        return OperationResult.ok(message);
    }

    public OperationResult withdraw(int studentId, LocalDate date, String reason) throws DataAccessException {
        Optional<Enrollment> active = enrollmentDAO.findActiveByStudent(studentId);
        if (active.isEmpty()) {
            return OperationResult.error("El estudiante no tiene una matrícula activa.");
        }
        Enrollment enrollment = active.get();
        if (date == null || date.isAfter(LocalDate.now()) || date.isBefore(enrollment.getEnrollmentDate())) {
            return OperationResult.error("La fecha de retiro no puede ser futura ni anterior a la matrícula ("
                    + Formats.date(enrollment.getEnrollmentDate()) + ").");
        }
        if (reason == null || reason.isBlank()) {
            return OperationResult.error("El motivo del retiro es obligatorio.");
        }
        enrollmentDAO.withdraw(enrollment, date, addNote(enrollment.getNotes(), "Retiro: " + reason));
        return OperationResult.ok(enrollment.getStudent().getFullName() + " quedó Retirado. La matrícula de "
                + enrollment.getAcademicYear() + " quedó Retirada.");
    }

    public OperationResult graduate(int studentId) throws DataAccessException {
        Optional<Enrollment> active = enrollmentDAO.findActiveByStudent(studentId);
        if (active.isEmpty()) {
            return OperationResult.error("El estudiante no tiene una matrícula activa.");
        }
        Enrollment enrollment = active.get();
        if (enrollment.getGrade().getLevel() != Grade.LAST_LEVEL) {
            return OperationResult.error("Solo se gradúa un estudiante con matrícula activa en grado Once. "
                    + enrollment.getStudent().getFullName() + " está en " + enrollment.getGrade().getName() + ".");
        }
        String note = "Graduado como bachiller el " + Formats.date(LocalDate.now());
        enrollmentDAO.graduate(enrollment, addNote(enrollment.getNotes(), note));
        return OperationResult.ok(enrollment.getStudent().getFullName() + " quedó Graduado. La matrícula de "
                + enrollment.getAcademicYear() + " quedó Completada.");
    }

    /** Keeps the previous notes of the enrollment and adds the new one at the end. */
    private static String addNote(String currentNotes, String note) {
        return currentNotes == null || currentNotes.isBlank() ? note : currentNotes + " | " + note;
    }
}
