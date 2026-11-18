package com.littlehorizons.erp.model;

import java.util.Arrays;

/**
 * Implemented by the enums that are stored in an ENUM column. The database keeps the Spanish
 * value (for example 'Activo') and the code uses the English constant (ACTIVE).
 */
public interface DatabaseValue {

    String getDatabaseValue();

    /** @throws IllegalArgumentException if the database returned a value the enum does not know */
    static <E extends Enum<E> & DatabaseValue> E fromDatabaseValue(Class<E> type, String value) {
        return Arrays.stream(type.getEnumConstants())
                .filter(constant -> constant.getDatabaseValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown " + type.getSimpleName() + " value: " + value));
    }
}
