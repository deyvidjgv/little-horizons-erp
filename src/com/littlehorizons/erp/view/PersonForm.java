package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.Validator;

import java.util.List;
import java.util.function.Function;

/**
 * Questions shared by the forms of students, guardians and teachers.
 * When current is not null the form is editing, and each question shows the current value.
 */
final class PersonForm {

    private final InputReader input;

    PersonForm(InputReader input) {
        this.input = input;
    }

    void readDocument(Person person, List<DocumentType> allowedTypes, Person current) {
        DocumentType type = input.readChoice("Tipo de documento", allowedTypes, DocumentType::getLabel,
                valueOf(current, Person::getDocumentType));
        person.setDocumentType(type);
        person.setDocumentNumber(input.readText("Número de documento", Person.DOCUMENT_MAX_LENGTH, true,
                type::isValidNumber, "Para " + type.getLabel() + " use " + type.getNumberRule() + ".",
                valueOf(current, Person::getDocumentNumber)));
    }

    void readNames(Person person, Person current) {
        person.setFirstName(readName("Primer nombre", true, valueOf(current, Person::getFirstName)));
        person.setMiddleName(readName("Segundo nombre (opcional)", false, valueOf(current, Person::getMiddleName)));
        person.setFirstSurname(readName("Primer apellido", true, valueOf(current, Person::getFirstSurname)));
        person.setSecondSurname(readName("Segundo apellido (opcional)", false,
                valueOf(current, Person::getSecondSurname)));
    }

    String readPhone(String label, boolean required, String current) {
        return input.readText(label, Person.PHONE_MAX_LENGTH, required, Validator::isPhone,
                Validator.PHONE_MESSAGE, current);
    }

    String readEmail(String current) {
        return input.readText("Correo electrónico (opcional)", Person.EMAIL_MAX_LENGTH, false, Validator::isEmail,
                Validator.EMAIL_MESSAGE, current);
    }

    /** Students and guardians must have an address (the guardian's address goes on the invoice). */
    String readAddress(String current) {
        return input.readText("Dirección", Person.ADDRESS_MAX_LENGTH, true, null, null, current);
    }

    static void printEditHint() {
        System.out.println("Enter conserva el valor actual; '-' borra un dato opcional.");
    }

    private String readName(String label, boolean required, String current) {
        return input.readText(label, Person.NAME_MAX_LENGTH, required, Validator::isName, Validator.NAME_MESSAGE,
                current);
    }

    private static <T> T valueOf(Person current, Function<Person, T> getter) {
        return current == null ? null : getter.apply(current);
    }
}
