package com.littlehorizons.erp.model;

import java.util.regex.Pattern;

/** Identity document of the tipo_documento columns. */
public enum DocumentType implements DatabaseValue {
    TI("TI", "Tarjeta de identidad", true),
    CC("CC", "Cédula de ciudadanía", true),
    CE("CE", "Cédula de extranjería", false),
    PASSPORT("Pasaporte", "Pasaporte", false);

    private static final Pattern NUMERIC_NUMBER = Pattern.compile("\\d{6,11}");
    private static final Pattern ALPHANUMERIC_NUMBER = Pattern.compile("[A-Za-z0-9]{5,20}");

    private final String databaseValue;
    private final String label;
    private final boolean numeric;

    DocumentType(String databaseValue, String label, boolean numeric) {
        this.databaseValue = databaseValue;
        this.label = label;
        this.numeric = numeric;
    }

    @Override
    public String getDatabaseValue() {
        return databaseValue;
    }

    public String getLabel() {
        return label;
    }

    /** TI and CC only have digits; CE and passport numbers can also have letters. */
    public boolean isValidNumber(String number) {
        Pattern pattern = numeric ? NUMERIC_NUMBER : ALPHANUMERIC_NUMBER;
        return number != null && pattern.matcher(number).matches();
    }

    public String getNumberRule() {
        return numeric ? "solo números, entre 6 y 11 dígitos" : "letras y números, entre 5 y 20 caracteres";
    }
}
