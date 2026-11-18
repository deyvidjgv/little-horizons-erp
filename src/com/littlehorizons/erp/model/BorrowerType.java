package com.littlehorizons.erp.model;

/**
 * Who borrows a resource. The script keeps the loans of students and of teachers in separate tables
 * (prestamos_estudiante and prestamos_profesor), so each type knows its table and its person table.
 */
public enum BorrowerType {
    STUDENT("Estudiante", "prestamos_estudiante", "estudiantes", "id_estudiante"),
    TEACHER("Profesor", "prestamos_profesor", "profesores", "id_profesor");

    private final String label;
    private final String loanTable;
    private final String personTable;
    private final String personIdColumn;

    BorrowerType(String label, String loanTable, String personTable, String personIdColumn) {
        this.label = label;
        this.loanTable = loanTable;
        this.personTable = personTable;
        this.personIdColumn = personIdColumn;
    }

    public String getLabel() {
        return label;
    }

    public String getLoanTable() {
        return loanTable;
    }

    public String getPersonTable() {
        return personTable;
    }

    public String getPersonIdColumn() {
        return personIdColumn;
    }

    /** @return an empty Student or Teacher, to fill with the borrower's data */
    public Person newPerson() {
        return this == STUDENT ? new Student() : new Teacher();
    }
}
