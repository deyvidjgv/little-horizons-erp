package com.littlehorizons.erp.model;

/** Row of the participantes_evento table: a student who took part in an event. */
public class EventParticipant {

    private int id;
    /** Only the id, name, date and competition flag of the event are loaded. */
    private SchoolEvent event;
    private Student student;
    private ParticipationRole role;
    /** Only for events with competition; null in the others. */
    private Placement placement;
    private String notes;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public SchoolEvent getEvent() {
        return event;
    }

    public void setEvent(SchoolEvent event) {
        this.event = event;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public ParticipationRole getRole() {
        return role;
    }

    public void setRole(ParticipationRole role) {
        this.role = role;
    }

    public Placement getPlacement() {
        return placement;
    }

    public void setPlacement(Placement placement) {
        this.placement = placement;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
