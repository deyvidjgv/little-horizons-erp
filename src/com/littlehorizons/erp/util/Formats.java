package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Formats used to type and show data in the console. Dates go day/month/year, as usual in Colombia. */
public final class Formats {

    /** Accepts one or two digits for day and month (6/8/2020 or 06/08/2020). STRICT rejects 31/02/2026. */
    public static final DateTimeFormatter INPUT_DATE =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    /** 24-hour clock; accepts 7:30 or 07:30. */
    public static final DateTimeFormatter INPUT_TIME =
            DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT);

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final String EMPTY_VALUE = "-";
    /** Pesos are shown without cents and with a dot every three digits, as usual in Colombia. */
    private static final DecimalFormat PESOS =
            new DecimalFormat("$ #,##0", DecimalFormatSymbols.getInstance(Locale.of("es", "CO")));

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date == null ? EMPTY_VALUE : date.format(DISPLAY_DATE);
    }

    public static String time(LocalTime time) {
        return time == null ? EMPTY_VALUE : time.format(DISPLAY_TIME);
    }

    public static String text(String value) {
        return value == null || value.isBlank() ? EMPTY_VALUE : value;
    }

    /** @return the value in Colombian pesos, for example "$ 350.000" */
    public static String money(BigDecimal value) {
        return value == null ? EMPTY_VALUE : PESOS.format(value);
    }

    /**
     * Reads a value in whole pesos; accepts dots as thousands separators (350.000) and a "$" sign.
     *
     * @return the value, or null if the text is not a whole number of pesos
     */
    public static BigDecimal parsePesos(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replace("$", "").replace(".", "").replace(" ", "");
        return digits.matches("\\d{1,12}") ? new BigDecimal(digits) : null;
    }
}
