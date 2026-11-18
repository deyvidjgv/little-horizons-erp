package com.littlehorizons.erp.model;

/** Values of the estudiantes.genero column. */
public enum Gender implements DatabaseValue {
    MALE("M", "Masculino"),
    FEMALE("F", "Femenino"),
    OTHER("Otro", "Otro");

    private final String databaseValue;
    private final String label;

    Gender(String databaseValue, String label) {
        this.databaseValue = databaseValue;
        this.label = label;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getLabel() {
        return label;
    }
}
