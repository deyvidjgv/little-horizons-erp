package com.littlehorizons.erp.model;

/** Values of the estado_prestamo column of both loan tables. */
public enum LoanStatus implements DatabaseValue {
    ACTIVE("Activo"),
    RETURNED("Devuelto"),
    OVERDUE("Vencido");

    private final String databaseValue;

    LoanStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
