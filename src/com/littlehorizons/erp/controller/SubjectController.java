package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.GradeDAO;
import com.littlehorizons.erp.dao.SubjectDAO;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Catalog of subjects by grade: unique code, valid data, and subjects with history are not deleted
 * (they are deactivated, so they are no longer offered).
 */
public class SubjectController {

    public static final int MIN_WEEKLY_HOURS = 1;
    public static final int MAX_WEEKLY_HOURS = 40;
    public static final String CODE_RULE = "El código lleva letras mayúsculas, números y guiones (ejemplo: MAT-5).";

    private static final Pattern CODE = Pattern.compile("[A-Z0-9-]{2,20}");

    private final SubjectDAO subjectDAO;
    private final GradeDAO gradeDAO;

    public SubjectController(SubjectDAO subjectDAO, GradeDAO gradeDAO) {
        this.subjectDAO = subjectDAO;
        this.gradeDAO = gradeDAO;
    }

    public List<Grade> listGrades() throws DataAccessException {
        return gradeDAO.findAll();
    }

    /** @return every subject of the grade, active or not */
    public List<Subject> listByGrade(int gradeId) throws DataAccessException {
        return subjectDAO.findByGrade(gradeId);
    }

    /** @return the subjects of the grade that are still taught (esta_activa) */
    public List<Subject> listActiveByGrade(int gradeId) throws DataAccessException {
        return subjectDAO.findByGrade(gradeId).stream()
                .filter(Subject::isActive)
                .toList();
    }

    public Optional<Subject> findByCode(String code) throws DataAccessException {
        return subjectDAO.findByCode(normalizeCode(code));
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValidCode(String code) {
        return code != null && CODE.matcher(code).matches();
    }

    public OperationResult register(Subject subject) throws DataAccessException {
        subject.setCode(normalizeCode(subject.getCode()));
        subject.setActive(true);
        String rejection = validate(subject);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (subjectDAO.findByCode(subject.getCode()).isPresent()) {
            return OperationResult.error("Ya existe una materia con el código " + subject.getCode() + ".");
        }
        subject.setId(subjectDAO.create(subject));
        return OperationResult.ok("Materia registrada: " + subject.getDisplayName() + ".");
    }

    public OperationResult update(Subject subject) throws DataAccessException {
        subject.setCode(normalizeCode(subject.getCode()));
        String rejection = validate(subject);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Subject> current = subjectDAO.findById(subject.getId());
        if (current.isEmpty()) {
            return OperationResult.error("No existe la materia.");
        }
        Optional<Subject> sameCode = subjectDAO.findByCode(subject.getCode());
        if (sameCode.isPresent() && sameCode.get().getId() != subject.getId()) {
            return OperationResult.error("Ya existe otra materia con el código " + subject.getCode() + ".");
        }
        boolean gradeChanged = current.get().getGrade().getId() != subject.getGrade().getId();
        if (gradeChanged && subjectDAO.hasHistory(subject.getId())) {
            return OperationResult.error("No se puede cambiar el grado: la materia ya tiene asignaciones, "
                    + "inscripciones, notas o asistencias. Registre una materia nueva para el otro grado.");
        }
        return subjectDAO.update(subject)
                ? OperationResult.ok("Materia actualizada: " + subject.getDisplayName() + ".")
                : OperationResult.error("No existe la materia.");
    }

    /** Only a subject without history can be deleted; otherwise the database would delete its grades too. */
    public OperationResult delete(int subjectId) throws DataAccessException {
        Optional<Subject> subject = subjectDAO.findById(subjectId);
        if (subject.isEmpty()) {
            return OperationResult.error("No existe la materia.");
        }
        if (subjectDAO.hasHistory(subjectId)) {
            return OperationResult.error("No se puede eliminar " + subject.get().getDisplayName() + ": tiene "
                    + "asignaciones, inscripciones, notas o asistencias. Desactívela con la opción Actualizar.");
        }
        return subjectDAO.delete(subjectId)
                ? OperationResult.ok("Materia eliminada: " + subject.get().getDisplayName() + ".")
                : OperationResult.error("No existe la materia.");
    }

    private String validate(Subject subject) {
        if (!isValidCode(subject.getCode())) {
            return CODE_RULE;
        }
        String name = subject.getName();
        if (name == null || name.isBlank() || name.length() > Subject.NAME_MAX_LENGTH) {
            return "El nombre es obligatorio (máximo " + Subject.NAME_MAX_LENGTH + " caracteres).";
        }
        String description = subject.getDescription();
        if (description != null && description.length() > Subject.DESCRIPTION_MAX_LENGTH) {
            return "La descripción admite máximo " + Subject.DESCRIPTION_MAX_LENGTH + " caracteres.";
        }
        if (subject.getGrade() == null) {
            return "El grado es obligatorio.";
        }
        if (subject.getWeeklyHours() < MIN_WEEKLY_HOURS || subject.getWeeklyHours() > MAX_WEEKLY_HOURS) {
            return "Las horas semanales deben estar entre " + MIN_WEEKLY_HOURS + " y " + MAX_WEEKLY_HOURS + ".";
        }
        return null;
    }
}
