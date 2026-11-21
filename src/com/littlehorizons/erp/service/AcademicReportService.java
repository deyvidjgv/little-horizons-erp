package com.littlehorizons.erp.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Saves the academic performance report as a text file. */
public class AcademicReportService {

    /** Folder of the project where the reports are saved. */
    public static final Path REPORT_FOLDER = Path.of("reportes");

    // With milliseconds, two reports generated in the same second do not overwrite each other.
    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd_HHmmss_SSS");

    /** @return the absolute path of the file, for example ...\reportes\reporte_rendimiento_2026-11-20_183015_042.txt */
    public Path write(List<String> lines) throws IOException {
        Files.createDirectories(REPORT_FOLDER);
        Path file = REPORT_FOLDER.resolve("reporte_rendimiento_" + LocalDateTime.now().format(FILE_DATE) + ".txt");
        Files.write(file, lines, StandardCharsets.UTF_8);
        return file.toAbsolutePath();
    }
}
