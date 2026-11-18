package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.EventController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.EventType;
import com.littlehorizons.erp.model.ParticipationRole;
import com.littlehorizons.erp.model.ParticipationSummary;
import com.littlehorizons.erp.model.Placement;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Event calendar and participants, inside Actividades Extracurriculares. */
public class EventView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int NAME_COLUMN_WIDTH = 34;

    private final InputReader input;
    private final EventController controller;
    private final TeacherSelector teacherSelector;
    private final StudentSelector studentSelector;

    public EventView(InputReader input, EventController controller, TeacherController teacherController,
                     StudentController studentController) {
        this.input = input;
        this.controller = controller;
        this.teacherSelector = new TeacherSelector(input, teacherController);
        this.studentSelector = new StudentSelector(input, studentController);
    }

    @Override
    public Navigation show() {
        int option;
        do {
            Screen.title("Eventos");
            System.out.println("1. Registrar evento");
            System.out.println("2. Calendario de eventos de un año");
            System.out.println("3. Consultar evento y participantes");
            System.out.println("4. Actualizar evento");
            System.out.println("5. Eliminar evento");
            System.out.println("6. Registrar participante en un evento");
            System.out.println("7. Registrar posición de un participante");
            System.out.println("8. Quitar participante de un evento");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Actividades Extracurriculares");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> calendar();
                    case 3 -> showEvent();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> addParticipant();
                    case 7 -> setPlacement();
                    case 8 -> removeParticipant();
                    case MAIN_MENU_OPTION -> {
                        return Navigation.MAIN_MENU;
                    }
                    default -> {
                    }
                }
            } catch (DataAccessException e) {
                Screen.error(e.getMessage());
            }
        } while (option != 0);
        return Navigation.BACK;
    }

    /** Participation of the students in the events of a year. Used by Reportes y Estadísticas. */
    void participationReport() throws DataAccessException {
        int year = Prompts.readAcademicYear(input);
        List<EventParticipant> participations = controller.listParticipations(year);
        Screen.subtitle("Participación en eventos " + year);
        if (participations.isEmpty()) {
            System.out.println("Sin participaciones registradas.");
            return;
        }
        System.out.println("Participaciones por tipo de evento:");
        controller.countByType(participations).forEach((type, count) ->
                System.out.println("  " + type.getDatabaseValue() + ": " + count));
        List<ParticipationSummary> byStudent = controller.participationByStudent(participations);
        System.out.println();
        String format = "  %-22s %-" + NAME_COLUMN_WIDTH + "s %-8s %s%n";
        System.out.printf(format, "Documento", "Estudiante", "Eventos", "Podios");
        byStudent.forEach(summary -> System.out.printf(format, summary.student().getDocument(),
                Screen.fit(summary.student().getFullName(), NAME_COLUMN_WIDTH), summary.events(), summary.podiums()));
        System.out.println("Total: " + participations.size() + " participaciones de " + byStudent.size()
                + " estudiante(s).");
    }

    private void register() throws DataAccessException {
        Screen.subtitle("Registrar evento");
        SchoolEvent event = new SchoolEvent();
        readEventData(event, null);
        if (input.readYesNo("¿Tiene un profesor organizador?")) {
            Optional<Teacher> organizer = teacherSelector.ask();
            if (organizer.isEmpty()) {
                return;
            }
            event.setOrganizer(organizer.get());
        }
        Screen.result(controller.register(event));
    }

    private void calendar() throws DataAccessException {
        int year = Prompts.readAcademicYear(input);
        List<SchoolEvent> events = controller.listByYear(year);
        if (events.isEmpty()) {
            System.out.println("No hay eventos registrados en " + year + ".");
            return;
        }
        Screen.subtitle("Calendario de eventos " + year);
        String format = "%-11s %-" + NAME_COLUMN_WIDTH + "s %-11s %-12s %-14s %s%n";
        System.out.printf(format, "Fecha", "Evento", "Tipo", "Competencia", "Participantes", "Estado");
        events.forEach(event -> System.out.printf(format, Formats.date(event.getDate()),
                Screen.fit(event.getName(), NAME_COLUMN_WIDTH), event.getType().getDatabaseValue(),
                event.hasCompetition() ? "Sí" : "No", event.getParticipants(), eventStatus(event)));
        System.out.println("Total: " + events.size() + " evento(s).");
    }

    private void showEvent() throws DataAccessException {
        Optional<SchoolEvent> found = chooseEvent();
        if (found.isEmpty()) {
            return;
        }
        SchoolEvent event = found.get();
        Screen.subtitle(event.getDisplayName());
        System.out.println("Tipo: " + event.getType().getDatabaseValue());
        System.out.println("Lugar: " + Formats.text(event.getPlace()));
        System.out.println("Descripción: " + Formats.text(event.getDescription()));
        System.out.println("Competencia: " + (event.hasCompetition() ? "Sí" : "No"));
        System.out.println("Organizador: " + (event.getOrganizer() == null ? "-" : event.getOrganizer().getFullName()));
        System.out.println("Estado: " + eventStatus(event));
        System.out.println();
        System.out.println("Participantes:");
        List<EventParticipant> participants = controller.listParticipants(event.getId());
        if (participants.isEmpty()) {
            System.out.println("  Ninguno.");
        }
        participants.forEach(participant -> System.out.println("  - " + participant.getStudent().getFullName()
                + " (" + participant.getRole().getDatabaseValue() + ")"
                + (participant.getPlacement() == null ? "" : ": " + participant.getPlacement().getDatabaseValue())));
    }

    private void update() throws DataAccessException {
        Optional<SchoolEvent> found = chooseEvent();
        if (found.isEmpty()) {
            return;
        }
        SchoolEvent current = found.get();
        Screen.subtitle("Actualizar " + current.getDisplayName());
        PersonForm.printEditHint();
        SchoolEvent edited = new SchoolEvent();
        edited.setId(current.getId());
        readEventData(edited, current);
        edited.setOrganizer(current.getOrganizer());
        System.out.println("Organizador actual: "
                + (current.getOrganizer() == null ? "ninguno" : current.getOrganizer().getFullName()));
        if (input.readYesNo("¿Cambiar el organizador?", false)) {
            edited.setOrganizer(null);
            if (input.readYesNo("¿Tiene un profesor organizador?")) {
                Optional<Teacher> organizer = teacherSelector.ask();
                if (organizer.isEmpty()) {
                    return;
                }
                edited.setOrganizer(organizer.get());
            }
        }
        edited.setActive(input.readYesNo("¿Está activo? (n = cancelado)", current.isActive()));
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<SchoolEvent> found = chooseEvent();
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar " + found.get().getDisplayName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    private void addParticipant() throws DataAccessException {
        Optional<SchoolEvent> event = chooseEvent();
        if (event.isEmpty()) {
            return;
        }
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        ParticipationRole suggested = event.get().hasCompetition()
                ? ParticipationRole.COMPETITOR : ParticipationRole.PARTICIPANT;
        ParticipationRole role = input.readChoice("Rol en el evento", List.of(ParticipationRole.values()),
                ParticipationRole::getDatabaseValue, suggested);
        Screen.result(controller.addParticipant(event.get().getId(), student.get().getId(), role));
    }

    private void setPlacement() throws DataAccessException {
        Optional<SchoolEvent> event = chooseEvent();
        if (event.isEmpty()) {
            return;
        }
        if (!event.get().hasCompetition()) {
            Screen.error(event.get().getName() + " no tiene competencia; no se registran posiciones.");
            return;
        }
        Optional<EventParticipant> participant = chooseParticipant(event.get());
        if (participant.isEmpty()) {
            return;
        }
        Placement placement = input.readChoice("Posición obtenida", List.of(Placement.values()),
                Placement::getDatabaseValue, participant.get().getPlacement());
        Screen.result(controller.setPlacement(participant.get().getId(), placement));
    }

    private void removeParticipant() throws DataAccessException {
        Optional<SchoolEvent> event = chooseEvent();
        if (event.isEmpty()) {
            return;
        }
        Optional<EventParticipant> participant = chooseParticipant(event.get());
        if (participant.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Quitar a " + participant.get().getStudent().getFullName() + " del evento?")) {
            Screen.result(controller.removeParticipant(participant.get().getId()));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    /** Asks for the year and lets the user choose one of its events. */
    private Optional<SchoolEvent> chooseEvent() throws DataAccessException {
        int year = Prompts.readAcademicYear(input);
        List<SchoolEvent> events = controller.listByYear(year);
        if (events.isEmpty()) {
            Screen.error("No hay eventos registrados en " + year + ".");
            return Optional.empty();
        }
        return Optional.of(input.readChoice("Evento", events, SchoolEvent::getDisplayName, null));
    }

    private Optional<EventParticipant> chooseParticipant(SchoolEvent event) throws DataAccessException {
        List<EventParticipant> participants = controller.listParticipants(event.getId());
        if (participants.isEmpty()) {
            Screen.error(event.getName() + " no tiene participantes registrados.");
            return Optional.empty();
        }
        return Optional.of(input.readChoice("Participante", participants,
                participant -> participant.getStudent().getFullName(), null));
    }

    /** @param current event being edited, or null when it is new */
    private void readEventData(SchoolEvent event, SchoolEvent current) {
        event.setName(input.readText("Nombre del evento", SchoolEvent.NAME_MAX_LENGTH, true, null, null,
                current == null ? null : current.getName()));
        event.setDescription(input.readText("Descripción (opcional)", SchoolEvent.DESCRIPTION_MAX_LENGTH, false,
                null, null, current == null ? null : current.getDescription()));
        event.setType(input.readChoice("Tipo de evento", List.of(EventType.values()), EventType::getDatabaseValue,
                current == null ? null : current.getType()));
        event.setDate(input.readDate("Fecha del evento", true, current == null ? null : current.getDate()));
        event.setPlace(input.readText("Lugar (opcional)", SchoolEvent.PLACE_MAX_LENGTH, false, null, null,
                current == null ? null : current.getPlace()));
        event.setHasCompetition(input.readYesNo("¿Tiene competencia con posiciones?",
                current == null ? null : current.hasCompetition()));
    }

    private static String eventStatus(SchoolEvent event) {
        if (!event.isActive()) {
            return "Cancelado";
        }
        return event.getDate().isBefore(LocalDate.now()) ? "Realizado" : "Próximo";
    }
}
