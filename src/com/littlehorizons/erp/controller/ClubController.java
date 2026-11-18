package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDAO;
import com.littlehorizons.erp.dao.DataAccessException;
import com.littlehorizons.erp.dao.MembershipDAO;
import com.littlehorizons.erp.dao.StudentDAO;
import com.littlehorizons.erp.dao.TeacherDAO;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.MembershipStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherStatus;
import com.littlehorizons.erp.util.Formats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Club catalog and memberships: a student can be in several clubs, a club never goes over its
 * maximum capacity, and each membership keeps its join date and its status (Activa or Retirada).
 */
public class ClubController {

    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 200;

    private final ClubDAO clubDAO;
    private final MembershipDAO membershipDAO;
    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;

    public ClubController(ClubDAO clubDAO, MembershipDAO membershipDAO, TeacherDAO teacherDAO, StudentDAO studentDAO) {
        this.clubDAO = clubDAO;
        this.membershipDAO = membershipDAO;
        this.teacherDAO = teacherDAO;
        this.studentDAO = studentDAO;
    }

    public List<Club> list() throws DataAccessException {
        return clubDAO.findAll();
    }

    public List<Club> listActive() throws DataAccessException {
        return clubDAO.findAll().stream()
                .filter(Club::isActive)
                .toList();
    }

    public List<ClubMembership> listMembers(int clubId) throws DataAccessException {
        return membershipDAO.findByClub(clubId);
    }

    public List<ClubMembership> listStudentClubs(int studentId) throws DataAccessException {
        return membershipDAO.findByStudent(studentId);
    }

    public OperationResult register(Club club) throws DataAccessException {
        club.setActive(true);
        String rejection = validate(club);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        rejection = validateSupervisor(club.getSupervisor().getId());
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        if (clubDAO.findByName(club.getName()).isPresent()) {
            return OperationResult.error("Ya existe un club llamado " + club.getName() + ".");
        }
        club.setId(clubDAO.create(club));
        return OperationResult.ok("Club registrado: " + club.getName() + " (cupo de " + club.getCapacity() + ").");
    }

    public OperationResult update(Club club) throws DataAccessException {
        String rejection = validate(club);
        if (rejection != null) {
            return OperationResult.error(rejection);
        }
        Optional<Club> current = clubDAO.findById(club.getId());
        if (current.isEmpty()) {
            return OperationResult.error("No existe el club.");
        }
        Teacher currentSupervisor = current.get().getSupervisor();
        boolean supervisorChanged = currentSupervisor == null
                || currentSupervisor.getId() != club.getSupervisor().getId();
        if (supervisorChanged) {
            rejection = validateSupervisor(club.getSupervisor().getId());
            if (rejection != null) {
                return OperationResult.error(rejection);
            }
        }
        Optional<Club> sameName = clubDAO.findByName(club.getName());
        if (sameName.isPresent() && sameName.get().getId() != club.getId()) {
            return OperationResult.error("Ya existe otro club llamado " + club.getName() + ".");
        }
        int activeMembers = current.get().getActiveMembers();
        if (club.getCapacity() < activeMembers) {
            return OperationResult.error("El cupo no puede ser menor que los " + activeMembers
                    + " miembros activos que tiene el club.");
        }
        return clubDAO.update(club)
                ? OperationResult.ok("Club actualizado: " + club.getName() + ".")
                : OperationResult.error("No existe el club.");
    }

    /** Only a club that never had members can be deleted; otherwise it is deactivated. */
    public OperationResult delete(int clubId) throws DataAccessException {
        Optional<Club> club = clubDAO.findById(clubId);
        if (club.isEmpty()) {
            return OperationResult.error("No existe el club.");
        }
        if (clubDAO.hasMemberships(clubId)) {
            return OperationResult.error("No se puede eliminar " + club.get().getName() + ": tiene historial de "
                    + "miembros. Desactívelo con la opción Actualizar.");
        }
        return clubDAO.delete(clubId)
                ? OperationResult.ok("Club eliminado: " + club.get().getName() + ".")
                : OperationResult.error("No existe el club.");
    }

