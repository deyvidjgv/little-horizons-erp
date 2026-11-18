package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.TeacherDAO;
import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Rules of the teacher directory: valid data, unique document and e-mail, and teachers with history
 * are not deleted (they are marked Inactivo).
 */
public class TeacherController {

    /** Same types as the profesores.tipo_documento column. */
    public static final List<DocumentType> DOCUMENT_TYPES = List.of(DocumentType.CC, DocumentType.CE,
            DocumentType.PASSPORT);

    private final TeacherDAO teacherDAO;

    public TeacherController(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
    }

    public Optional<Teacher> findByDocument(String documentNumber) throws DataAccessException {
        return teacherDAO.findByDocument(documentNumber.trim());
    }

    /** @param status null to list every teacher */
    public List<Teacher> list(TeacherStatus status) throws DataAccessException {
        return teacherDAO.findAll().stream()
                .filter(teacher -> status == null || teacher.getStatus() == status)
                .toList();
    }

    public OperationResult register(Teacher teacher) throws DataAccessException {
        teacher.setStatus(TeacherStatus.ACTIVE);
        String rejection = validate(teacher);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (teacherDAO.findByDocument(teacher.getDocumentNumber()).isPresent()) {
            return OperationResult.error("Ya existe un profesor con el documento " + teacher.getDocumentNumber() + ".");
        }
        if (teacherDAO.findByEmail(teacher.getEmail()).isPresent()) {
            return OperationResult.error("Ya existe un profesor con el correo " + teacher.getEmail() + ".");
        }
        teacher.setId(teacherDAO.create(teacher));
        return OperationResult.ok("Profesor registrado con id " + teacher.getId() + ": " + teacher.getFullName() + ".");
    }

    public OperationResult update(Teacher teacher) throws DataAccessException {
        String rejection = validate(teacher);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Teacher> sameDocument = teacherDAO.findByDocument(teacher.getDocumentNumber());
        if (sameDocument.isPresent() && sameDocument.get().getId() != teacher.getId()) {
            return OperationResult.error("Ya existe otro profesor con el documento " + teacher.getDocumentNumber() + ".");
        }
        Optional<Teacher> sameEmail = teacherDAO.findByEmail(teacher.getEmail());
        if (sameEmail.isPresent() && sameEmail.get().getId() != teacher.getId()) {
            return OperationResult.error("Ya existe otro profesor con el correo " + teacher.getEmail() + ".");
        }
        return teacherDAO.update(teacher)
                ? OperationResult.ok("Datos de " + teacher.getFullName() + " actualizados.")
                : OperationResult.error("No existe un profesor con id " + teacher.getId() + ".");
    }

    /** Only a teacher without history can be deleted (for example, one registered by mistake). */
    public OperationResult delete(int teacherId) throws DataAccessException {
        Optional<Teacher> teacher = teacherDAO.findById(teacherId);
        if (teacher.isEmpty()) {
            return OperationResult.error("No existe un profesor con id " + teacherId + ".");
        }
        String name = teacher.get().getFullName();
        if (teacherDAO.hasHistory(teacherId)) {
            return OperationResult.error("No se puede eliminar a " + name + ": tiene materias asignadas, notas, "
                    + "asistencias, clubes, eventos o préstamos registrados. Cambie su estado a Inactivo.");
        }
        return teacherDAO.delete(teacherId)
                ? OperationResult.ok("Profesor eliminado: " + name + ".")
                : OperationResult.error("No existe un profesor con id " + teacherId + ".");
    }

    private String validate(Teacher teacher) {
        String rejection = PersonRules.validateIdentity(teacher, DOCUMENT_TYPES);
        if (rejection != null) {
            return rejection;
        }
        String email = teacher.getEmail();
        if (email == null || email.length() > Person.EMAIL_MAX_LENGTH || !Validator.isEmail(email)) {
            return "El correo es obligatorio. " + Validator.EMAIL_MESSAGE;
        }
        // The profesores table has no address; only the optional phone is checked.
        if (teacher.getPhone() != null && !Validator.isPhone(teacher.getPhone())) {
            return Validator.PHONE_MESSAGE;
        }
        String specialization = teacher.getSpecialization();
        if (specialization == null || specialization.isBlank()
                || specialization.length() > Teacher.SPECIALIZATION_MAX_LENGTH) {
            return "La especialización es obligatoria (máximo " + Teacher.SPECIALIZATION_MAX_LENGTH + " caracteres).";
        }
        if (teacher.getHireDate() == null || teacher.getHireDate().isAfter(LocalDate.now())) {
            return "La fecha de contratación es obligatoria y no puede ser futura.";
        }
        if (teacher.getStatus() == null) {
            return "El estado es obligatorio.";
        }
        return null;
    }
}
