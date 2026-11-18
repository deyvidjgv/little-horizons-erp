package com.littlehorizons.erp.model;

/** A guardian linked to a student, with the flags of the estudiante_acudiente table. */
public class StudentGuardian {

    private final Guardian guardian;
    private final boolean primary;
    private final boolean emergencyContact;

    public StudentGuardian(Guardian guardian, boolean primary, boolean emergencyContact) {
        this.guardian = guardian;
        this.primary = primary;
        this.emergencyContact = emergencyContact;
    }

    public Guardian getGuardian() {
        return guardian;
    }

    /** The primary guardian is the one the school contacts first. */
    public boolean isPrimary() {
        return primary;
    }

    public boolean isEmergencyContact() {
        return emergencyContact;
    }
}