    public OperationResult join(int studentId, int clubId, LocalDate joinDate) throws DataAccessException {
        Optional<Student> student = studentDAO.findById(studentId);
        Optional<Club> club = clubDAO.findById(clubId);
        if (student.isEmpty() || club.isEmpty()) {
            return OperationResult.error("El estudiante o el club no existe.");
        }
        String name = student.get().getFullName();
        if (student.get().getStatus() != StudentStatus.ACTIVE) {
            return OperationResult.error(name + " no está Activo; no se puede inscribir en clubes.");
        }
        if (!club.get().isActive()) {
            return OperationResult.error("El club " + club.get().getName() + " no está activo.");
        }
        if (joinDate == null || joinDate.isAfter(LocalDate.now())) {
            return OperationResult.error("La fecha de ingreso es obligatoria y no puede ser futura.");
        }
        if (membershipDAO.findActive(studentId, clubId).isPresent()) {
            return OperationResult.error(name + " ya es miembro activo de " + club.get().getName() + ".");
        }
        ClubMembership membership = new ClubMembership();
        membership.setStudent(student.get());
        membership.setClub(club.get());
        membership.setJoinDate(joinDate);
        membership.setStatus(MembershipStatus.ACTIVE);
        // The capacity is checked again inside the transaction, with the club row locked.
        if (!membershipDAO.joinIfThereIsRoom(membership)) {
            return OperationResult.error("El club " + club.get().getName() + " no tiene cupo ("
                    + club.get().getCapacity() + " de " + club.get().getCapacity() + ").");
        }
        return OperationResult.ok(name + " quedó inscrito en " + club.get().getName() + " desde el "
                + Formats.date(joinDate) + ".");
    }

    public OperationResult withdraw(int membershipId, LocalDate date, String reason) throws DataAccessException {
        Optional<ClubMembership> membership = membershipDAO.findById(membershipId);
        if (membership.isEmpty() || membership.get().getStatus() != MembershipStatus.ACTIVE) {
            return OperationResult.error("La membresía no existe o ya estaba retirada.");
        }
        LocalDate joinDate = membership.get().getJoinDate();
        if (date == null || date.isAfter(LocalDate.now()) || date.isBefore(joinDate)) {
            return OperationResult.error("La fecha de retiro no puede ser futura ni anterior al ingreso ("
                    + Formats.date(joinDate) + ").");
        }
        if (reason == null || reason.isBlank() || reason.length() > ClubMembership.REASON_MAX_LENGTH) {
            return OperationResult.error("El motivo del retiro es obligatorio (máximo "
                    + ClubMembership.REASON_MAX_LENGTH + " caracteres).");
        }
        membershipDAO.withdraw(membershipId, date, reason);
        return OperationResult.ok(membership.get().getStudent().getFullName() + " quedó retirado de "
                + membership.get().getClub().getName() + ".");
    }

    private String validateSupervisor(int teacherId) throws DataAccessException {
        Optional<Teacher> teacher = teacherDAO.findById(teacherId);
        if (teacher.isEmpty()) {
            return "El profesor encargado no existe.";
        }
        if (teacher.get().getStatus() != TeacherStatus.ACTIVE) {
            return teacher.get().getFullName() + " no está Activo; elija otro profesor encargado.";
        }
        return null;
    }

    private static String validate(Club club) {
        String name = club.getName();
        if (name == null || name.isBlank() || name.length() > Club.NAME_MAX_LENGTH) {
            return "El nombre es obligatorio (máximo " + Club.NAME_MAX_LENGTH + " caracteres).";
        }
        String description = club.getDescription();
        if (description != null && description.length() > Club.DESCRIPTION_MAX_LENGTH) {
            return "La descripción admite máximo " + Club.DESCRIPTION_MAX_LENGTH + " caracteres.";
        }
        if (club.getSupervisor() == null) {
            return "El profesor encargado es obligatorio.";
        }
        if (club.getCapacity() < MIN_CAPACITY || club.getCapacity() > MAX_CAPACITY) {
            return "El cupo máximo debe estar entre " + MIN_CAPACITY + " y " + MAX_CAPACITY + ".";
        }
        String schedule = club.getMeetingSchedule();
        if (schedule != null && schedule.length() > Club.SCHEDULE_MAX_LENGTH) {
            return "El horario de reunión admite máximo " + Club.SCHEDULE_MAX_LENGTH + " caracteres.";
        }
        return null;
    }
}
