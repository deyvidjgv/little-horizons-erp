package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.InputReader;

import java.util.Optional;

/** Asks for a teacher by document number. */
final class TeacherSelector {

    private final InputReader input;
    private final TeacherController controller;

    TeacherSelector(InputReader input, TeacherController controller) {
        this.input = input;
        this.controller = controller;
    }

    /** Shows an error and returns empty if no teacher has that document. */
    Optional<Teacher> ask() throws DataAccessException {
        String document = input.readText("Número de documento del profesor", Person.DOCUMENT_MAX_LENGTH);
        Optional<Teacher> teacher = controller.findByDocument(document);
        if (teacher.isEmpty()) {
            Screen.error("No existe un profesor con el documento " + document + ".");
        }
        return teacher;
    }
}
