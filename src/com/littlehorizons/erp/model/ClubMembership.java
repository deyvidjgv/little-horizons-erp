package com.littlehorizons.erp.model;

import java.time.LocalDate;

/** Row of the membresias_club table: a student in a club, from its join date until it leaves. */
public class ClubMembership {

    public static final int REASON_MAX_LENGTH = 500;

    private int id;
    private Student student;
    /** Only the id and the name of the club are loaded. */
    private Club club;
    private LocalDate joinDate;
    private MembershipStatus status;
    private LocalDate withdrawalDate;
    private String withdrawalReason;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Club getClub() {
        return club;
    }

    public void setClub(Club club) {
        this.club = club;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public MembershipStatus getStatus() {
        return status;
    }

    public void setStatus(MembershipStatus status) {
        this.status = status;
    }

    public LocalDate getWithdrawalDate() {
        return withdrawalDate;
    }

    public void setWithdrawalDate(LocalDate withdrawalDate) {
        this.withdrawalDate = withdrawalDate;
    }

    public String getWithdrawalReason() {
        return withdrawalReason;
    }

    public void setWithdrawalReason(String withdrawalReason) {
        this.withdrawalReason = withdrawalReason;
    }
}
