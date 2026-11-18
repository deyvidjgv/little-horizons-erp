package com.littlehorizons.erp.model;

import com.littlehorizons.erp.util.Formats;

import java.time.LocalDate;

/** Row of the eventos table (Feria de Ciencias, Día Cultural...), with how many participants it has. */
public class SchoolEvent {

    public static final int NAME_MAX_LENGTH = 150;
    public static final int PLACE_MAX_LENGTH = 150;
    /** descripcion is a TEXT column; the application keeps it short to fit on screen. */
    public static final int DESCRIPTION_MAX_LENGTH = 500;

    private int id;
    private String name;
    private String description;
    private EventType type;
    private LocalDate date;
    private String place;
    private boolean hasCompetition;
    /** Organizing teacher, optional; only the id and the names are loaded. */
    private Teacher organizer;
    private boolean active;
    private int participants;

    /** @return for example "Feria de Ciencias 2026 (04/09/2026)" */
    public String getDisplayName() {
        return name + " (" + Formats.date(date) + ")";
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

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public boolean hasCompetition() {
        return hasCompetition;
    }

    public void setHasCompetition(boolean hasCompetition) {
        this.hasCompetition = hasCompetition;
    }

    public Teacher getOrganizer() {
        return organizer;
    }

    public void setOrganizer(Teacher organizer) {
        this.organizer = organizer;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getParticipants() {
        return participants;
    }

    public void setParticipants(int participants) {
        this.participants = participants;
    }
}
