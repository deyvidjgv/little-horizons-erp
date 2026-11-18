package com.littlehorizons.erp.util;

import java.util.regex.Pattern;

/**
 * Format rules shared by the views (to ask again when a value is wrong) and the controllers
 * (to never save invalid data, no matter who calls them).
 */
public final class Validator {

    public static final String NAME_MESSAGE = "Solo se permiten letras, espacios, apóstrofos y guiones.";
    public static final String PHONE_MESSAGE = "El teléfono debe tener entre 7 y 15 dígitos (puede empezar con +).";
    public static final String EMAIL_MESSAGE = "El correo no tiene un formato válido (ejemplo: nombre@dominio.com).";
    public static final String BLOOD_TYPE_MESSAGE = "El tipo de sangre debe ser A, B, AB u O seguido de + o - (ejemplo: O+).";

    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L} '-]*");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{7,15}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern BLOOD_TYPE = Pattern.compile("(A|B|AB|O)[+-]");

    private Validator() {
    }

    public static boolean isName(String value) {
        return matches(NAME, value);
    }

    public static boolean isPhone(String value) {
        return matches(PHONE, value);
    }

    public static boolean isEmail(String value) {
        return matches(EMAIL, value);
    }

    public static boolean isBloodType(String value) {
        return matches(BLOOD_TYPE, value);
    }

    private static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }
}
