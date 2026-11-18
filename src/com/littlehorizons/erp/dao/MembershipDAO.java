package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.DatabaseValue;
import com.littlehorizons.erp.model.MembershipStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Members of the clubs (membresias_club table). */
public class MembershipDAO extends BaseDAO {

    private static final String SELECT_MEMBERSHIPS = "SELECT mc.id_membresia, mc.fecha_ingreso, mc.estado_membresia, "
            + "mc.fecha_retiro, mc.motivo_retiro, c.id_club, c.nombre_club, " + StudentDAO.SUMMARY_COLUMNS
            + " FROM membresias_club mc JOIN clubes c ON c.id_club = mc.id_club "
            + "JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante";
    private static final String INSERT_MEMBERSHIP = "INSERT INTO membresias_club (id_estudiante, id_club, "
            + "fecha_ingreso, estado_membresia) VALUES (?, ?, ?, ?)";
    // FOR UPDATE locks the club row until the transaction ends, so two inscriptions at the same time
    // cannot both take the last place.
    private static final String LOCK_CAPACITY = "SELECT capacidad_maxima FROM clubes WHERE id_club = ? FOR UPDATE";
    private static final String COUNT_ACTIVE = "SELECT COUNT(*) FROM membresias_club "
            + "WHERE id_club = ? AND estado_membresia = ?";

    /**
     * Saves the membership only if the club still has a free place.
     *
     * @return false if the club is full
     */
    public boolean joinIfThereIsRoom(ClubMembership membership) throws DataAccessException {
        int clubId = membership.getClub().getId();
        return inTransaction(connection -> {
            int capacity = queryInt(connection, LOCK_CAPACITY, statement -> statement.setInt(1, clubId));
            int activeMembers = queryInt(connection, COUNT_ACTIVE, statement -> {
                statement.setInt(1, clubId);
                statement.setString(2, MembershipStatus.ACTIVE.getDatabaseValue());
            });
            if (activeMembers >= capacity) {
                return false;
            }
            update(connection, INSERT_MEMBERSHIP, statement -> {
                statement.setInt(1, membership.getStudent().getId());
                statement.setInt(2, clubId);
                statement.setObject(3, membership.getJoinDate());
                statement.setString(4, MembershipStatus.ACTIVE.getDatabaseValue());
            });
            return true;
        }, "No se pudo inscribir al estudiante en el club.");
    }

    public Optional<ClubMembership> findById(int id) throws DataAccessException {
        return queryOne(SELECT_MEMBERSHIPS + " WHERE mc.id_membresia = ?", statement -> statement.setInt(1, id),
                MembershipDAO::mapMembership, "No se pudo buscar la membresía.");
    }

    /** @return the members of the club, active first */
    public List<ClubMembership> findByClub(int clubId) throws DataAccessException {
        return queryList(SELECT_MEMBERSHIPS + " WHERE mc.id_club = ? "
                        + "ORDER BY mc.estado_membresia, e.primer_apellido, e.primer_nombre",
                statement -> statement.setInt(1, clubId), MembershipDAO::mapMembership,
                "No se pudo consultar los miembros del club.");
    }

    public List<ClubMembership> findByStudent(int studentId) throws DataAccessException {
        return queryList(SELECT_MEMBERSHIPS + " WHERE mc.id_estudiante = ? ORDER BY mc.fecha_ingreso DESC",
                statement -> statement.setInt(1, studentId), MembershipDAO::mapMembership,
                "No se pudo consultar los clubes del estudiante.");
    }

    public Optional<ClubMembership> findActive(int studentId, int clubId) throws DataAccessException {
        return queryOne(SELECT_MEMBERSHIPS + " WHERE mc.id_estudiante = ? AND mc.id_club = ? "
                + "AND mc.estado_membresia = ?", statement -> {
                    statement.setInt(1, studentId);
                    statement.setInt(2, clubId);
                    statement.setString(3, MembershipStatus.ACTIVE.getDatabaseValue());
                }, MembershipDAO::mapMembership, "No se pudo buscar la membresía.");
    }

    public boolean withdraw(int id, LocalDate date, String reason) throws DataAccessException {
        return executeUpdate("UPDATE membresias_club SET estado_membresia = ?, fecha_retiro = ?, motivo_retiro = ? "
                + "WHERE id_membresia = ?", statement -> {
                    statement.setString(1, MembershipStatus.WITHDRAWN.getDatabaseValue());
                    statement.setObject(2, date);
                    statement.setString(3, reason);
                    statement.setInt(4, id);
                }, "No se pudo registrar el retiro del club.") == 1;
    }

    private static ClubMembership mapMembership(ResultSet row) throws SQLException {
        Club club = new Club();
        club.setId(row.getInt("id_club"));
        club.setName(row.getString("nombre_club"));

        ClubMembership membership = new ClubMembership();
        membership.setId(row.getInt("id_membresia"));
        membership.setStudent(StudentDAO.mapStudentSummary(row));
        membership.setClub(club);
        membership.setJoinDate(row.getObject("fecha_ingreso", LocalDate.class));
        membership.setStatus(DatabaseValue.fromDatabaseValue(MembershipStatus.class,
                row.getString("estado_membresia")));
        membership.setWithdrawalDate(row.getObject("fecha_retiro", LocalDate.class));
        membership.setWithdrawalReason(row.getString("motivo_retiro"));
        return membership;
    }
}
