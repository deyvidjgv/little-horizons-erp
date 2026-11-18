package com.littlehorizons.erp.model;

/** Values of the recursos.estado_recurso column. */
public enum ResourceStatus implements DatabaseValue {
    AVAILABLE("Disponible"),
    LOANED("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    RETIRED("Dado de Baja");

    private final String databaseValue;

    ResourceStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
