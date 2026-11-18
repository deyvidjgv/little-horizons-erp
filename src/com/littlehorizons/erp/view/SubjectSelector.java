package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.SubjectController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.util.InputReader;

import java.util.List;
import java.util.Optional;

/** Asks for a subject choosing first the grade and then one of its active subjects. */
final class SubjectSelector {

    private final InputReader input;
    private final SubjectController controller;

    SubjectSelector(InputReader input, SubjectController controller) {
        this.input = input;
        this.controller = controller;
    }

    Optional<Subject> ask() throws DataAccessException {
        Grade grade = input.readChoice("Grado", controller.listGrades(), Grade::getName, null);
        List<Subject> subjects = controller.listActiveByGrade(grade.getId());
        if (subjects.isEmpty()) {
            Screen.error("El grado " + grade.getName() + " no tiene materias activas.");
            return Optional.empty();
        }
        return Optional.of(input.readChoice("Materia", subjects,
                subject -> subject.getCode() + " " + subject.getName(), null));
    }
}
