package com.littlehorizons.erp.model;

/** Values of the estudiantes.estado column. */
public enum StudentStatus implements DatabaseValue {
    ACTIVE("Activo"),
    GRADUATED("Graduado"),
    RETIRED("Retirado"),
    SUSPENDED("Suspendido");

    private final String databaseValue;

    StudentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
