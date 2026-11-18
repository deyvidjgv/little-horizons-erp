package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AssignmentDAO;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.ScheduleDAO;
import com.littlehorizons.erp.dao.SubjectDAO;
import com.littlehorizons.erp.dao.TeacherDAO;
import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.model.Weekday;
import com.littlehorizons.erp.util.Formats;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Academic load of the teachers: which subjects each one gives (a subject can have several teachers,
 * one per group) and on which days and hours, without two classes of the same teacher at the same time.
 */
public class AssignmentController {

    private final AssignmentDAO assignmentDAO;
    private final ScheduleDAO scheduleDAO;
    private final TeacherDAO teacherDAO;
    private final SubjectDAO subjectDAO;

    public AssignmentController(AssignmentDAO assignmentDAO, ScheduleDAO scheduleDAO, TeacherDAO teacherDAO,
                                SubjectDAO subjectDAO) {
        this.assignmentDAO = assignmentDAO;
        this.scheduleDAO = scheduleDAO;
        this.teacherDAO = teacherDAO;
        this.subjectDAO = subjectDAO;
    }

    public List<Assignment> listAssignments(int teacherId, int year) throws DataAccessException {
        return assignmentDAO.findByTeacherAndYear(teacherId, year);
    }

    public List<Assignment> listTeachersOfSubject(int subjectId, int year) throws DataAccessException {
        return assignmentDAO.findBySubjectAndYear(subjectId, year);
    }

    public List<ClassSchedule> listSchedules(int assignmentId) throws DataAccessException {
        return scheduleDAO.findByAssignment(assignmentId);
    }

    /** @return the classes of the teacher in that year, from Monday to Friday and by hour */
    public List<ClassSchedule> weeklySchedule(int teacherId, int year) throws DataAccessException {
        return scheduleDAO.findByTeacherAndYear(teacherId, year);
    }

    /** @return the minutes of class of the teacher in the week */
    public long weeklyMinutes(List<ClassSchedule> schedules) {
        return schedules.stream().mapToLong(ClassSchedule::getDurationMinutes).sum();
    }

    public OperationResult assign(int teacherId, int subjectId, int year, String section) throws DataAccessException {
        Optional<Teacher> teacher = teacherDAO.findById(teacherId);
        Optional<Subject> subject = subjectDAO.findById(subjectId);
        if (teacher.isEmpty() || subject.isEmpty()) {
            return OperationResult.error("El profesor o la materia no existe.");
        }
        if (teacher.get().getStatus() != TeacherStatus.ACTIVE) {
            return OperationResult.error(teacher.get().getFullName() + " no está Activo (estado: "
                    + teacher.get().getStatus().getDatabaseValue() + "); no se le pueden asignar materias.");
        }
        if (!subject.get().isActive()) {
            return OperationResult.error("La materia " + subject.get().getCode() + " no está activa.");
        }
        if (!AcademicYear.isValid(year)) {
            return OperationResult.error("El año académico debe estar entre " + AcademicYear.FIRST + " y "
                    + AcademicYear.last() + ".");
        }
        String group = Assignment.normalizeSection(section);
        if (!Assignment.isValidSection(group)) {
            return OperationResult.error(Assignment.SECTION_RULE);
        }
        // One teacher per group: other sections of the same subject can have other teachers.
        Optional<Assignment> taken = assignmentDAO.findBySubjectYearAndSection(subjectId, year, group);
        if (taken.isPresent()) {
            return OperationResult.error(taken.get().getDisplayName() + " ya la dicta "
                    + taken.get().getTeacher().getFullName() + " en " + year + ".");
        }

        Assignment assignment = new Assignment();
        assignment.setTeacher(teacher.get());
        assignment.setSubject(subject.get());
        assignment.setAcademicYear(year);
        assignment.setSection(group);
        assignment.setId(assignmentDAO.create(assignment));
        return OperationResult.ok("Asignada " + assignment.getDisplayName() + " a " + teacher.get().getFullName()
                + " para " + year + ".");
    }

    public OperationResult removeAssignment(int assignmentId) throws DataAccessException {
        Optional<Assignment> assignment = assignmentDAO.findById(assignmentId);
        if (assignment.isEmpty()) {
            return OperationResult.error("No existe la asignación.");
        }
        int schedules = scheduleDAO.findByAssignment(assignmentId).size();
        return assignmentDAO.delete(assignmentId)
                ? OperationResult.ok("Asignación eliminada: " + assignment.get().getDisplayName()
                + (schedules > 0 ? " (con sus " + schedules + " horarios)." : "."))
                : OperationResult.error("No existe la asignación.");
    }

    public OperationResult addSchedule(int assignmentId, Weekday day, LocalTime start, LocalTime end)
            throws DataAccessException {
        Optional<Assignment> found = assignmentDAO.findById(assignmentId);
        if (found.isEmpty()) {
            return OperationResult.error("No existe la asignación.");
        }
        Assignment assignment = found.get();
        if (day == null || start == null || end == null) {
            return OperationResult.error("El día, la hora de inicio y la hora de fin son obligatorios.");
        }
        if (!start.isBefore(end)) {
            return OperationResult.error("La hora de inicio debe ser anterior a la hora de fin.");
        }
        Teacher teacher = assignment.getTeacher();
        Optional<ClassSchedule> conflict = scheduleDAO.findByTeacherAndYear(teacher.getId(),
                        assignment.getAcademicYear()).stream()
                .filter(schedule -> schedule.overlaps(day, start, end))
                .findFirst();
        if (conflict.isPresent()) {
            ClassSchedule other = conflict.get();
            return OperationResult.error("Cruce de horario: " + teacher.getFullName() + " ya tiene "
                    + other.getAssignment().getDisplayName() + " el " + other.getDay().getDatabaseValue() + " de "
                    + Formats.time(other.getStartTime()) + " a " + Formats.time(other.getEndTime()) + ".");
        }

        ClassSchedule schedule = new ClassSchedule();
        schedule.setAssignment(assignment);
        schedule.setDay(day);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setId(scheduleDAO.create(schedule));
        return OperationResult.ok("Horario agregado: " + assignment.getDisplayName() + ", " + day.getDatabaseValue()
                + " de " + Formats.time(start) + " a " + Formats.time(end) + ".");
    }

    public OperationResult removeSchedule(int scheduleId) throws DataAccessException {
        return scheduleDAO.delete(scheduleId)
                ? OperationResult.ok("Horario eliminado.")
                : OperationResult.error("No existe el horario.");
    }
}
