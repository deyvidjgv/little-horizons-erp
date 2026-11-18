package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row of the pagos table. */
public class Payment {

    /** Largest value of a DECIMAL(10,2) column in whole pesos. */
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999");
    /** notas is a TEXT column; the application keeps it short to fit on screen. */
    public static final int NOTES_MAX_LENGTH = 500;

    private int id;
    private Student student;
    /** Guardian who paid; for the invoice it is the customer. */
    private Guardian payer;
    private PaymentConcept concept;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private PaymentMethod method;
    private String receiptNumber;
    private int academicYear;
    private PaymentPeriod period;
    private String notes;
    /** Number of the invoice of this payment, or null if it has none (the test data have no invoices). */
    private String invoiceNumber;

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

    public Guardian getPayer() {
        return payer;
    }

    public void setPayer(Guardian payer) {
        this.payer = payer;
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

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public PaymentPeriod getPeriod() {
        return period;
    }

    public void setPeriod(PaymentPeriod period) {
        this.period = period;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
}
