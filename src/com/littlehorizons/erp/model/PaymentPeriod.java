package com.littlehorizons.erp.model;

/** Values of the pagos.periodo_academico column: the period the payment is for, or the whole year. */
public enum PaymentPeriod implements DatabaseValue {
    FIRST("I"),
    SECOND("II"),
    THIRD("III"),
    FOURTH("IV"),
    ANNUAL("Anual");

    private final String databaseValue;

    PaymentPeriod(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
