package com.littlehorizons.erp.model;

/** Values of the eventos.tipo_evento column. */
public enum EventType implements DatabaseValue {
    ACADEMIC("Académico"),
    CULTURAL("Cultural"),
    SPORTS("Deportivo"),
    SOCIAL("Social"),
    SCIENTIFIC("Científico");

    private final String databaseValue;

    EventType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
