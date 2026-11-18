package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Row of prestamos_estudiante or prestamos_profesor: who has a resource and until when. */
public class Loan {

    /** observaciones is a TEXT column; the application keeps it short to fit on screen. */
    public static final int NOTES_MAX_LENGTH = 500;

    private int id;
    private BorrowerType borrowerType;
    /** A Student or a Teacher, with the id, the document and the names. */
    private Person borrower;
    /** Only the id, code and name of the resource are loaded. */
    private Resource resource;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private String notes;

    /** A loan is overdue when it was not returned and its due date already passed. */
    public boolean isOverdue(LocalDate today) {
        return returnDate == null && dueDate.isBefore(today);
    }

    public long getDaysOverdue(LocalDate today) {
        return isOverdue(today) ? ChronoUnit.DAYS.between(dueDate, today) : 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public BorrowerType getBorrowerType() {
        return borrowerType;
    }

    public void setBorrowerType(BorrowerType borrowerType) {
        this.borrowerType = borrowerType;
    }

    public Person getBorrower() {
        return borrower;
    }

    public void setBorrower(Person borrower) {
        this.borrower = borrower;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
