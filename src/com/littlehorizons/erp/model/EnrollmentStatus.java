package com.littlehorizons.erp.model;

/** Values of the matriculas.estado_matricula column. */
public enum EnrollmentStatus implements DatabaseValue {
    ACTIVE("Activa"),
    COMPLETED("Completada"),
    WITHDRAWN("Retirada");

    private final String databaseValue;

    EnrollmentStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
