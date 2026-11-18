package com.littlehorizons.erp.model;

/** Values of the membresias_club.estado_membresia column. */
public enum MembershipStatus implements DatabaseValue {
    ACTIVE("Activa"),
    WITHDRAWN("Retirada");

    private final String databaseValue;

    MembershipStatus(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
