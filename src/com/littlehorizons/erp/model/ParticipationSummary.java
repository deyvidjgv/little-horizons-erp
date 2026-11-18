package com.littlehorizons.erp.model;

/**
 * Participation of a student in the events of a year.
 *
 * @param events  how many events the student took part in
 * @param podiums how many times the student got 1er, 2do or 3er Lugar
 */
public record ParticipationSummary(Student student, long events, long podiums) {
}
