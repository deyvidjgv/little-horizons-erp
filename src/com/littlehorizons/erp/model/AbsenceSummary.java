package com.littlehorizons.erp.model;

import java.util.List;

/**
 * A student with worrying absences in a week.
 *
 * @param subjects codes of the subjects the student missed
 */
public record AbsenceSummary(Student student, long absences, List<String> subjects) {
}
