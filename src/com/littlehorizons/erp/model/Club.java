package com.littlehorizons.erp.model;

/** Row of the clubes table, with how many active members it has now. */
public class Club {

    public static final int NAME_MAX_LENGTH = 100;
    public static final int SCHEDULE_MAX_LENGTH = 100;
    /** descripcion is a TEXT column; the application keeps it short to fit on screen. */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private int id;
    private String name;
    private String description;
    /** Teacher in charge; only the id and the names are loaded. */
    private Teacher supervisor;
    private int capacity;
    private String meetingSchedule;
    private boolean active;
    private int activeMembers;

    public boolean isFull() {
        return activeMembers >= capacity;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Teacher getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(Teacher supervisor) {
        this.supervisor = supervisor;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getMeetingSchedule() {
        return meetingSchedule;
    }

    public void setMeetingSchedule(String meetingSchedule) {
        this.meetingSchedule = meetingSchedule;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getActiveMembers() {
        return activeMembers;
    }

    public void setActiveMembers(int activeMembers) {
        this.activeMembers = activeMembers;
    }
}
