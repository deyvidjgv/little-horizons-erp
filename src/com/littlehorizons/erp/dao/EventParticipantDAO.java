package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.Placement;
import com.littlehorizons.erp.model.SchoolEvent;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Students who took part in each event (participantes_evento table). */
public class EventParticipantDAO extends BaseDAO {

    private static final String SELECT_PARTICIPANTS = "SELECT pe.id_participacion, pe.rol_participacion, "
            + "pe.posicion_obtenida, pe.observaciones, ev.id_evento, ev.nombre_evento, ev.fecha_evento, "
            + "ev.tipo_evento, ev.tiene_competencia, " + StudentDAO.SUMMARY_COLUMNS + " FROM participantes_evento pe "
            + "JOIN eventos ev ON ev.id_evento = pe.id_evento JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante";
    private static final String INSERT_PARTICIPANT = "INSERT INTO participantes_evento (id_evento, id_estudiante, "
            + "rol_participacion, posicion_obtenida) VALUES (?, ?, ?, ?)";

    public int create(EventParticipant participant) throws DataAccessException {
        return executeInsert(INSERT_PARTICIPANT, statement -> {
            statement.setInt(1, participant.getEvent().getId());
            statement.setInt(2, participant.getStudent().getId());
            statement.setString(3, participant.getRole().getDatabaseValue());
            statement.setString(4, participant.getPlacement() == null
                    ? null : participant.getPlacement().getDatabaseValue());
        }, "No se pudo registrar el participante.");
    }

    public Optional<EventParticipant> findById(int id) throws DataAccessException {
        return queryOne(SELECT_PARTICIPANTS + " WHERE pe.id_participacion = ?", statement -> statement.setInt(1, id),
                EventParticipantDAO::mapParticipant, "No se pudo buscar el participante.");
    }

    public List<EventParticipant> findByEvent(int eventId) throws DataAccessException {
        return queryList(SELECT_PARTICIPANTS + " WHERE pe.id_evento = ? ORDER BY e.primer_apellido, e.primer_nombre",
                statement -> statement.setInt(1, eventId), EventParticipantDAO::mapParticipant,
                "No se pudo consultar los participantes del evento.");
    }

    /** @return the participations of every student between the two dates, for the participation reports */
    public List<EventParticipant> findBetween(LocalDate from, LocalDate to) throws DataAccessException {
        return queryList(SELECT_PARTICIPANTS + " WHERE ev.fecha_evento BETWEEN ? AND ? ORDER BY ev.fecha_evento",
                statement -> {
                    statement.setObject(1, from);
                    statement.setObject(2, to);
                }, EventParticipantDAO::mapParticipant, "No se pudo consultar la participación en eventos.");
    }

    public boolean updatePlacement(int id, Placement placement) throws DataAccessException {
        return executeUpdate("UPDATE participantes_evento SET posicion_obtenida = ? WHERE id_participacion = ?",
                statement -> {
                    statement.setString(1, placement.getDatabaseValue());
                    statement.setInt(2, id);
                }, "No se pudo registrar la posición.") == 1;
    }

    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM participantes_evento WHERE id_participacion = ?",
                statement -> statement.setInt(1, id), "No se pudo quitar el participante.") == 1;
    }

    private static EventParticipant mapParticipant(ResultSet row) throws SQLException {
        SchoolEvent event = new SchoolEvent();
        event.setId(row.getInt("id_evento"));
        event.setName(row.getString("nombre_evento"));
        event.setDate(row.getObject("fecha_evento", LocalDate.class));
        event.setType(DatabaseValue.fromDatabaseValue(EventType.class, row.getString("tipo_evento")));
        event.setHasCompetition(row.getBoolean("tiene_competencia"));

        String placement = row.getString("posicion_obtenida");
        EventParticipant participant = new EventParticipant();
        participant.setId(row.getInt("id_participacion"));
        participant.setEvent(event);
        participant.setStudent(StudentDAO.mapStudentSummary(row));
        participant.setRole(DatabaseValue.fromDatabaseValue(ParticipationRole.class,
                row.getString("rol_participacion")));
        participant.setPlacement(placement == null ? null : DatabaseValue.fromDatabaseValue(Placement.class, placement));
        participant.setNotes(row.getString("observaciones"));
        return participant;
    }
}
