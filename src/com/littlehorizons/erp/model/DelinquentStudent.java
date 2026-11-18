package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * A student with an active enrollment whose last payment of a recurring concept is more than a month old.
 *
 * @param lastPayment date of the last payment of the concept, or null if the student never paid it
 */
public record DelinquentStudent(Student student, Grade grade, LocalDate lastPayment) {
}
