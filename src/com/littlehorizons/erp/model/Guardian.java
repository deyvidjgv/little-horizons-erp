package com.littlehorizons.erp.model;

/** Row of the acudientes table: father, mother or legal guardian of one or more students. */
public class Guardian extends Person {

    public static final int RELATIONSHIP_MAX_LENGTH = 50;
    public static final int OCCUPATION_MAX_LENGTH = 100;

    private String relationship;
    private String mainPhone;
    private String secondaryPhone;
    private String email;
    private String address;
    private String occupation;
    private boolean financiallyResponsible;

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getMainPhone() {
        return mainPhone;
    }

    public void setMainPhone(String mainPhone) {
        this.mainPhone = mainPhone;
    }

    public String getSecondaryPhone() {
        return secondaryPhone;
    }

    public void setSecondaryPhone(String secondaryPhone) {
        this.secondaryPhone = secondaryPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public boolean isFinanciallyResponsible() {
        return financiallyResponsible;
    }

    public void setFinanciallyResponsible(boolean financiallyResponsible) {
        this.financiallyResponsible = financiallyResponsible;
    }
}
