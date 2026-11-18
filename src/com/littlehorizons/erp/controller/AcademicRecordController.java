package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDAO;
import com.littlehorizons.erp.dao.AssignmentDAO;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.SubjectEnrollmentDAO;
import com.littlehorizons.erp.model.AcademicPeriod;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.ReportCard;
import com.littlehorizons.erp.model.ReportCardRow;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.SubjectAverage;
import com.littlehorizons.erp.model.TeacherStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Grades: the teacher registers them for the students of its groups, and the system calculates the
 * average by subject, the general average of the student in each period and the course average by subject.
 */
public class AcademicRecordController {

    private static final int AVERAGE_DECIMALS = 2;

    private final AcademicRecordDAO recordDAO;
    private final AssignmentDAO assignmentDAO;
    private final SubjectEnrollmentDAO inscriptionDAO;

    public AcademicRecordController(AcademicRecordDAO recordDAO, AssignmentDAO assignmentDAO,
                                    SubjectEnrollmentDAO inscriptionDAO) {
        this.recordDAO = recordDAO;
        this.assignmentDAO = assignmentDAO;
        this.inscriptionDAO = inscriptionDAO;
    }

    /**
     * For a period, the students inscribed in it (a student who retired later keeps the grades of the
     * periods it attended). For the Final period, the students inscribed in the year who did not retire.
     */
    public List<Student> listStudentsToGrade(Assignment assignment, AcademicPeriod period) throws DataAccessException {
        List<Student> students = inscriptionDAO.findStudentsOfGroup(assignment.getSubject().getId(),
                assignment.getAcademicYear(), assignment.getSection(), period.isTerm() ? period : null);
        return period.isTerm() ? students : students.stream()
                .filter(student -> student.getStatus() != StudentStatus.RETIRED)
                .toList();
    }

    /** @return the grades already registered in the subject and period, by student id */
    public Map<Integer, BigDecimal> currentGrades(Assignment assignment, AcademicPeriod period)
            throws DataAccessException {
        return recordDAO.findBySubject(assignment.getSubject().getId(), assignment.getAcademicYear(), period).stream()
                .collect(Collectors.toMap(record -> record.getStudent().getId(), AcademicRecord::getValue));
    }

    /**
     * Saves new grades and corrections. Only the teacher of the group can grade, and only its students.
     *
     * @param grades value of each student, by student id
     */
    public OperationResult saveGrades(int assignmentId, AcademicPeriod period, Map<Integer, BigDecimal> grades)
            throws DataAccessException {
        Optional<Assignment> found = assignmentDAO.findById(assignmentId);
        if (found.isEmpty() || period == null) {
            return OperationResult.error("La asignación o el período no existe.");
        }
        Assignment assignment = found.get();
        if (assignment.getTeacher().getStatus() != TeacherStatus.ACTIVE) {
            return OperationResult.error(assignment.getTeacher().getFullName() + " no está Activo; no puede registrar notas.");
        }
        if (grades.isEmpty()) {
            return OperationResult.error("No se ingresó ninguna nota nueva.");
        }
        Set<Integer> groupStudents = listStudentsToGrade(assignment, period).stream()
                .map(Student::getId)
                .collect(Collectors.toSet());
        boolean outsideGroup = grades.keySet().stream().anyMatch(studentId -> !groupStudents.contains(studentId));
        if (outsideGroup) {
            return OperationResult.error("Solo se registran notas de los estudiantes inscritos en el grupo.");
        }
        if (!grades.values().stream().allMatch(AcademicRecord::isValidValue)) {
            return OperationResult.error("Las notas deben estar entre 0.0 y 5.0 con un decimal.");
        }

        LocalDate today = LocalDate.now();
        List<AcademicRecord> records = grades.entrySet().stream().map(entry -> {
            Student student = new Student();
            student.setId(entry.getKey());
            AcademicRecord record = new AcademicRecord();
            record.setStudent(student);
            record.setSubject(assignment.getSubject());
            record.setTeacher(assignment.getTeacher());
            record.setAcademicYear(assignment.getAcademicYear());
            record.setPeriod(period);
            record.setValue(entry.getValue().setScale(1, RoundingMode.UNNECESSARY));
            record.setRecordDate(today);
            return record;
        }).toList();
        recordDAO.saveAll(records);
        return OperationResult.ok(records.size() + " nota(s) guardada(s) en " + assignment.getDisplayName()
                + ", período " + period.getDatabaseValue() + " de " + assignment.getAcademicYear() + ".");
    }

