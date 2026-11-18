package com.littlehorizons.erp.model;

/** Values of the participantes_evento.rol_participacion column. */
public enum ParticipationRole implements DatabaseValue {
    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String databaseValue;

    ParticipationRole(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }
}
