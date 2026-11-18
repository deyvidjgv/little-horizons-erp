package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Person;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.InputReader;

import java.util.Optional;

/** Asks for a student by document number. Used by the views of students, guardians and enrollments. */
final class StudentSelector {

    private final InputReader input;
    private final StudentController controller;

    StudentSelector(InputReader input, StudentController controller) {
        this.input = input;
        this.controller = controller;
    }

    /** Shows an error and returns empty if no student has that document. */
    Optional<Student> ask() throws DataAccessException {
        String document = input.readText("Número de documento del estudiante", Person.DOCUMENT_MAX_LENGTH);
        Optional<Student> student = controller.findByDocument(document);
        if (student.isEmpty()) {
            Screen.error("No existe un estudiante con el documento " + document + ".");
        }
        return student;
    }
}