    public ReportCard reportCard(int studentId, int year) throws DataAccessException {
        List<AcademicRecord> records = recordDAO.findByStudent(studentId).stream()
                .filter(record -> record.getAcademicYear() == year)
                .toList();
        return buildReportCard(year, records);
    }

    /** @return a report card for each year since the student entered the school, most recent first */
    public List<ReportCard> history(int studentId) throws DataAccessException {
        Map<Integer, List<AcademicRecord>> byYear = recordDAO.findByStudent(studentId).stream()
                .collect(Collectors.groupingBy(AcademicRecord::getAcademicYear,
                        () -> new TreeMap<>(Comparator.reverseOrder()), Collectors.toList()));
        return byYear.entrySet().stream()
                .map(entry -> buildReportCard(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** General average of the course (all the students of the grade) in each subject. */
    public List<SubjectAverage> courseAverages(int gradeId, int year, AcademicPeriod period)
            throws DataAccessException {
        Map<Integer, List<AcademicRecord>> bySubject = recordDAO.findByGrade(gradeId, year, period).stream()
                .collect(Collectors.groupingBy(record -> record.getSubject().getId(), LinkedHashMap::new,
                        Collectors.toList()));
        return bySubject.values().stream()
                .map(records -> new SubjectAverage(records.get(0).getSubject(),
                        average(records.stream().map(AcademicRecord::getValue).toList()),
                        records.size(),
                        records.stream().filter(record -> !record.isPassing()).count()))
                .toList();
    }

    /**
     * The average of a subject is the average of its period grades (I to IV). If the subject only has the
     * Final grade (for example, the history of previous years), that grade is its average.
     */
    private static ReportCard buildReportCard(int year, List<AcademicRecord> records) {
        Map<Integer, List<AcademicRecord>> bySubject = records.stream()
                .collect(Collectors.groupingBy(record -> record.getSubject().getId(), LinkedHashMap::new,
                        Collectors.toList()));
        List<ReportCardRow> rows = bySubject.values().stream()
                .map(subjectRecords -> {
                    Map<AcademicPeriod, BigDecimal> grades = subjectRecords.stream()
                            .collect(Collectors.toMap(AcademicRecord::getPeriod, AcademicRecord::getValue,
                                    (first, second) -> first, () -> new EnumMap<>(AcademicPeriod.class)));
                    List<BigDecimal> termGrades = grades.entrySet().stream()
                            .filter(entry -> entry.getKey().isTerm())
                            .map(Map.Entry::getValue)
                            .toList();
                    BigDecimal subjectAverage = termGrades.isEmpty()
                            ? grades.get(AcademicPeriod.FINAL) : average(termGrades);
                    return new ReportCardRow(subjectRecords.get(0).getSubject(), grades, subjectAverage);
                })
                .sorted(Comparator.comparing(row -> row.subject().getCode()))
                .toList();
        Map<AcademicPeriod, BigDecimal> periodAverages = records.stream()
                .collect(Collectors.groupingBy(AcademicRecord::getPeriod, () -> new EnumMap<>(AcademicPeriod.class),
                        Collectors.collectingAndThen(Collectors.mapping(AcademicRecord::getValue, Collectors.toList()),
                                AcademicRecordController::average)));
        return new ReportCard(year, rows, periodAverages);
    }

    /** @return the average with two decimals, or null if there are no values */
    static BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return null;
        }
        BigDecimal sum = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), AVERAGE_DECIMALS, RoundingMode.HALF_UP);
    }
}
