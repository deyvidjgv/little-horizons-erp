package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.SchoolEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** CRUD of the eventos table; each event comes with its organizer and how many participants it has. */
public class EventDAO extends BaseDAO {

    private static final String SELECT_EVENTS = "SELECT ev.id_evento, ev.nombre_evento, ev.descripcion, ev.tipo_evento, "
            + "ev.fecha_evento, ev.lugar, ev.tiene_competencia, ev.esta_activo, " + TeacherDAO.SUMMARY_COLUMNS + ", "
            + "(SELECT COUNT(*) FROM participantes_evento pe WHERE pe.id_evento = ev.id_evento) AS participantes "
            + "FROM eventos ev LEFT JOIN profesores p ON p.id_profesor = ev.id_organizador";
    private static final String INSERT_EVENT = "INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, "
            + "fecha_evento, lugar, tiene_competencia, id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_EVENT = "UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, "
            + "fecha_evento = ?, lugar = ?, tiene_competencia = ?, id_organizador = ?, esta_activo = ? "
            + "WHERE id_evento = ?";

    /** @return the id generated for the event */
    public int create(SchoolEvent event) throws DataAccessException {
        return executeInsert(INSERT_EVENT, statement -> fillData(statement, event), "No se pudo registrar el evento.");
    }

    public Optional<SchoolEvent> findById(int id) throws DataAccessException {
        return queryOne(SELECT_EVENTS + " WHERE ev.id_evento = ?", statement -> statement.setInt(1, id),
                EventDAO::mapEvent, "No se pudo buscar el evento.");
    }

    /** @return the events between the two dates (both included), in date order */
    public List<SchoolEvent> findBetween(LocalDate from, LocalDate to) throws DataAccessException {
        return queryList(SELECT_EVENTS + " WHERE ev.fecha_evento BETWEEN ? AND ? ORDER BY ev.fecha_evento, ev.nombre_evento",
                statement -> {
                    statement.setObject(1, from);
                    statement.setObject(2, to);
                }, EventDAO::mapEvent, "No se pudo consultar los eventos.");
    }

    public boolean update(SchoolEvent event) throws DataAccessException {
        return executeUpdate(UPDATE_EVENT, statement -> {
            fillData(statement, event);
            statement.setInt(9, event.getId());
        }, "No se pudo actualizar el evento.") == 1;
    }

    /** The participants are deleted by the database (ON DELETE CASCADE). */
    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM eventos WHERE id_evento = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el evento.") == 1;
    }

    /** Fills parameters 1 to 8, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, SchoolEvent event) throws SQLException {
        statement.setString(1, event.getName());
        statement.setString(2, event.getDescription());
        statement.setString(3, event.getType().getDatabaseValue());
        statement.setObject(4, event.getDate());
        statement.setString(5, event.getPlace());
        statement.setBoolean(6, event.hasCompetition());
        if (event.getOrganizer() == null) {
            statement.setNull(7, Types.INTEGER);
        } else {
            statement.setInt(7, event.getOrganizer().getId());
        }
        statement.setBoolean(8, event.isActive());
    }

    private static SchoolEvent mapEvent(ResultSet row) throws SQLException {
        SchoolEvent event = new SchoolEvent();
        event.setId(row.getInt("id_evento"));
        event.setName(row.getString("nombre_evento"));
        event.setDescription(row.getString("descripcion"));
        event.setType(DatabaseValue.fromDatabaseValue(EventType.class, row.getString("tipo_evento")));
        event.setDate(row.getObject("fecha_evento", LocalDate.class));
        event.setPlace(row.getString("lugar"));
        event.setHasCompetition(row.getBoolean("tiene_competencia"));
        event.setOrganizer(TeacherDAO.mapTeacherSummary(row));
        event.setActive(row.getBoolean("esta_activo"));
        event.setParticipants(row.getInt("participantes"));
        return event;
    }
}
