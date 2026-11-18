package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.EventDAO;
import com.littlehorizons.erp.dao.EventParticipantDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.TeacherDAO;
import com.littlehorizons.erp.model.AcademicYear;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.ParticipationSummary;
import com.littlehorizons.erp.model.Placement;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Event calendar and participants: each event has a type, and the events with competition record the
 * position of each participant (1er, 2do, 3er Lugar or Participante).
 */
public class EventController {

    private final EventDAO eventDAO;
    private final EventParticipantDAO participantDAO;
    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;

    public EventController(EventDAO eventDAO, EventParticipantDAO participantDAO, TeacherDAO teacherDAO,
                           StudentDAO studentDAO) {
        this.eventDAO = eventDAO;
        this.participantDAO = participantDAO;
        this.teacherDAO = teacherDAO;
        this.studentDAO = studentDAO;
    }

    public List<SchoolEvent> listByYear(int year) throws DataAccessException {
        return eventDAO.findBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    /** @return the participants with the podium first (1er, 2do and 3er Lugar), then the rest */
    public List<EventParticipant> listParticipants(int eventId) throws DataAccessException {
        return participantDAO.findByEvent(eventId).stream()
                .sorted(Comparator.comparing(EventParticipant::getPlacement,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /** @return the participations of every event of the year */
    public List<EventParticipant> listParticipations(int year) throws DataAccessException {
        return participantDAO.findBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    /** Participants by event type, in the order of the types. */
    public Map<EventType, Long> countByType(List<EventParticipant> participations) {
        return participations.stream().collect(Collectors.groupingBy(participation -> participation.getEvent().getType(),
                () -> new EnumMap<>(EventType.class), Collectors.counting()));
    }

    /** Events and podiums of each student, the most participative first. */
    public List<ParticipationSummary> participationByStudent(List<EventParticipant> participations) {
        Map<Integer, List<EventParticipant>> byStudent = participations.stream()
                .collect(Collectors.groupingBy(participation -> participation.getStudent().getId()));
        return byStudent.values().stream()
                .map(studentParticipations -> new ParticipationSummary(studentParticipations.get(0).getStudent(),
                        studentParticipations.size(),
                        studentParticipations.stream().filter(EventController::isPodium).count()))
                .sorted(Comparator.comparingLong(ParticipationSummary::events).reversed()
                        .thenComparing(Comparator.comparingLong(ParticipationSummary::podiums).reversed())
                        .thenComparing(summary -> summary.student().getFullName()))
                .toList();
    }

    private static boolean isPodium(EventParticipant participation) {
        Placement placement = participation.getPlacement();
        return placement != null && placement != Placement.PARTICIPANT;
    }

    public OperationResult register(SchoolEvent event) throws DataAccessException {
        event.setActive(true);
        String rejection = validate(event);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        event.setId(eventDAO.create(event));
        return OperationResult.ok("Evento registrado: " + event.getDisplayName() + ".");
    }

    public OperationResult update(SchoolEvent event) throws DataAccessException {
        String rejection = validate(event);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        return eventDAO.update(event)
                ? OperationResult.ok("Evento actualizado: " + event.getDisplayName() + ".")
                : OperationResult.error("No existe el evento.");
    }

    /** An event with participants is not deleted: it is marked as not active (cancelled). */
    public OperationResult delete(int eventId) throws DataAccessException {
        Optional<SchoolEvent> event = eventDAO.findById(eventId);
        if (event.isEmpty()) {
            return OperationResult.error("No existe el evento.");
        }
        if (event.get().getParticipants() > 0) {
            return OperationResult.error("No se puede eliminar " + event.get().getName() + ": tiene "
                    + event.get().getParticipants() + " participante(s). Márquelo como no activo con la opción Actualizar.");
        }
        return eventDAO.delete(eventId)
                ? OperationResult.ok("Evento eliminado: " + event.get().getDisplayName() + ".")
                : OperationResult.error("No existe el evento.");
    }

    public OperationResult addParticipant(int eventId, int studentId, ParticipationRole role)
            throws DataAccessException {
        Optional<SchoolEvent> event = eventDAO.findById(eventId);
        Optional<Student> student = studentDAO.findById(studentId);
        if (event.isEmpty() || student.isEmpty()) {
            return OperationResult.error("El evento o el estudiante no existe.");
        }
        if (!event.get().isActive()) {
            return OperationResult.error("El evento " + event.get().getName() + " no está activo.");
        }
        String name = student.get().getFullName();
        if (student.get().getStatus() != StudentStatus.ACTIVE) {
            return OperationResult.error(name + " no está Activo.");
        }
        boolean alreadyIn = participantDAO.findByEvent(eventId).stream()
                .anyMatch(participant -> participant.getStudent().getId() == studentId);
        if (alreadyIn) {
            return OperationResult.error(name + " ya está registrado en " + event.get().getName() + ".");
        }
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event.get());
        participant.setStudent(student.get());
        participant.setRole(role == null ? ParticipationRole.PARTICIPANT : role);
        participant.setId(participantDAO.create(participant));
        return OperationResult.ok(name + " quedó registrado en " + event.get().getDisplayName() + " como "
                + participant.getRole().getDatabaseValue() + ".");
    }

    /** Only the events with competition record positions. */
    public OperationResult setPlacement(int participantId, Placement placement) throws DataAccessException {
        Optional<EventParticipant> participant = participantDAO.findById(participantId);
        if (participant.isEmpty() || placement == null) {
            return OperationResult.error("El participante o la posición no existe.");
        }
        if (!participant.get().getEvent().hasCompetition()) {
            return OperationResult.error("El evento " + participant.get().getEvent().getName()
                    + " no tiene competencia; no se registran posiciones.");
        }
        participantDAO.updatePlacement(participantId, placement);
        return OperationResult.ok(participant.get().getStudent().getFullName() + ": " + placement.getDatabaseValue()
                + " en " + participant.get().getEvent().getName() + ".");
    }

    public OperationResult removeParticipant(int participantId) throws DataAccessException {
        Optional<EventParticipant> participant = participantDAO.findById(participantId);
        if (participant.isEmpty()) {
            return OperationResult.error("No existe el participante.");
        }
        participantDAO.delete(participantId);
        return OperationResult.ok(participant.get().getStudent().getFullName() + " ya no figura en "
                + participant.get().getEvent().getName() + ".");
    }

    private String validate(SchoolEvent event) throws DataAccessException {
        String name = event.getName();
        if (name == null || name.isBlank() || name.length() > SchoolEvent.NAME_MAX_LENGTH) {
            return "El nombre es obligatorio (máximo " + SchoolEvent.NAME_MAX_LENGTH + " caracteres).";
        }
        String description = event.getDescription();
        if (description != null && description.length() > SchoolEvent.DESCRIPTION_MAX_LENGTH) {
            return "La descripción admite máximo " + SchoolEvent.DESCRIPTION_MAX_LENGTH + " caracteres.";
        }
        if (event.getType() == null) {
            return "El tipo de evento es obligatorio.";
        }
        if (event.getDate() == null || !AcademicYear.isValid(event.getDate().getYear())) {
            return "La fecha es obligatoria y su año debe estar entre " + AcademicYear.FIRST + " y "
                    + AcademicYear.last() + ".";
        }
        String place = event.getPlace();
        if (place != null && place.length() > SchoolEvent.PLACE_MAX_LENGTH) {
            return "El lugar admite máximo " + SchoolEvent.PLACE_MAX_LENGTH + " caracteres.";
        }
        if (event.getOrganizer() != null && teacherDAO.findById(event.getOrganizer().getId()).isEmpty()) {
            return "El profesor organizador no existe.";
        }
        return null;
    }
}
