package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.LoanDAO;
import com.littlehorizons.erp.dao.ResourceDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.TeacherDAO;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.Formats;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Loans of resources to students and teachers: only an available resource is lent, the return puts it
 * back as Disponible, and the report shows the loans not returned on their due date.
 */
public class LoanController {

    private final LoanDAO loanDAO;
    private final ResourceDAO resourceDAO;
    private final StudentDAO studentDAO;
    private final TeacherDAO teacherDAO;

    public LoanController(LoanDAO loanDAO, ResourceDAO resourceDAO, StudentDAO studentDAO, TeacherDAO teacherDAO) {
        this.loanDAO = loanDAO;
        this.resourceDAO = resourceDAO;
        this.studentDAO = studentDAO;
        this.teacherDAO = teacherDAO;
    }

    /** @return the loan of the resource that has not been returned yet, if there is one */
    public Optional<Loan> findOpenLoan(int resourceId) throws DataAccessException {
        return loanDAO.findOpenByResource(resourceId);
    }

    public OperationResult lend(Loan loan) throws DataAccessException {
        Optional<Resource> resource = resourceDAO.findById(loan.getResource().getId());
        if (resource.isEmpty()) {
            return OperationResult.error("No existe el recurso.");
        }
        if (resource.get().getStatus() != ResourceStatus.AVAILABLE) {
            return OperationResult.error(resource.get().getDisplayName() + " no está disponible (estado: "
                    + resource.get().getStatus().getDatabaseValue() + ").");
        }
        String rejection = validateBorrower(loan.getBorrowerType(), loan.getBorrower().getId());
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        LocalDate today = LocalDate.now();
        if (loan.getLoanDate() == null || loan.getLoanDate().isAfter(today)) {
            return OperationResult.error("La fecha del préstamo es obligatoria y no puede ser futura.");
        }
        if (loan.getDueDate() == null || loan.getDueDate().isBefore(loan.getLoanDate())) {
            return OperationResult.error("La fecha de devolución programada no puede ser anterior al préstamo.");
        }
        if (loan.getNotes() != null && loan.getNotes().length() > Loan.NOTES_MAX_LENGTH) {
            return OperationResult.error("Las observaciones admiten máximo " + Loan.NOTES_MAX_LENGTH + " caracteres.");
        }
        // The availability is checked again inside the transaction, with the resource row locked.
        if (!loanDAO.lend(loan)) {
            return OperationResult.error(resource.get().getDisplayName() + " ya no está disponible.");
        }
        return OperationResult.ok(resource.get().getDisplayName() + " prestado a " + loan.getBorrower().getFullName()
                + " hasta el " + Formats.date(loan.getDueDate()) + ".");
    }

    public OperationResult registerReturn(int resourceId, LocalDate returnDate) throws DataAccessException {
        Optional<Loan> loan = loanDAO.findOpenByResource(resourceId);
        if (loan.isEmpty()) {
            return OperationResult.error("El recurso no tiene un préstamo pendiente de devolución.");
        }
        LocalDate loanDate = loan.get().getLoanDate();
        if (returnDate == null || returnDate.isAfter(LocalDate.now()) || returnDate.isBefore(loanDate)) {
            return OperationResult.error("La fecha de devolución no puede ser futura ni anterior al préstamo ("
                    + Formats.date(loanDate) + ").");
        }
        loanDAO.registerReturn(loan.get(), returnDate);
        String late = returnDate.isAfter(loan.get().getDueDate()) ? " Se devolvió después de la fecha programada." : "";
        return OperationResult.ok(loan.get().getResource().getDisplayName() + " devuelto por "
                + loan.get().getBorrower().getFullName() + "; ahora está Disponible." + late);
    }

    /** Updates the status of the loans that are already overdue and returns them, the oldest due date first. */
    public List<Loan> overdueLoans() throws DataAccessException {
        LocalDate today = LocalDate.now();
        loanDAO.markOverdue(today);
        return loanDAO.findOverdue(today).stream()
                .sorted(Comparator.comparing(Loan::getDueDate))
                .toList();
    }

    private String validateBorrower(BorrowerType type, int borrowerId) throws DataAccessException {
        if (type == BorrowerType.STUDENT) {
            Optional<Student> student = studentDAO.findById(borrowerId);
            return student.isEmpty() ? "El estudiante no existe."
                    : student.get().getStatus() == StudentStatus.ACTIVE ? null
                    : student.get().getFullName() + " no está Activo; no se le pueden prestar recursos.";
        }
        Optional<Teacher> teacher = teacherDAO.findById(borrowerId);
        return teacher.isEmpty() ? "El profesor no existe."
                : teacher.get().getStatus() == TeacherStatus.ACTIVE ? null
                : teacher.get().getFullName() + " no está Activo; no se le pueden prestar recursos.";
    }
}
