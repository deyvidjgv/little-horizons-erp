package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** General average of a subject, with all its grades (any period and student). */
public record SubjectPerformance(Subject subject, BigDecimal average) {
}
