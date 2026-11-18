package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/**
 * Row of the valores_concepto_estudiante table: what a specific student pays for a concept instead
 * of its base amount (discount for siblings, partial scholarship...).
 */
public class StudentConceptValue {

    public static final int REASON_MAX_LENGTH = 150;

    private int id;
    private Student student;
    private PaymentConcept concept;
    private BigDecimal amount;
    private String reason;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public PaymentConcept getConcept() {
        return concept;
    }

    public void setConcept(PaymentConcept concept) {
        this.concept = concept;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
