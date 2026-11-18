package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.MembershipStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

/** CRUD of the clubes table; each club comes with its teacher in charge and its active members. */
public class ClubDAO extends BaseDAO {

    private static final String SELECT_CLUBS = "SELECT c.id_club, c.nombre_club, c.descripcion, c.capacidad_maxima, "
            + "c.horario_reunion, c.esta_activo, " + TeacherDAO.SUMMARY_COLUMNS + ", "
            + "(SELECT COUNT(*) FROM membresias_club mc WHERE mc.id_club = c.id_club AND mc.estado_membresia = '"
            + MembershipStatus.ACTIVE.getDatabaseValue() + "') AS miembros_activos "
            + "FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor";
    private static final String INSERT_CLUB = "INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, "
            + "capacidad_maxima, horario_reunion, esta_activo) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_CLUB = "UPDATE clubes SET nombre_club = ?, descripcion = ?, "
            + "id_profesor_supervisor = ?, capacidad_maxima = ?, horario_reunion = ?, esta_activo = ? WHERE id_club = ?";

    /** @return the id generated for the club */
    public int create(Club club) throws DataAccessException {
        return executeInsert(INSERT_CLUB, statement -> fillData(statement, club), "No se pudo registrar el club.");
    }

    public Optional<Club> findById(int id) throws DataAccessException {
        return queryOne(SELECT_CLUBS + " WHERE c.id_club = ?", statement -> statement.setInt(1, id),
                ClubDAO::mapClub, "No se pudo buscar el club.");
    }

    public Optional<Club> findByName(String name) throws DataAccessException {
        return queryOne(SELECT_CLUBS + " WHERE c.nombre_club = ?", statement -> statement.setString(1, name),
                ClubDAO::mapClub, "No se pudo buscar el club.");
    }

    public List<Club> findAll() throws DataAccessException {
        return queryList(SELECT_CLUBS + " ORDER BY c.nombre_club", statement -> { }, ClubDAO::mapClub,
                "No se pudo consultar los clubes.");
    }

    public boolean update(Club club) throws DataAccessException {
        return executeUpdate(UPDATE_CLUB, statement -> {
            fillData(statement, club);
            statement.setInt(7, club.getId());
        }, "No se pudo actualizar el club.") == 1;
    }

    /** The memberships are deleted by the database (ON DELETE CASCADE). */
    public boolean delete(int id) throws DataAccessException {
        return executeUpdate("DELETE FROM clubes WHERE id_club = ?", statement -> statement.setInt(1, id),
                "No se pudo eliminar el club.") == 1;
    }

    /** @return true if the club has had members, active or not */
    public boolean hasMemberships(int id) throws DataAccessException {
        return queryOne("SELECT EXISTS (SELECT 1 FROM membresias_club WHERE id_club = ?)",
                statement -> statement.setInt(1, id), row -> row.getBoolean(1),
                "No se pudo revisar los miembros del club.").orElse(false);
    }

    /** Fills parameters 1 to 6, in the same order in the INSERT and in the UPDATE. */
    private static void fillData(PreparedStatement statement, Club club) throws SQLException {
        statement.setString(1, club.getName());
        statement.setString(2, club.getDescription());
        if (club.getSupervisor() == null) {
            statement.setNull(3, Types.INTEGER);
        } else {
            statement.setInt(3, club.getSupervisor().getId());
        }
        statement.setInt(4, club.getCapacity());
        statement.setString(5, club.getMeetingSchedule());
        statement.setBoolean(6, club.isActive());
    }

    private static Club mapClub(ResultSet row) throws SQLException {
        Club club = new Club();
        club.setId(row.getInt("id_club"));
        club.setName(row.getString("nombre_club"));
        club.setDescription(row.getString("descripcion"));
        club.setSupervisor(TeacherDAO.mapTeacherSummary(row));
        club.setCapacity(row.getInt("capacidad_maxima"));
        club.setMeetingSchedule(row.getString("horario_reunion"));
        club.setActive(row.getBoolean("esta_activo"));
        club.setActiveMembers(row.getInt("miembros_activos"));
        return club;
    }
}
