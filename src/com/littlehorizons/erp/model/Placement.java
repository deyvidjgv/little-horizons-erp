package com.littlehorizons.erp.model;

/**
 * Position obtained in an event with competition. The column posicion_obtenida is VARCHAR(20); these
 * are the values the statement asks for.
 */
public enum Placement implements DatabaseValue {
    FIRST("1er Lugar"),
    SECOND("2do Lugar"),
    THIRD("3er Lugar"),
    PARTICIPANT("Participante");

    private final String databaseValue;

    Placement(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
