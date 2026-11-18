package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Grades of a student in one academic year, with the averages the system calculates.
 *
 * @param rows           one row per subject, with its average
 * @param periodAverages general average of the student in each period
 */
public record ReportCard(int academicYear, List<ReportCardRow> rows, Map<AcademicPeriod, BigDecimal> periodAverages) {
}
