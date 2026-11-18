package com.littlehorizons.erp.model;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Data shared by students, guardians and teachers: the identity document and the names.
 * The length limits match the columns of the database.
 */
public abstract class Person {

    public static final int DOCUMENT_MAX_LENGTH = 20;
    public static final int NAME_MAX_LENGTH = 100;
    public static final int ADDRESS_MAX_LENGTH = 255;
    public static final int PHONE_MAX_LENGTH = 20;
    public static final int EMAIL_MAX_LENGTH = 100;

    /** 0 while the person has not been saved. */
    private int id;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String middleName;
    private String firstSurname;
    private String secondSurname;

    /** @return the names and surnames that are filled in, separated by one space */
    public String getFullName() {
        return Stream.of(firstName, middleName, firstSurname, secondSurname)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }

    /** @return the type and the number, for example "TI 1096749007" */
    public String getDocument() {
        return documentType.getDatabaseValue() + " " + documentNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getFirstSurname() {
        return firstSurname;
    }

    public void setFirstSurname(String firstSurname) {
        this.firstSurname = firstSurname;
    }

    public String getSecondSurname() {
        return secondSurname;
    }

    public void setSecondSurname(String secondSurname) {
        this.secondSurname = secondSurname;
    }
}
