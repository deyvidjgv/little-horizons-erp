package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Row of the registros_academicos table: the grade of a student in a subject and period. */
public class AcademicRecord {

    public static final BigDecimal MIN_VALUE = new BigDecimal("0.0");
    public static final BigDecimal MAX_VALUE = new BigDecimal("5.0");
    /** In the Colombian scale a grade under 3.0 does not pass. */
    public static final BigDecimal PASSING_VALUE = new BigDecimal("3.0");

    private int id;
    private Student student;
    private Subject subject;
    private Teacher teacher;
    private int academicYear;
    private AcademicPeriod period;
    private BigDecimal value;
    private String notes;
    private LocalDate recordDate;

    /** Same rule as the column: DECIMAL(2,1) between 0.0 and 5.0. */
    public static boolean isValidValue(BigDecimal value) {
        return value != null
                && value.stripTrailingZeros().scale() <= 1
                && value.compareTo(MIN_VALUE) >= 0
                && value.compareTo(MAX_VALUE) <= 0;
    }

    public boolean isPassing() {
        return value.compareTo(PASSING_VALUE) >= 0;
    }

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

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }

    public int getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(int academicYear) {
        this.academicYear = academicYear;
    }

    public AcademicPeriod getPeriod() {
        return period;
    }

    public void setPeriod(AcademicPeriod period) {
        this.period = period;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }
}
