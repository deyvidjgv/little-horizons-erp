package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.EnrollmentDAO;
import com.littlehorizons.erp.dao.GradeDAO;
import com.littlehorizons.erp.dao.SubjectDAO;
import com.littlehorizons.erp.dao.SubjectEnrollmentDAO;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.EnrollmentStatus;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inscription of students in the subjects of their grade, for each period. The grade and the year come
 * from the active enrollment, so a student is only inscribed in subjects of its own grade.
 */
public class SubjectEnrollmentController {

    private final SubjectEnrollmentDAO inscriptionDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final SubjectDAO subjectDAO;
    private final GradeDAO gradeDAO;

    public SubjectEnrollmentController(SubjectEnrollmentDAO inscriptionDAO, EnrollmentDAO enrollmentDAO,
                                       SubjectDAO subjectDAO, GradeDAO gradeDAO) {
        this.inscriptionDAO = inscriptionDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.subjectDAO = subjectDAO;
        this.gradeDAO = gradeDAO;
    }

    public List<SubjectEnrollment> listByStudent(int studentId, int year, AcademicPeriod period)
            throws DataAccessException {
        return inscriptionDAO.findByStudent(studentId, year, period);
    }

    public List<SubjectEnrollment> listBySubject(int subjectId, int year, AcademicPeriod period)
            throws DataAccessException {
        return inscriptionDAO.findBySubject(subjectId, year, period);
    }

    /** Inscribes the student in the active subjects of its grade that it does not have yet in that period. */
    public OperationResult enrollStudent(int studentId, AcademicPeriod period, String section)
            throws DataAccessException {
        String group = Assignment.normalizeSection(section);
        String rejection = validatePeriodAndSection(period, group);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Enrollment> active = enrollmentDAO.findActiveByStudent(studentId);
        if (active.isEmpty()) {
            return OperationResult.error("El estudiante no tiene una matrícula activa; primero matricúlelo en un grado.");
        }
        Enrollment enrollment = active.get();
        Grade grade = enrollment.getGrade();
        int year = enrollment.getAcademicYear();
        List<Subject> subjects = activeSubjects(grade);
        if (subjects.isEmpty()) {
            return OperationResult.error("El grado " + grade.getName() + " no tiene materias activas.");
        }
        Set<Integer> alreadyInscribed = inscriptionDAO.findByStudent(studentId, year, period).stream()
                .map(inscription -> inscription.getSubject().getId())
                .collect(Collectors.toSet());
        List<SubjectEnrollment> missing = subjects.stream()
                .filter(subject -> !alreadyInscribed.contains(subject.getId()))
                .map(subject -> newInscription(enrollment.getStudent(), subject, year, period, group))
                .toList();
        String target = grade.getName() + ", período " + period.getDatabaseValue() + " de " + year;
        if (missing.isEmpty()) {
            return OperationResult.error("El estudiante ya está inscrito en todas las materias de " + target + ".");
        }
        inscriptionDAO.createAll(missing);
        return OperationResult.ok(enrollment.getStudent().getFullName() + " quedó inscrito en " + missing.size()
                + " materia(s) de " + target + "."
                + (alreadyInscribed.isEmpty() ? "" : " Ya tenía " + alreadyInscribed.size() + "."));
    }

    /** Inscribes every student with an active enrollment in the grade, in all the active subjects of the grade. */
    public OperationResult enrollGrade(int gradeId, int year, AcademicPeriod period, String section)
            throws DataAccessException {
        String group = Assignment.normalizeSection(section);
        String rejection = validatePeriodAndSection(period, group);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (!AcademicYear.isValid(year)) {
            return OperationResult.error("El año académico debe estar entre " + AcademicYear.FIRST + " y "
                    + AcademicYear.last() + ".");
        }
        Optional<Grade> grade = gradeDAO.findById(gradeId);
        if (grade.isEmpty()) {
            return OperationResult.error("No existe el grado.");
        }
        List<Student> students = enrollmentDAO.findByGradeAndYear(gradeId, year).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(Enrollment::getStudent)
                .toList();
        String target = grade.get().getName() + ", período " + period.getDatabaseValue() + " de " + year;
        if (students.isEmpty()) {
            return OperationResult.error("No hay estudiantes con matrícula activa en " + grade.get().getName()
                    + " " + year + ".");
        }
        List<Subject> subjects = activeSubjects(grade.get());
        if (subjects.isEmpty()) {
            return OperationResult.error("El grado " + grade.get().getName() + " no tiene materias activas.");
        }

        record Pair(int studentId, int subjectId) {
        }
        Set<Pair> existing = inscriptionDAO.findByGrade(gradeId, year, period).stream()
                .map(inscription -> new Pair(inscription.getStudent().getId(), inscription.getSubject().getId()))
                .collect(Collectors.toSet());
        List<SubjectEnrollment> missing = students.stream()
                .flatMap(student -> subjects.stream()
                        .filter(subject -> !existing.contains(new Pair(student.getId(), subject.getId())))
                        .map(subject -> newInscription(student, subject, year, period, group)))
                .toList();
        if (missing.isEmpty()) {
            return OperationResult.error("Todos los estudiantes ya estaban inscritos en las materias de " + target + ".");
        }
        inscriptionDAO.createAll(missing);
        return OperationResult.ok("Se crearon " + missing.size() + " inscripciones para " + students.size()
                + " estudiante(s) de " + target + ".");
    }

    public OperationResult withdraw(int inscriptionId) throws DataAccessException {
        Optional<SubjectEnrollment> inscription = inscriptionDAO.findById(inscriptionId);
        if (inscription.isEmpty()) {
            return OperationResult.error("No existe la inscripción.");
        }
        if (inscription.get().getStatus() == EnrollmentStatus.WITHDRAWN) {
            return OperationResult.error("Esa inscripción ya estaba retirada.");
        }
        inscriptionDAO.updateStatus(inscriptionId, EnrollmentStatus.WITHDRAWN);
        return OperationResult.ok(inscription.get().getStudent().getFullName() + " quedó retirado de "
                + inscription.get().getSubject().getDisplayName() + " en el período "
                + inscription.get().getPeriod().getDatabaseValue() + ".");
    }

    private List<Subject> activeSubjects(Grade grade) throws DataAccessException {
        return subjectDAO.findByGrade(grade.getId()).stream()
                .filter(Subject::isActive)
                .toList();
    }

    private static String validatePeriodAndSection(AcademicPeriod period, String section) {
        if (period == null || !period.isTerm()) {
            return "Las inscripciones son por período: I, II, III o IV.";
        }
        if (!Assignment.isValidSection(section)) {
            return Assignment.SECTION_RULE;
        }
        return null;
    }

    private static SubjectEnrollment newInscription(Student student, Subject subject, int year, AcademicPeriod period,
                                                    String section) {
        SubjectEnrollment inscription = new SubjectEnrollment();
        inscription.setStudent(student);
        inscription.setSubject(subject);
        inscription.setAcademicYear(year);
        inscription.setPeriod(period);
        inscription.setSection(section);
        inscription.setEnrollmentDate(LocalDate.now());
        inscription.setStatus(EnrollmentStatus.ACTIVE);
        return inscription;
    }
}
