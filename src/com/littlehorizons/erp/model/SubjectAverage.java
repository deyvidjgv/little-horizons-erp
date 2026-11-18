package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/**
 * General average of a course (grade) in one subject and period.
 *
 * @param grades       how many grades were averaged
 * @param belowPassing how many of them are under 3.0
 */
public record SubjectAverage(Subject subject, BigDecimal average, long grades, long belowPassing) {
}
