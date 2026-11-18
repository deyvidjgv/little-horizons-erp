package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.GuardianDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.StudentGuardianDAO;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentGuardian;
import com.littlehorizons.erp.util.Validator;

import java.util.List;
import java.util.Optional;

/**
 * Rules of the guardians: valid data, unique document, one guardian can have several students
 * (siblings) and every student keeps at least one guardian.
 */
public class GuardianController {

    /** Guardians are adults: no tarjeta de identidad (same types as the acudientes.tipo_documento column). */
    public static final List<DocumentType> DOCUMENT_TYPES = List.of(DocumentType.CC, DocumentType.CE,
            DocumentType.PASSPORT);

    private final GuardianDAO guardianDAO;
    private final StudentDAO studentDAO;
    private final StudentGuardianDAO studentGuardianDAO;

    public GuardianController(GuardianDAO guardianDAO, StudentDAO studentDAO, StudentGuardianDAO studentGuardianDAO) {
        this.guardianDAO = guardianDAO;
        this.studentDAO = studentDAO;
        this.studentGuardianDAO = studentGuardianDAO;
    }

    public Optional<Guardian> findByDocument(String documentNumber) throws DataAccessException {
        return guardianDAO.findByDocument(documentNumber.trim());
    }

    public List<Guardian> list() throws DataAccessException {
        return guardianDAO.findAll();
    }

    public List<Student> listStudents(int guardianId) throws DataAccessException {
        return studentGuardianDAO.findStudentsByGuardian(guardianId);
    }

    public OperationResult register(Guardian guardian) throws DataAccessException {
        String rejection = validate(guardian);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (guardianDAO.findByDocument(guardian.getDocumentNumber()).isPresent()) {
            return OperationResult.error("Ya existe un acudiente con el documento " + guardian.getDocumentNumber() + ".");
        }
        guardian.setId(guardianDAO.create(guardian));
        return OperationResult.ok("Acudiente registrado con id " + guardian.getId() + ": "
                + guardian.getFullName() + ".");
    }

    public OperationResult update(Guardian guardian) throws DataAccessException {
        String rejection = validate(guardian);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Guardian> sameDocument = guardianDAO.findByDocument(guardian.getDocumentNumber());
        if (sameDocument.isPresent() && sameDocument.get().getId() != guardian.getId()) {
            return OperationResult.error("Ya existe otro acudiente con el documento "
                    + guardian.getDocumentNumber() + ".");
        }
        return guardianDAO.update(guardian)
                ? OperationResult.ok("Datos de " + guardian.getFullName() + " actualizados.")
                : OperationResult.error("No existe un acudiente con id " + guardian.getId() + ".");
    }

    public OperationResult delete(int guardianId) throws DataAccessException {
        Optional<Guardian> guardian = guardianDAO.findById(guardianId);
        if (guardian.isEmpty()) {
            return OperationResult.error("No existe un acudiente con id " + guardianId + ".");
        }
        String name = guardian.get().getFullName();
        if (!studentGuardianDAO.findStudentsByGuardian(guardianId).isEmpty()) {
            return OperationResult.error("No se puede eliminar a " + name + ": tiene estudiantes a cargo. "
                    + "Primero desvincúlelo de cada estudiante.");
        }
        if (guardianDAO.hasPayments(guardianId)) {
            return OperationResult.error("No se puede eliminar a " + name + ": tiene pagos registrados.");
        }
        return guardianDAO.delete(guardianId)
                ? OperationResult.ok("Acudiente eliminado: " + name + ".")
                : OperationResult.error("No existe un acudiente con id " + guardianId + ".");
    }

    /** Links the guardian to the student; if they were already linked, only the flags change. */
    public OperationResult link(int studentId, int guardianId, boolean primary, boolean emergencyContact)
            throws DataAccessException {
        Optional<Student> student = studentDAO.findById(studentId);
        Optional<Guardian> guardian = guardianDAO.findById(guardianId);
        if (student.isEmpty() || guardian.isEmpty()) {
            return OperationResult.error("El estudiante o el acudiente no existe.");
        }
        List<StudentGuardian> links = studentGuardianDAO.findGuardiansByStudent(studentId);
        boolean alreadyLinked = links.stream().anyMatch(link -> link.getGuardian().getId() == guardianId);
        if (!emergencyContact && !hasOtherEmergencyContact(links, guardianId)) {
            return OperationResult.error("El estudiante necesita al menos un contacto de emergencia: "
                    + "marque a este acudiente como contacto de emergencia o vincule primero a otro.");
        }
        studentGuardianDAO.link(studentId, guardianId, primary, emergencyContact);
        String names = guardian.get().getFullName() + " y " + student.get().getFullName();
        return OperationResult.ok(alreadyLinked
                ? "Se actualizó el vínculo entre " + names + "."
                : "Quedaron vinculados " + names + ".");
    }

    public OperationResult unlink(int studentId, int guardianId) throws DataAccessException {
        List<StudentGuardian> links = studentGuardianDAO.findGuardiansByStudent(studentId);
        boolean linked = links.stream().anyMatch(link -> link.getGuardian().getId() == guardianId);
        if (!linked) {
            return OperationResult.error("Ese acudiente no está vinculado al estudiante.");
        }
        if (links.size() == 1) {
            return OperationResult.error("No se puede desvincular: cada estudiante debe tener al menos un acudiente. "
                    + "Vincule primero a otro acudiente.");
        }
        if (!hasOtherEmergencyContact(links, guardianId)) {
            return OperationResult.error("No se puede desvincular: es el único contacto de emergencia del estudiante. "
                    + "Marque primero a otro acudiente como contacto de emergencia.");
        }
        return studentGuardianDAO.unlink(studentId, guardianId)
                ? OperationResult.ok("Acudiente desvinculado del estudiante.")
                : OperationResult.error("Ese acudiente no está vinculado al estudiante.");
    }

    /** @return true if a guardian other than guardianId is an emergency contact of the student */
    private static boolean hasOtherEmergencyContact(List<StudentGuardian> links, int guardianId) {
        return links.stream()
                .anyMatch(link -> link.getGuardian().getId() != guardianId && link.isEmergencyContact());
    }

    private String validate(Guardian guardian) {
        String rejection = PersonRules.validateIdentity(guardian, DOCUMENT_TYPES);
        if (rejection != null) {
            return rejection;
        }
        String relationship = guardian.getRelationship();
        if (relationship == null || relationship.length() > Guardian.RELATIONSHIP_MAX_LENGTH
                || !Validator.isName(relationship)) {
            return "El parentesco es obligatorio (máximo " + Guardian.RELATIONSHIP_MAX_LENGTH + " letras).";
        }
        if (!Validator.isPhone(guardian.getMainPhone())) {
            return "El teléfono principal es obligatorio. " + Validator.PHONE_MESSAGE;
        }
        rejection = PersonRules.validateContact(guardian.getAddress(), guardian.getSecondaryPhone(),
                guardian.getEmail());
        if (rejection != null) {
            return rejection;
        }
        String occupation = guardian.getOccupation();
        if (occupation != null && occupation.length() > Guardian.OCCUPATION_MAX_LENGTH) {
            return "La ocupación admite máximo " + Guardian.OCCUPATION_MAX_LENGTH + " caracteres.";
        }
        return null;
    }
}
