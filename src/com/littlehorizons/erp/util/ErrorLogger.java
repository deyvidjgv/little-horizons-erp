package com.littlehorizons.erp.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes the application errors to error_log.txt, in the folder where the program runs.
 * The user only sees a short message on screen; the full detail (stack trace) stays in the file.
 */
public final class ErrorLogger {

    public static final String LOG_FILE = "error_log.txt";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ErrorLogger() {
    }

    /** @param source where the error happened, for example "StudentDAO.create" */
    public static void log(String source, Exception error) {
        // The file is opened in append mode, so the errors of previous runs are kept.
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, StandardCharsets.UTF_8, true))) {
            writer.printf("[%s] ERROR in %s: %s%n",
                    LocalDateTime.now().format(TIMESTAMP_FORMAT), source, error.getMessage());
            error.printStackTrace(writer);
            writer.println();
        } catch (IOException e) {
            System.out.println("No se pudo escribir en " + LOG_FILE + ": " + e.getMessage());
        }
    }
}
