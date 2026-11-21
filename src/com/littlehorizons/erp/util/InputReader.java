package com.littlehorizons.erp.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Reads the user's answers from the console. Each method repeats the question until the value
 * is valid, so the views always receive correct data.
 *
 * When editing, the current value is shown in brackets: Enter keeps it and "-" clears an
 * optional field.
 */
public class InputReader {

    /** Typed in any question, cancels what the user was doing and goes back to the main menu. */
    public static final String MAIN_MENU_WORD = "menu";

    private static final String CLEAR_VALUE = "-";
    private static final String REQUIRED_MESSAGE = "[ERROR] Este dato es obligatorio.";

    private final Scanner scanner;

    public InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    /** @return a whole number between min and max, both included */
    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String text = readLine();
            try {
                int value = Integer.parseInt(text);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.println("[ERROR] Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("[ERROR] '" + text + "' no es un número entero válido.");
            }
        }
    }

    public String readText(String label, int maxLength) {
        return readText(label, maxLength, true, null, null, null);
    }

    public String readOptionalText(String label, int maxLength) {
        return readText(label, maxLength, false, null, null, null);
    }

    /**
     * @param rule        extra format rule, or null; ruleMessage is shown when it fails
     * @param current     value being edited, or null when the data is new
     * @return the text without spaces at the ends, or null when an optional field is left empty
     */
    public String readText(String label, int maxLength, boolean required, Predicate<String> rule,
                           String ruleMessage, String current) {
        while (true) {
            String text = ask(label, current);
            if (current != null && text.isEmpty()) {
                return current;
            }
            if (current != null && !required && text.equals(CLEAR_VALUE)) {
                return null;
            }
            if (text.isEmpty()) {
                if (!required) {
                    return null;
                }
                System.out.println(REQUIRED_MESSAGE);
            } else if (text.length() > maxLength) {
                System.out.println("[ERROR] Admite máximo " + maxLength + " caracteres.");
            } else if (rule != null && !rule.test(text)) {
                System.out.println("[ERROR] " + ruleMessage);
            } else {
                return text;
            }
        }
    }

    /** @param current date being edited, or null when the data is new */
    public LocalDate readDate(String label, boolean required, LocalDate current) {
        while (true) {
            String text = ask(label + " (dd/mm/aaaa)", current == null ? null : Formats.date(current));
            if (current != null && text.isEmpty()) {
                return current;
            }
            if (current != null && !required && text.equals(CLEAR_VALUE)) {
                return null;
            }
            if (text.isEmpty()) {
                if (!required) {
                    return null;
                }
                System.out.println(REQUIRED_MESSAGE);
                continue;
            }
            try {
                return LocalDate.parse(text, Formats.INPUT_DATE);
            } catch (DateTimeParseException e) {
                System.out.println("[ERROR] '" + text + "' no es una fecha válida. Use el formato dd/mm/aaaa.");
            }
        }
    }

    /** Reads a required hour in 24-hour format. */
    public LocalTime readTime(String label) {
        while (true) {
            String text = ask(label + " (hh:mm, formato 24 horas)", null);
            if (text.isEmpty()) {
                System.out.println(REQUIRED_MESSAGE);
                continue;
            }
            try {
                return LocalTime.parse(text, Formats.INPUT_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("[ERROR] '" + text + "' no es una hora válida. Use hh:mm, por ejemplo 07:30 o 14:00.");
            }
        }
    }

    /**
     * Shows the options numbered from 1.
     *
     * @param label   how each option is shown
     * @param current option being edited, or null; Enter keeps it
     */
    public <T> T readChoice(String title, List<T> options, Function<T, String> label, T current) {
        System.out.println(title + ":");
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + label.apply(options.get(i)));
        }
        while (true) {
            String text = ask("Opción", current == null ? null : label.apply(current));
            if (current != null && text.isEmpty()) {
                return current;
            }
            try {
                int number = Integer.parseInt(text);
                if (number >= 1 && number <= options.size()) {
                    return options.get(number - 1);
                }
            } catch (NumberFormatException e) {
                // Not a number: the message below asks again.
            }
            System.out.println("[ERROR] Elija un número entre 1 y " + options.size() + ".");
        }
    }

    public boolean readYesNo(String question) {
        return readYesNo(question, null);
    }

    /** @param current answer being edited, or null; Enter keeps it */
    public boolean readYesNo(String question, Boolean current) {
        while (true) {
            String shownCurrent = current == null ? null : (current ? "s" : "n");
            String text = ask(question + " (s/n)", shownCurrent).toLowerCase();
            if (current != null && text.isEmpty()) {
                return current;
            }
            if (text.equals("s")) {
                return true;
            }
            if (text.equals("n")) {
                return false;
            }
            System.out.println("[ERROR] Responda s (sí) o n (no).");
        }
    }

    /** Waits until the user presses Enter. */
    public void waitForEnter() {
        readLine();
    }

    private String ask(String label, String current) {
        System.out.print(current == null ? label + ": " : label + " [" + current + "]: ");
        return readLine();
    }

    /** @throws ReturnToMainMenuException if the user typed the main menu word */
    private String readLine() {
        String text = scanner.nextLine().trim();
        if (text.equalsIgnoreCase(MAIN_MENU_WORD)) {
            throw new ReturnToMainMenuException();
        }
        return text;
    }
}
