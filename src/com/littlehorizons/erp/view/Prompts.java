package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.math.BigDecimal;

/** Questions repeated in several menus. */
final class Prompts {

    private static final int PESOS_MAX_LENGTH = 15;

    private Prompts() {
    }

    static int readAcademicYear(InputReader input) {
        return input.readInt("Año académico (" + AcademicYear.FIRST + "-" + AcademicYear.last() + "): ",
                AcademicYear.FIRST, AcademicYear.last());
    }

    /**
     * Reads a value in whole pesos (350000 or 350.000).
     *
     * @param current value shown as default (Enter keeps it), or null
     */
    static BigDecimal readPesos(InputReader input, String label, boolean allowZero, BigDecimal current) {
        String rule = "Escriba un valor en pesos sin centavos" + (allowZero ? "" : ", mayor que 0")
                + " (ejemplo: 350000).";
        String text = input.readText(label, PESOS_MAX_LENGTH, true, value -> isValidPesos(value, allowZero), rule,
                current == null ? null : current.stripTrailingZeros().toPlainString());
        return Formats.parsePesos(text);
    }

    private static boolean isValidPesos(String value, boolean allowZero) {
        BigDecimal amount = Formats.parsePesos(value);
        return amount != null && amount.compareTo(Payment.MAX_AMOUNT) <= 0 && (allowZero || amount.signum() > 0);
    }
}
