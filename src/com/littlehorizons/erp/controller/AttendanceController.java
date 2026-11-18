package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDAO;
import com.littlehorizons.erp.dao.AttendanceDAO;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.EnrollmentDAO;
import com.littlehorizons.erp.dao.SubjectEnrollmentDAO;
import com.littlehorizons.erp.model.AbsenceSummary;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.AttendanceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Daily attendance by subject: the teacher takes it for the students of its group, and the report shows
 * the students with worrying absences in a week.
 */
public class AttendanceController {

    private static final int LAST_DAY_OF_WEEK = 6;

    private final AttendanceDAO attendanceDAO;
    private final AssignmentDAO assignmentDAO;
    private final SubjectEnrollmentDAO inscriptionDAO;
    private final EnrollmentDAO enrollmentDAO;

    public AttendanceController(AttendanceDAO attendanceDAO, AssignmentDAO assignmentDAO,
                                SubjectEnrollmentDAO inscriptionDAO, EnrollmentDAO enrollmentDAO) {
        this.attendanceDAO = attendanceDAO;
        this.assignmentDAO = assignmentDAO;
        this.inscriptionDAO = inscriptionDAO;
        this.enrollmentDAO = enrollmentDAO;
    }

    /**
     * Students of the group who attended classes on that date. A retired student keeps its inscriptions as
     * history: it appears only for dates up to its withdrawal, so that attendance can still be corrected.
     */
    public List<Student> listStudents(Assignment assignment, LocalDate date) throws DataAccessException {
        List<Student> students = new ArrayList<>();
        for (Student student : inscriptionDAO.findStudentsOfGroup(assignment.getSubject().getId(),
                assignment.getAcademicYear(), assignment.getSection(), null)) {
            if (student.getStatus() != StudentStatus.RETIRED
                    || attendedUntil(student, assignment.getAcademicYear(), date)) {
                students.add(student);
            }
        }
        return students;
    }

    /** @return true if the retired student was still enrolled on that date */
    private boolean attendedUntil(Student student, int year, LocalDate date) throws DataAccessException {
        return enrollmentDAO.findByStudentAndYear(student.getId(), year)
                .map(Enrollment::getWithdrawalDate)
                .map(withdrawal -> !date.isAfter(withdrawal))
                .orElse(false);
    }

    /** @return the attendance already taken that day in the subject, by student id */
    public Map<Integer, AttendanceStatus> currentAttendance(Assignment assignment, LocalDate date)
            throws DataAccessException {
        return attendanceDAO.findBySubjectAndDate(assignment.getSubject().getId(), date).stream()
                .collect(Collectors.toMap(attendance -> attendance.getStudent().getId(), Attendance::getStatus));
    }

    /** @param statuses status of each student, by student id */
    public OperationResult saveAttendance(int assignmentId, LocalDate date, Map<Integer, AttendanceStatus> statuses)
            throws DataAccessException {
        Optional<Assignment> found = assignmentDAO.findById(assignmentId);
        if (found.isEmpty()) {
            return OperationResult.error("No existe la asignación.");
        }
        Assignment assignment = found.get();
        if (date == null || date.isAfter(LocalDate.now())) {
            return OperationResult.error("La fecha es obligatoria y no puede ser futura.");
        }
        if (date.getYear() != assignment.getAcademicYear()) {
            return OperationResult.error("La fecha debe ser del año " + assignment.getAcademicYear()
                    + ", el año de la asignación.");
        }
        if (statuses.isEmpty()) {
            return OperationResult.error("No hay asistencia para guardar.");
        }
        Set<Integer> groupStudents = listStudents(assignment, date).stream()
                .map(Student::getId)
                .collect(Collectors.toSet());
        if (!groupStudents.containsAll(statuses.keySet())) {
            return OperationResult.error("Solo se toma asistencia a los estudiantes inscritos en el grupo.");
        }

        List<Attendance> attendances = statuses.entrySet().stream().map(entry -> {
            Student student = new Student();
            student.setId(entry.getKey());
            Attendance attendance = new Attendance();
            attendance.setStudent(student);
            attendance.setSubject(assignment.getSubject());
            attendance.setDate(date);
            attendance.setStatus(entry.getValue());
            attendance.setRecordedById(assignment.getTeacher().getId());
            return attendance;
        }).toList();
        attendanceDAO.saveAll(attendances);
        String summary = countByStatus(attendances).entrySet().stream()
                .map(entry -> entry.getKey().getDatabaseValue() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));
        return OperationResult.ok("Asistencia guardada para " + attendances.size() + " estudiante(s) (" + summary + ").");
    }

    public List<Attendance> studentAttendance(int studentId, LocalDate from, LocalDate to) throws DataAccessException {
        return attendanceDAO.findByStudent(studentId, from, to);
    }

    public Map<AttendanceStatus, Long> countByStatus(List<Attendance> attendances) {
        return attendances.stream().collect(Collectors.groupingBy(Attendance::getStatus,
                () -> new EnumMap<>(AttendanceStatus.class), Collectors.counting()));
    }

    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static LocalDate weekEnd(LocalDate date) {
        return weekStart(date).plusDays(LAST_DAY_OF_WEEK);
    }

    /**
     * Students with more than maxAbsences absences in the week (Monday to Sunday) that contains the date.
     * Late arrivals and medical excuses do not count as absences.
     */
    public List<AbsenceSummary> absenceReport(LocalDate dayOfWeek, int maxAbsences) throws DataAccessException {
        List<Attendance> absences = attendanceDAO.findByStatus(AttendanceStatus.ABSENT, weekStart(dayOfWeek),
                weekEnd(dayOfWeek));
        Map<Integer, List<Attendance>> byStudent = absences.stream()
                .collect(Collectors.groupingBy(absence -> absence.getStudent().getId(), LinkedHashMap::new,
                        Collectors.toList()));
        return byStudent.values().stream()
                .filter(studentAbsences -> studentAbsences.size() > maxAbsences)
                .map(studentAbsences -> new AbsenceSummary(studentAbsences.get(0).getStudent(), studentAbsences.size(),
                        studentAbsences.stream().map(absence -> absence.getSubject().getCode()).distinct().toList()))
                .sorted(Comparator.comparingLong(AbsenceSummary::absences).reversed())
                .toList();
    }
}
