package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicRecordDAO;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.SubjectPerformance;
import com.littlehorizons.erp.service.AcademicReportService;
import com.littlehorizons.erp.util.ErrorLogger;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Academic performance report: general average of each subject and the subject(s) with the best one. */
public class AcademicReportController {

    private static final int AVERAGE_DECIMALS = 1;

    private final AcademicRecordDAO recordDAO;
    private final AcademicReportService reportService;

    public AcademicReportController(AcademicRecordDAO recordDAO, AcademicReportService reportService) {
        this.recordDAO = recordDAO;
        this.reportService = reportService;
    }

    /** @return the average of each subject (any period and student), from the highest to the lowest */
    public List<SubjectPerformance> subjectAverages() throws DataAccessException {
        return recordDAO.findAllWithSubject().stream()
                .collect(Collectors.groupingBy(record -> record.getSubject().getId()))
                .values().stream()
                .map(records -> new SubjectPerformance(records.get(0).getSubject(), average(records)))
                .sorted(Comparator.comparing(SubjectPerformance::average).reversed()
                        .thenComparing(performance -> performance.subject().getName()))
                .toList();
    }

    /** @return every subject with the highest average, so a tie returns all of them */
    public List<SubjectPerformance> bestSubjects(List<SubjectPerformance> averages) {
        return averages.stream()
                .map(SubjectPerformance::average)
                .max(Comparator.naturalOrder())
                .map(highest -> averages.stream()
                        .filter(performance -> performance.average().compareTo(highest) == 0)
                        .toList())
                .orElse(List.of());
    }

    /** Saves the report in a text file; if it fails, the error is logged and the user is informed. */
    public OperationResult saveReport(List<String> lines) {
        try {
            return OperationResult.ok("Reporte guardado en: " + reportService.write(lines));
        } catch (IOException e) {
            ErrorLogger.log("AcademicReportController.saveReport", e);
            return OperationResult.error("No se pudo guardar el archivo del reporte. El detalle quedó guardado en "
                    + ErrorLogger.LOG_FILE + ".");
        }
    }

    /** BigDecimal keeps the exact sum of the grades; the average is rounded to one decimal. */
    private static BigDecimal average(List<AcademicRecord> records) {
        BigDecimal sum = records.stream()
                .map(AcademicRecord::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(records.size()), AVERAGE_DECIMALS, RoundingMode.HALF_UP);
    }
}
