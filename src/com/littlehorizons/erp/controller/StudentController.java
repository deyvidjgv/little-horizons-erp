package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.GuardianDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.StudentGuardianDAO;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.Validator;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Rules of the student records: valid data, unique document, at least one guardian, and students
 * with history (enrollments, grades or payments) are not deleted.
 */
public class StudentController {

    /** A student can have any document type; TI is the usual one. */
    public static final List<DocumentType> DOCUMENT_TYPES = List.of(DocumentType.values());

    public static final int MIN_AGE = 3;
    public static final int MAX_AGE = 25;

    private final StudentDAO studentDAO;
    private final GuardianDAO guardianDAO;
    private final StudentGuardianDAO studentGuardianDAO;

    public StudentController(StudentDAO studentDAO, GuardianDAO guardianDAO, StudentGuardianDAO studentGuardianDAO) {
        this.studentDAO = studentDAO;
        this.guardianDAO = guardianDAO;
        this.studentGuardianDAO = studentGuardianDAO;
    }

    public Optional<Student> findByDocument(String documentNumber) throws DataAccessException {
        return studentDAO.findByDocument(documentNumber.trim());
    }

    /** @param status null to list every student */
    public List<Student> list(StudentStatus status) throws DataAccessException {
        return studentDAO.findAll().stream()
                .filter(student -> status == null || student.getStatus() == status)
                .toList();
    }

    /** Looks for the text inside the full name, ignoring upper case and accents. */
    public List<Student> searchByName(String text) throws DataAccessException {
        String wanted = normalize(text);
        return studentDAO.findAll().stream()
                .filter(student -> normalize(student.getFullName()).contains(wanted))
                .toList();
    }

    public List<StudentGuardian> listGuardians(int studentId) throws DataAccessException {
        return studentGuardianDAO.findGuardiansByStudent(studentId);
    }

    /**
     * Checks the student before asking for the guardian, so the user does not type the guardian in vain.
     *
     * @return null if the student can be registered; otherwise the reason
     */
    public String validateNewStudent(Student student) throws DataAccessException {
        String rejection = validate(student);
        if (rejection != null) {
            return rejection;
        }
        int age = student.getAge();
        if (age < MIN_AGE || age > MAX_AGE) {
            return "La fecha de nacimiento no es válida: un estudiante nuevo debe tener entre "
                    + MIN_AGE + " y " + MAX_AGE + " años.";
        }
        if (studentDAO.findByDocument(student.getDocumentNumber()).isPresent()) {
            return "Ya existe un estudiante con el documento " + student.getDocumentNumber() + ".";
        }
        return null;
    }

    /**
     * Saves the student as Activo, already linked to its first guardian. The registration must include an
     * emergency contact, so the first guardian is always one.
     */
    public OperationResult register(Student student, int guardianId, boolean primary, boolean emergencyContact)
            throws DataAccessException {
        String rejection = validateNewStudent(student);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (!emergencyContact) {
            return OperationResult.error("El primer acudiente debe ser contacto de emergencia.");
        }
        if (guardianDAO.findById(guardianId).isEmpty()) {
            return OperationResult.error("El acudiente no existe. Todo estudiante debe tener al menos un acudiente.");
        }
        student.setStatus(StudentStatus.ACTIVE);
        student.setId(studentDAO.create(student, guardianId, primary, emergencyContact));
        return OperationResult.ok("Estudiante registrado con id " + student.getId() + ": "
                + student.getFullName() + ".");
    }

    public OperationResult update(Student student) throws DataAccessException {
        String rejection = validate(student);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Student> sameDocument = studentDAO.findByDocument(student.getDocumentNumber());
        if (sameDocument.isPresent() && sameDocument.get().getId() != student.getId()) {
            return OperationResult.error("Ya existe otro estudiante con el documento "
                    + student.getDocumentNumber() + ".");
        }
        return studentDAO.update(student)
                ? OperationResult.ok("Datos de " + student.getFullName() + " actualizados.")
                : OperationResult.error("No existe un estudiante con id " + student.getId() + ".");
    }

    /** Only a student without history can be deleted (for example, one registered by mistake). */
    public OperationResult delete(int studentId) throws DataAccessException {
        Optional<Student> student = studentDAO.findById(studentId);
        if (student.isEmpty()) {
            return OperationResult.error("No existe un estudiante con id " + studentId + ".");
        }
        String name = student.get().getFullName();
        if (studentDAO.hasHistory(studentId)) {
            return OperationResult.error("No se puede eliminar a " + name + ": tiene matrículas, notas, pagos o "
                    + "préstamos registrados. Si dejó el colegio, use la opción Retirar estudiante en Matrículas.");
        }
        return studentDAO.delete(studentId)
                ? OperationResult.ok("Estudiante eliminado: " + name + ".")
                : OperationResult.error("No existe un estudiante con id " + studentId + ".");
    }

    private String validate(Student student) {
        String rejection = PersonRules.validateIdentity(student, DOCUMENT_TYPES);
        if (rejection != null) {
            return rejection;
        }
        LocalDate today = LocalDate.now();
        if (student.getBirthDate() == null || student.getBirthDate().isAfter(today)) {
            return "La fecha de nacimiento es obligatoria y no puede ser futura.";
        }
        if (student.getGender() == null) {
            return "El género es obligatorio.";
        }
        rejection = PersonRules.validateContact(student.getAddress(), student.getPhone(), student.getEmail());
        if (rejection != null) {
            return rejection;
        }
        if (student.getBloodType() != null && !Validator.isBloodType(student.getBloodType())) {
            return Validator.BLOOD_TYPE_MESSAGE;
        }
        if (isTooLong(student.getAllergies()) || isTooLong(student.getMedicalConditions())) {
            return "Alergias y condiciones médicas admiten máximo " + Student.HEALTH_TEXT_MAX_LENGTH + " caracteres.";
        }
        LocalDate entryDate = student.getEntryDate();
        if (entryDate == null || entryDate.isAfter(today) || entryDate.isBefore(student.getBirthDate())) {
            return "La fecha de ingreso es obligatoria, no puede ser futura ni anterior a la fecha de nacimiento.";
        }
        return null;
    }

    private static boolean isTooLong(String text) {
        return text != null && text.length() > Student.HEALTH_TEXT_MAX_LENGTH;
    }

    private static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim();
    }
}
