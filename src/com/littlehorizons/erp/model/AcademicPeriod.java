package com.littlehorizons.erp.model;

import java.util.List;

/** Values of the periodo_academico columns of inscriptions and grades. */
public enum AcademicPeriod implements DatabaseValue {
    FIRST("I"),
    SECOND("II"),
    THIRD("III"),
    FOURTH("IV"),
    FINAL("Final");

    private final String databaseValue;

    AcademicPeriod(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }

    /** @return the four periods of the year; Final is only used for grades, not for inscriptions */
    public static List<AcademicPeriod> terms() {
        return List.of(FIRST, SECOND, THIRD, FOURTH);
    }

    public boolean isTerm() {
        return this != FINAL;
    }
}
