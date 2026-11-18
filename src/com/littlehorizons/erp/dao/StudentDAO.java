package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Gender;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** CRUD of the estudiantes table. */
public class StudentDAO extends BaseDAO {

    private static final List<String> COLUMNS = List.of("id_estudiante", "tipo_documento", "numero_documento",
            "primer_nombre", "segundo_nombre", "primer_apellido", "segundo_apellido", "fecha_nacimiento", "genero",
            "direccion", "telefono", "correo_electronico", "tipo_sangre", "alergias", "condiciones_medicas",
            "fecha_ingreso", "estado");

    private static final String SELECT_STUDENTS = "SELECT " + columns("e") + " FROM estudiantes e";
    private static final String ORDER_BY_NAME = " ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";
    private static final String INSERT_STUDENT = "INSERT INTO estudiantes (tipo_documento, numero_documento, "
            + "primer_nombre, segundo_nombre, primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, "
            + "telefono, correo_electronico, tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_GUARDIAN_LINK = "INSERT INTO estudiante_acudiente "
            + "(id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_STUDENT = "UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, "
            + "primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, "
            + "fecha_nacimiento = ?, genero = ?, direccion = ?, telefono = ?, correo_electronico = ?, "
            + "tipo_sangre = ?, alergias = ?, condiciones_medicas = ?, fecha_ingreso = ? WHERE id_estudiante = ?";
    private static final String HAS_HISTORY = "SELECT EXISTS (SELECT 1 FROM matriculas WHERE id_estudiante = ?) "
            + "OR EXISTS (SELECT 1 FROM registros_academicos WHERE id_estudiante = ?) "
            + "OR EXISTS (SELECT 1 FROM pagos WHERE id_estudiante = ?) "
            + "OR EXISTS (SELECT 1 FROM prestamos_estudiante WHERE id_estudiante = ?)";
    private static final int HISTORY_TABLES = 4;

    /**
     * Identity and status of the student (alias e), for the queries that only need to show who it is.
     * The status goes as estado_estudiante because the other table of the JOIN also has an estado column.
     */
    static final String SUMMARY_COLUMNS = "e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, "
            + "e.segundo_nombre, e.primer_apellido, e.segundo_apellido, e.estado AS estado_estudiante";

    /** @return the student columns with the table alias in front, for queries with JOIN */
    static String columns(String alias) {
        return COLUMNS.stream().map(column -> alias + "." + column).collect(Collectors.joining(", "));
    }

    /**
     * Saves the student and its first guardian in one transaction, because every student must have
     * at least one guardian.
     *
     * @return the id generated for the student
     */
    public int create(Student student, int guardianId, boolean primary, boolean emergencyContact)
            throws DataAccessException {
        return inTransaction(connection -> {
            int studentId = insert(connection, INSERT_STUDENT, statement -> {
                fillPersonalData(statement, student);
                statement.setString(16, student.getStatus().getDatabaseValue());
            });
            update(connection, INSERT_GUARDIAN_LINK, statement -> {
                statement.setInt(1, studentId);
                statement.setInt(2, guardianId);
                statement.setBoolean(3, primary);
                statement.setBoolean(4, emergencyContact);
            });
            return studentId;
        }, "No se pudo registrar el estudiante.");
    }

    public Optional<Student> findById(int id) throws DataAccessException {
        return queryOne(SELECT_STUDENTS + " WHERE e.id_estudiante = ?", statement -> statement.setInt(1, id),
                StudentDAO::mapStudent, "No se pudo buscar el estudiante.");
    }

    public Optional<Student> findByDocument(String documentNumber) throws DataAccessException {
        return queryOne(SELECT_STUDENTS + " WHERE e.numero_documento = ?",
                statement -> statement.setString(1, documentNumber),
                StudentDAO::mapStudent, "No se pudo buscar el estudiante.");
    }

    public List<Student> findAll() throws DataAccessException {
        return queryList(SELECT_STUDENTS + ORDER_BY_NAME, statement -> { },
                StudentDAO::mapStudent, "No se pudo consultar los estudiantes.");
    }

    /** Updates the personal data. The status only changes with the enrollment operations. */
    public boolean update(Student student) throws DataAccessException {
        return executeUpdate(UPDATE_STUDENT, statement -> {
            fillPersonalData(statement, student);
            statement.setInt(16, student.getId());
        }, "No se pudo actualizar el estudiante.") == 1;
    }

    /** The links with guardians are deleted by the database (ON DELETE CASCADE). */
    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM estudiantes WHERE id_estudiante = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el estudiante.") == 1;
    }

    /**
     * @return true if the student has enrollments, grades, payments or loans. They must be kept: deleting
     * the student would delete them too (ON DELETE CASCADE), and a deleted loan would leave its resource
     * as Prestado forever.
     */
    public boolean hasHistory(int id) throws DataAccessException {
        return queryOne(HAS_HISTORY, statement -> {
            for (int index = 1; index <= HISTORY_TABLES; index++) {
                statement.setInt(index, id);
            }
        }, row -> row.getBoolean(1), "No se pudo revisar el historial del estudiante.").orElse(false);
    }

    /** Fills parameters 1 to 15, in the same order in the INSERT and in the UPDATE. */
    private static void fillPersonalData(PreparedStatement statement, Student student) throws SQLException {
        fillIdentity(statement, student);
        statement.setObject(7, student.getBirthDate());
        statement.setString(8, student.getGender().getDatabaseValue());
        statement.setString(9, student.getAddress());
        statement.setString(10, student.getPhone());
        statement.setString(11, student.getEmail());
        statement.setString(12, student.getBloodType());
        statement.setString(13, student.getAllergies());
        statement.setString(14, student.getMedicalConditions());
        statement.setObject(15, student.getEntryDate());
    }

    /** Maps the SUMMARY_COLUMNS: the rest of the student data stays empty. */
    static Student mapStudentSummary(ResultSet row) throws SQLException {
        Student student = new Student();
        mapIdentity(row, student, "id_estudiante");
        student.setStatus(DatabaseValue.fromDatabaseValue(StudentStatus.class, row.getString("estado_estudiante")));
        return student;
    }

    static Student mapStudent(ResultSet row) throws SQLException {
        Student student = new Student();
        mapIdentity(row, student, "id_estudiante");
        student.setBirthDate(row.getObject("fecha_nacimiento", LocalDate.class));
        student.setGender(DatabaseValue.fromDatabaseValue(Gender.class, row.getString("genero")));
        student.setAddress(row.getString("direccion"));
        student.setPhone(row.getString("telefono"));
        student.setEmail(row.getString("correo_electronico"));
        student.setBloodType(row.getString("tipo_sangre"));
        student.setAllergies(row.getString("alergias"));
        student.setMedicalConditions(row.getString("condiciones_medicas"));
        student.setEntryDate(row.getObject("fecha_ingreso", LocalDate.class));
        student.setStatus(DatabaseValue.fromDatabaseValue(StudentStatus.class, row.getString("estado")));
        return student;
    }
}
