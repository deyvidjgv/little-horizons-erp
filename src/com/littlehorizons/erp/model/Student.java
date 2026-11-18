package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.time.Period;

/** Row of the estudiantes table. */
public class Student extends Person {

    public static final int BLOOD_TYPE_MAX_LENGTH = 5;
    /** Allergies and medical conditions are TEXT columns; the application keeps them short to fit on screen. */
    public static final int HEALTH_TEXT_MAX_LENGTH = 500;

    private LocalDate birthDate;
    private Gender gender;
    private String address;
    private String phone;
    private String email;
    private String bloodType;
    private String allergies;
    private String medicalConditions;
    private LocalDate entryDate;
    private StudentStatus status;

    /** @return the age in whole years on the current date */
    public int getAge() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalConditions() {
        return medicalConditions;
    }

    public void setMedicalConditions(String medicalConditions) {
        this.medicalConditions = medicalConditions;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public StudentStatus getStatus() {
        return status;
    }

    public void setStatus(StudentStatus status) {
        this.status = status;
    }
}
