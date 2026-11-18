package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.Weekday;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.List;

/** Class schedules of the assignments (horarios_asignacion table). */
public class ScheduleDAO extends BaseDAO {

    private static final String SELECT_SCHEDULES = "SELECT h.id_horario, h.dia_semana, h.hora_inicio, h.hora_fin, "
            + AssignmentDAO.COLUMNS + " FROM horarios_asignacion h "
            + "JOIN asignaciones_profesor ap ON ap.id_asignacion = h.id_asignacion " + AssignmentDAO.JOINS;
    // dia_semana is an ENUM: MySQL sorts it in the order of its values (Lunes to Viernes).
    private static final String ORDER_BY_TIME = " ORDER BY h.dia_semana, h.hora_inicio";
    private static final String INSERT_SCHEDULE = "INSERT INTO horarios_asignacion "
            + "(id_asignacion, dia_semana, hora_inicio, hora_fin) VALUES (?, ?, ?, ?)";

    /** @return the id generated for the schedule */
    public int create(ClassSchedule schedule) throws DataAccessException {
        return executeInsert(INSERT_SCHEDULE, statement -> {
            statement.setInt(1, schedule.getAssignment().getId());
            statement.setString(2, schedule.getDay().getDatabaseValue());
            statement.setObject(3, schedule.getStartTime());
            statement.setObject(4, schedule.getEndTime());
        }, "No se pudo registrar el horario.");
    }

    public List<ClassSchedule> findByAssignment(int assignmentId) throws DataAccessException {
        return queryList(SELECT_SCHEDULES + " WHERE h.id_asignacion = ?" + ORDER_BY_TIME,
                statement -> statement.setInt(1, assignmentId), ScheduleDAO::mapSchedule,
                "No se pudo consultar el horario de la asignación.");
    }

    /** @return every class of the teacher in that year, from Monday to Friday */
    public List<ClassSchedule> findByTeacherAndYear(int teacherId, int year) throws DataAccessException {
        return queryList(SELECT_SCHEDULES + " WHERE ap.id_profesor = ? AND ap.año_academico = ?" + ORDER_BY_TIME,
                statement -> {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, year);
                }, ScheduleDAO::mapSchedule, "No se pudo consultar el horario del profesor.");
    }

    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM horarios_asignacion WHERE id_horario = ?",
                statement -> statement.setInt(1, id), "No se pudo eliminar el horario.") == 1;
    }

    private static ClassSchedule mapSchedule(ResultSet row) throws SQLException {
        ClassSchedule schedule = new ClassSchedule();
        schedule.setId(row.getInt("id_horario"));
        schedule.setAssignment(AssignmentDAO.mapAssignment(row));
        schedule.setDay(DatabaseValue.fromDatabaseValue(Weekday.class, row.getString("dia_semana")));
        schedule.setStartTime(row.getObject("hora_inicio", LocalTime.class));
        schedule.setEndTime(row.getObject("hora_fin", LocalTime.class));
        return schedule;
    }
}
