package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AssignmentController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Assignment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.InputReader;

import java.util.List;
import java.util.Optional;

/** Lets the user choose one of the groups (subject and section) of a teacher in a year. */
final class AssignmentSelector {

    private final InputReader input;
    private final AssignmentController controller;

    AssignmentSelector(InputReader input, AssignmentController controller) {
        this.input = input;
        this.controller = controller;
    }

    /** Shows an error and returns empty if the teacher has no subjects that year. */
    Optional<Assignment> choose(Teacher teacher, int year) throws DataAccessException {
        List<Assignment> assignments = controller.listAssignments(teacher.getId(), year);
        if (assignments.isEmpty()) {
            Screen.error(teacher.getFullName() + " no tiene materias asignadas en " + year + ".");
            return Optional.empty();
        }
        return Optional.of(input.readChoice("Grupo (materia y sección)", assignments, Assignment::getDisplayName,
                null));
    }
}
