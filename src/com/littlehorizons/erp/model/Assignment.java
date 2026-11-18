package com.littlehorizons.erp.model;

import java.util.Locale;
import java.util.regex.Pattern;

/** Row of the asignaciones_profesor table: a teacher gives a subject to one group in one year. */
public class Assignment {

    public static final int SECTION_MAX_LENGTH = 10;
    public static final String SECTION_RULE = "La sección debe tener de 1 a 10 letras o números (ejemplo: A).";

    private static final Pattern SECTION = Pattern.compile("[A-Z0-9]{1,10}");

    private int id;
    private Teacher teacher;
    private Subject subject;
    private int academicYear;
    private String section;

    /** @return the section in capital letters, or null if it is empty */
    public static String normalizeSection(String section) {
        return section == null || section.isBlank() ? null : section.trim().toUpperCase(Locale.ROOT);
    }

    /** The section (group) is also used by the inscriptions in subjects. */
    public static boolean isValidSection(String section) {
        return section != null && SECTION.matcher(section).matches();
    }

    /** @return the subject and the group, for example "MAT-5 Matemáticas - Quinto A" */
    public String getDisplayName() {
        return subject.getCode() + " " + subject.getName() + " - " + subject.getGrade().getName() + " " + section;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
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

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}
