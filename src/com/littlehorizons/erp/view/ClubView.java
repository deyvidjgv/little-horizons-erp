package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ClubController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.MembershipStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Club catalog and memberships, inside Actividades Extracurriculares. */
public class ClubView implements MenuView {

    private static final int MAIN_MENU_OPTION = 9;
    private static final int NAME_COLUMN_WIDTH = 28;
    private static final int TEACHER_COLUMN_WIDTH = 22;
    private static final int SCHEDULE_COLUMN_WIDTH = 18;
    private static final int CAPACITY_MAX_LENGTH = 3;

    private final InputReader input;
    private final ClubController controller;
    private final TeacherSelector teacherSelector;
    private final StudentSelector studentSelector;

    public ClubView(InputReader input, ClubController controller, TeacherController teacherController,
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
            Screen.title("Clubes");
            System.out.println("1. Registrar club");
            System.out.println("2. Listar clubes y cupos");
            System.out.println("3. Consultar club y sus miembros");
            System.out.println("4. Actualizar club");
            System.out.println("5. Eliminar club");
            System.out.println("6. Inscribir estudiante en un club");
            System.out.println("7. Retirar estudiante de un club");
            System.out.println("8. Ver clubes de un estudiante");
            System.out.println(MAIN_MENU_OPTION + ". Volver al menú principal");
            System.out.println("0. Volver a Actividades Extracurriculares");
            option = input.readInt("Seleccione una opción: ", 0, MAIN_MENU_OPTION);
            try {
                switch (option) {
                    case 1 -> register();
                    case 2 -> list();
                    case 3 -> showClub();
                    case 4 -> update();
                    case 5 -> delete();
                    case 6 -> join();
                    case 7 -> withdraw();
                    case 8 -> showStudentClubs();
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

    private void register() throws DataAccessException {
        Screen.subtitle("Registrar club");
        Club club = new Club();
        readClubData(club, null);
        System.out.println("Profesor encargado:");
        Optional<Teacher> supervisor = teacherSelector.ask();
        if (supervisor.isEmpty()) {
            Screen.error("Registro cancelado: el club necesita un profesor encargado.");
            return;
        }
        club.setSupervisor(supervisor.get());
        Screen.result(controller.register(club));
    }

    private void list() throws DataAccessException {
        List<Club> clubs = controller.list();
        if (clubs.isEmpty()) {
            System.out.println("No hay clubes registrados.");
            return;
        }
        String format = "%-" + NAME_COLUMN_WIDTH + "s %-" + TEACHER_COLUMN_WIDTH + "s %-8s %-"
                + SCHEDULE_COLUMN_WIDTH + "s %s%n";
        System.out.printf(format, "Club", "Encargado", "Cupo", "Horario", "Estado");
        clubs.forEach(club -> System.out.printf(format, Screen.fit(club.getName(), NAME_COLUMN_WIDTH),
                Screen.fit(supervisorName(club), TEACHER_COLUMN_WIDTH),
                club.getActiveMembers() + "/" + club.getCapacity(),
                Screen.fit(Formats.text(club.getMeetingSchedule()), SCHEDULE_COLUMN_WIDTH), clubStatus(club)));
        System.out.println("Total: " + clubs.size() + " club(es).");
    }

    private void showClub() throws DataAccessException {
        Optional<Club> found = chooseClub(controller.list());
        if (found.isEmpty()) {
            return;
        }
        Club club = found.get();
        Screen.subtitle(club.getName());
        System.out.println("Descripción: " + Formats.text(club.getDescription()));
        System.out.println("Profesor encargado: " + supervisorName(club));
        System.out.println("Horario de reunión: " + Formats.text(club.getMeetingSchedule()));
        System.out.println("Cupo: " + club.getActiveMembers() + " de " + club.getCapacity());
        System.out.println("Estado: " + clubStatus(club));
        System.out.println();
        System.out.println("Miembros:");
        List<ClubMembership> members = controller.listMembers(club.getId());
        if (members.isEmpty()) {
            System.out.println("  Ninguno.");
        }
        members.forEach(member -> System.out.println("  - " + member.getStudent().getFullName() + ", desde "
                + Formats.date(member.getJoinDate()) + " - " + membershipDetail(member)));
    }

    private void update() throws DataAccessException {
        Optional<Club> found = chooseClub(controller.list());
        if (found.isEmpty()) {
            return;
        }
        Club current = found.get();
        Screen.subtitle("Actualizar " + current.getName());
        PersonForm.printEditHint();
        Club edited = new Club();
        edited.setId(current.getId());
        readClubData(edited, current);
        edited.setSupervisor(current.getSupervisor());
        System.out.println("Profesor encargado actual: " + supervisorName(current));
        if (current.getSupervisor() == null || input.readYesNo("¿Cambiar el profesor encargado?", false)) {
            Optional<Teacher> supervisor = teacherSelector.ask();
            if (supervisor.isEmpty()) {
                return;
            }
            edited.setSupervisor(supervisor.get());
        }
        edited.setActive(input.readYesNo("¿Está activo?", current.isActive()));
        Screen.result(controller.update(edited));
    }

    private void delete() throws DataAccessException {
        Optional<Club> found = chooseClub(controller.list());
        if (found.isEmpty()) {
            return;
        }
        if (input.readYesNo("¿Seguro que desea eliminar " + found.get().getName() + "?")) {
            Screen.result(controller.delete(found.get().getId()));
        } else {
            System.out.println("Eliminación cancelada.");
        }
    }

    private void join() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        Optional<Club> club = chooseClub(controller.listActive());
        if (club.isEmpty()) {
            return;
        }
        LocalDate joinDate = input.readDate("Fecha de ingreso al club", true, null);
        Screen.result(controller.join(student.get().getId(), club.get().getId(), joinDate));
    }

    private void withdraw() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<ClubMembership> active = controller.listStudentClubs(student.get().getId()).stream()
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .toList();
        if (active.isEmpty()) {
            Screen.error(student.get().getFullName() + " no es miembro activo de ningún club.");
            return;
        }
        ClubMembership chosen = input.readChoice("Club", active, membership -> membership.getClub().getName(), null);
        LocalDate date = input.readDate("Fecha de retiro", true, null);
        String reason = input.readText("Motivo del retiro", ClubMembership.REASON_MAX_LENGTH);
        if (input.readYesNo("¿Retirar a " + student.get().getFullName() + " de " + chosen.getClub().getName() + "?")) {
            Screen.result(controller.withdraw(chosen.getId(), date, reason));
        } else {
            System.out.println("Operación cancelada.");
        }
    }

    private void showStudentClubs() throws DataAccessException {
        Optional<Student> student = studentSelector.ask();
        if (student.isEmpty()) {
            return;
        }
        List<ClubMembership> memberships = controller.listStudentClubs(student.get().getId());
        System.out.println();
        System.out.println("Clubes de " + student.get().getFullName() + ":");
        if (memberships.isEmpty()) {
            System.out.println("  Ninguno.");
        }
        memberships.forEach(membership -> System.out.println("  - " + membership.getClub().getName() + ", desde "
                + Formats.date(membership.getJoinDate()) + " - " + membershipDetail(membership)));
    }

    /** Shows the clubs with their capacity, so the user sees which ones are full before choosing. */
    private Optional<Club> chooseClub(List<Club> clubs) {
        if (clubs.isEmpty()) {
            Screen.error("No hay clubes para elegir.");
            return Optional.empty();
        }
        return Optional.of(input.readChoice("Club", clubs,
                club -> club.getName() + " (" + club.getActiveMembers() + "/" + club.getCapacity() + ")", null));
    }

    /** @param current club being edited, or null when it is new */
    private void readClubData(Club club, Club current) {
        club.setName(input.readText("Nombre del club", Club.NAME_MAX_LENGTH, true, null, null,
                current == null ? null : current.getName()));
        club.setDescription(input.readText("Descripción (opcional)", Club.DESCRIPTION_MAX_LENGTH, false, null, null,
                current == null ? null : current.getDescription()));
        String capacityRule = "Escriba un número entre " + ClubController.MIN_CAPACITY + " y "
                + ClubController.MAX_CAPACITY + ".";
        String capacity = input.readText("Cupo máximo de estudiantes", CAPACITY_MAX_LENGTH, true,
                ClubView::isValidCapacity, capacityRule, current == null ? null : String.valueOf(current.getCapacity()));
        club.setCapacity(Integer.parseInt(capacity));
        club.setMeetingSchedule(input.readText("Días y horario de reunión (opcional, ej: Martes 3-5 PM)",
                Club.SCHEDULE_MAX_LENGTH, false, null, null, current == null ? null : current.getMeetingSchedule()));
    }

    private static boolean isValidCapacity(String value) {
        if (!value.matches("\\d+")) {
            return false;
        }
        int capacity = Integer.parseInt(value);
        return capacity >= ClubController.MIN_CAPACITY && capacity <= ClubController.MAX_CAPACITY;
    }

    private static String supervisorName(Club club) {
        return club.getSupervisor() == null ? "-" : club.getSupervisor().getFullName();
    }

    private static String clubStatus(Club club) {
        if (!club.isActive()) {
            return "Inactivo";
        }
        return club.isFull() ? "Activo (lleno)" : "Activo";
    }

    private static String membershipDetail(ClubMembership membership) {
        if (membership.getStatus() == MembershipStatus.ACTIVE) {
            return "Activa";
        }
        return "Retirada el " + Formats.date(membership.getWithdrawalDate()) + " ("
                + Formats.text(membership.getWithdrawalReason()) + ")";
    }
}
