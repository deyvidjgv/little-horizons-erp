package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Row of the asistencias table: the attendance of a student to one class of a subject. */
public class Attendance {

    private int id;
    private Student student;
    private Subject subject;
    private LocalDate date;
    private AttendanceStatus status;
    private String notes;
    /** Teacher who took the attendance (registrado_por). */
    private int recordedById;

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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getRecordedById() {
        return recordedById;
    }

    public void setRecordedById(int recordedById) {
        this.recordedById = recordedById;
    }
}
