package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Grades of a student in one subject during a year.
 *
 * @param grades  grade of each period that has one
 * @param average average of the subject (null when there are no grades)
 */
public record ReportCardRow(Subject subject, Map<AcademicPeriod, BigDecimal> grades, BigDecimal average) {
}
