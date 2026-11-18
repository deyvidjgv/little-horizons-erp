package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Row of the inscripciones_materia table: a student takes a subject in one period. */
public class SubjectEnrollment {

    private int id;
    private Student student;
    private Subject subject;
    private int academicYear;
    private AcademicPeriod period;
    private String section;
    private LocalDate enrollmentDate;
    /** The column has the same values as the enrollment status: Activa, Retirada, Completada. */
    private EnrollmentStatus status;

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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public LocalDate getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDate enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }
}
