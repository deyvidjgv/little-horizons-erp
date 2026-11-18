package com.littlehorizons.erp.model;

import java.util.Arrays;
import java.util.Optional;

/** Values of the asistencias.estado_asistencia column, each with the letter typed when taking attendance. */
public enum AttendanceStatus implements DatabaseValue {
    PRESENT("Presente", "P"),
    ABSENT("Ausente", "A"),
    LATE("Tardanza", "T"),
    MEDICAL_EXCUSE("Excusa Médica", "E");

    private final String databaseValue;
    private final String shortcut;

    AttendanceStatus(String databaseValue, String shortcut) {
        this.databaseValue = databaseValue;
        this.shortcut = shortcut;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getShortcut() {
        return shortcut;
    }

    public static Optional<AttendanceStatus> fromShortcut(String text) {
        return Arrays.stream(values())
                .filter(status -> status.shortcut.equalsIgnoreCase(text))
                .findFirst();
    }
}
