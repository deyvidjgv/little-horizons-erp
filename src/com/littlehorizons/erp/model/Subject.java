package com.littlehorizons.erp.model;

/** Row of the materias table: a subject of one grade (for example MAT-5, Matemáticas de Quinto). */
public class Subject {

    public static final int CODE_MAX_LENGTH = 20;
    public static final int NAME_MAX_LENGTH = 100;
    /** descripcion is a TEXT column; the application keeps it short to fit on screen. */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private int id;
    private String code;
    private String name;
    private String description;
    private Grade grade;
    private int weeklyHours;
    private boolean active;

    /** @return for example "MAT-5 Matemáticas (Quinto)" */
    public String getDisplayName() {
        return code + " " + name + " (" + grade.getName() + ")";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Grade getGrade() {
        return grade;
    }

    public void setGrade(Grade grade) {
        this.grade = grade;
    }

    public int getWeeklyHours() {
        return weeklyHours;
    }

    public void setWeeklyHours(int weeklyHours) {
        this.weeklyHours = weeklyHours;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
