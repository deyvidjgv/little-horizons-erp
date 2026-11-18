package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Academic years accepted by the application: from 2000 to next year (to enroll or assign in advance). */
public final class AcademicYear {

    public static final int FIRST = 2000;

    private AcademicYear() {
    }

    public static int current() {
        return LocalDate.now().getYear();
    }

    public static int last() {
        return current() + 1;
    }

    public static boolean isValid(int year) {
        return year >= FIRST && year <= last();
    }
}
