package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.DocumentType;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.util.Validator;

import java.util.List;

/** Validations shared by the controllers of students, guardians and teachers. */
final class PersonRules {

    private PersonRules() {
    }

    /** @return null if the document and the names are valid; otherwise the reason */
    static String validateIdentity(Person person, List<DocumentType> allowedTypes) {
        DocumentType type = person.getDocumentType();
        if (type == null || !allowedTypes.contains(type)) {
            return "Tipo de documento no permitido.";
        }
        if (!type.isValidNumber(person.getDocumentNumber())) {
            return "Número de documento inválido para " + type.getLabel() + ": use " + type.getNumberRule() + ".";
        }
        if (!isName(person.getFirstName(), true) || !isName(person.getFirstSurname(), true)) {
            return "El primer nombre y el primer apellido son obligatorios. " + Validator.NAME_MESSAGE;
        }
        if (!isName(person.getMiddleName(), false) || !isName(person.getSecondSurname(), false)) {
            return "Segundo nombre o segundo apellido inválido. " + Validator.NAME_MESSAGE;
        }
        return null;
    }

    /** @return null if the address is filled and the optional phone and e-mail have a valid format */
    static String validateContact(String address, String phone, String email) {
        if (address == null || address.isBlank() || address.length() > Person.ADDRESS_MAX_LENGTH) {
            return "La dirección es obligatoria (máximo " + Person.ADDRESS_MAX_LENGTH + " caracteres).";
        }
        if (phone != null && !Validator.isPhone(phone)) {
            return Validator.PHONE_MESSAGE;
        }
        if (email != null && (email.length() > Person.EMAIL_MAX_LENGTH || !Validator.isEmail(email))) {
            return Validator.EMAIL_MESSAGE;
        }
        return null;
    }

    private static boolean isName(String value, boolean required) {
        if (value == null) {
            return !required;
        }
        return value.length() <= Person.NAME_MAX_LENGTH && Validator.isName(value);
    }
}
