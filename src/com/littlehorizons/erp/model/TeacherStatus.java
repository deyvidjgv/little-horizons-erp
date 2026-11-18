package com.littlehorizons.erp.model;

/** Values of the profesores.estado column. */
public enum TeacherStatus implements DatabaseValue {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    ON_LEAVE("Licencia");

    private final String databaseValue;

    TeacherStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
