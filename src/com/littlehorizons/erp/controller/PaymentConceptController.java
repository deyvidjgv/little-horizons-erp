package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.PaymentConceptDAO;
import com.littlehorizons.erp.dao.StudentConceptValueDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentConceptValue;
import com.littlehorizons.erp.util.Formats;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Catalog of payment concepts with their base amount, and the special values of a concept for
 * specific students (discount for siblings, partial scholarships).
 */
public class PaymentConceptController {

    public static final String CODE_RULE = "El código lleva letras mayúsculas, números, guiones o guion bajo "
            + "(ejemplo: PENSION).";

    private static final Pattern CODE = Pattern.compile("[A-Z0-9_-]{2,20}");

    private final PaymentConceptDAO conceptDAO;
    private final StudentConceptValueDAO valueDAO;
    private final StudentDAO studentDAO;

    public PaymentConceptController(PaymentConceptDAO conceptDAO, StudentConceptValueDAO valueDAO,
                                    StudentDAO studentDAO) {
        this.conceptDAO = conceptDAO;
        this.valueDAO = valueDAO;
        this.studentDAO = studentDAO;
    }

    public List<PaymentConcept> list() throws DataAccessException {
        return conceptDAO.findAll();
    }

    public List<PaymentConcept> listActive() throws DataAccessException {
        return conceptDAO.findAll().stream()
                .filter(PaymentConcept::isActive)
                .toList();
    }

    public List<StudentConceptValue> listStudentValues(int studentId) throws DataAccessException {
        return valueDAO.findByStudent(studentId);
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValidCode(String code) {
        return code != null && CODE.matcher(code).matches();
    }

    /** Amounts are whole pesos between 0 and the largest value of the column. */
    public static boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.signum() >= 0 && amount.stripTrailingZeros().scale() <= 0
                && amount.compareTo(Payment.MAX_AMOUNT) <= 0;
    }

    public OperationResult register(PaymentConcept concept) throws DataAccessException {
        concept.setCode(normalizeCode(concept.getCode()));
        concept.setActive(true);
        String rejection = validate(concept);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (conceptDAO.findByCode(concept.getCode()).isPresent()) {
            return OperationResult.error("Ya existe un concepto con el código " + concept.getCode() + ".");
        }
        concept.setId(conceptDAO.create(concept));
        return OperationResult.ok("Concepto registrado: " + concept.getName() + " por "
                + Formats.money(concept.getBaseAmount()) + ".");
    }

    public OperationResult update(PaymentConcept concept) throws DataAccessException {
        concept.setCode(normalizeCode(concept.getCode()));
        String rejection = validate(concept);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<PaymentConcept> sameCode = conceptDAO.findByCode(concept.getCode());
        if (sameCode.isPresent() && sameCode.get().getId() != concept.getId()) {
            return OperationResult.error("Ya existe otro concepto con el código " + concept.getCode() + ".");
        }
        return conceptDAO.update(concept)
                ? OperationResult.ok("Concepto actualizado: " + concept.getName() + " por "
                + Formats.money(concept.getBaseAmount()) + ".")
                : OperationResult.error("No existe el concepto.");
    }

    /** A concept with payments or special values is not deleted: it is deactivated. */
    public OperationResult delete(int conceptId) throws DataAccessException {
        Optional<PaymentConcept> concept = conceptDAO.findById(conceptId);
        if (concept.isEmpty()) {
            return OperationResult.error("No existe el concepto.");
        }
        if (conceptDAO.hasHistory(conceptId)) {
            return OperationResult.error("No se puede eliminar " + concept.get().getName() + ": tiene pagos o valores "
                    + "especiales registrados. Desactívelo con la opción Actualizar.");
        }
        return conceptDAO.delete(conceptId)
                ? OperationResult.ok("Concepto eliminado: " + concept.get().getName() + ".")
                : OperationResult.error("No existe el concepto.");
    }

    /** Sets what the student pays for the concept instead of its base amount; saving it again changes it. */
    public OperationResult setStudentValue(int studentId, int conceptId, BigDecimal amount, String reason)
            throws DataAccessException {
        Optional<Student> student = studentDAO.findById(studentId);
        Optional<PaymentConcept> concept = conceptDAO.findById(conceptId);
        if (student.isEmpty() || concept.isEmpty()) {
            return OperationResult.error("El estudiante o el concepto no existe.");
        }
        if (!concept.get().isActive()) {
            return OperationResult.error("El concepto " + concept.get().getName() + " no está activo.");
        }
        if (!isValidAmount(amount)) {
            return OperationResult.error("El valor debe ser un número entero de pesos, sin centavos.");
        }
        if (reason == null || reason.isBlank() || reason.length() > StudentConceptValue.REASON_MAX_LENGTH) {
            return OperationResult.error("El motivo es obligatorio (máximo " + StudentConceptValue.REASON_MAX_LENGTH
                    + " caracteres).");
        }
        StudentConceptValue value = new StudentConceptValue();
        value.setStudent(student.get());
        value.setConcept(concept.get());
        value.setAmount(amount);
        value.setReason(reason);
        valueDAO.save(value);
        return OperationResult.ok(student.get().getFullName() + " paga " + Formats.money(amount) + " por "
                + concept.get().getName() + " (valor base " + Formats.money(concept.get().getBaseAmount()) + ").");
    }

    public OperationResult removeStudentValue(int valueId) throws DataAccessException {
        return valueDAO.delete(valueId)
                ? OperationResult.ok("Valor especial quitado: el estudiante vuelve a pagar el valor base.")
                : OperationResult.error("No existe el valor especial.");
    }

    private static String validate(PaymentConcept concept) {
        if (!isValidCode(concept.getCode())) {
            return CODE_RULE;
        }
        String name = concept.getName();
        if (name == null || name.isBlank() || name.length() > PaymentConcept.NAME_MAX_LENGTH) {
            return "El nombre es obligatorio (máximo " + PaymentConcept.NAME_MAX_LENGTH + " caracteres).";
        }
        String description = concept.getDescription();
        if (description != null && description.length() > PaymentConcept.DESCRIPTION_MAX_LENGTH) {
            return "La descripción admite máximo " + PaymentConcept.DESCRIPTION_MAX_LENGTH + " caracteres.";
        }
        if (!isValidAmount(concept.getBaseAmount()) || concept.getBaseAmount().signum() == 0) {
            return "El valor base debe ser un número entero de pesos mayor que 0.";
        }
        return null;
    }
}
