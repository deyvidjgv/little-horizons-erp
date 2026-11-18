package com.littlehorizons.erp.model;

/** Values of the horarios_asignacion.dia_semana column (school days). */
public enum Weekday implements DatabaseValue {
    MONDAY("Lunes"),
    TUESDAY("Martes"),
    WEDNESDAY("Miércoles"),
    THURSDAY("Jueves"),
    FRIDAY("Viernes");

    private final String databaseValue;

    Weekday(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
