package com.littlehorizons.erp.model;

import java.time.Duration;
import java.time.LocalTime;

/** Row of the horarios_asignacion table: one class of an assignment in the week. */
public class ClassSchedule {

    private int id;
    private Assignment assignment;
    private Weekday day;
    private LocalTime startTime;
    private LocalTime endTime;

    /**
     * Two classes cross when they are on the same day and each one starts before the other ends.
     * A class that starts just when another ends (08:00-09:00 after 07:00-08:00) does not cross.
     */
    public boolean overlaps(Weekday otherDay, LocalTime otherStart, LocalTime otherEnd) {
        return day == otherDay && startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }

    public long getDurationMinutes() {
        return Duration.between(startTime, endTime).toMinutes();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    public Weekday getDay() {
        return day;
    }

    public void setDay(Weekday day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
