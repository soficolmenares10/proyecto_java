package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.MembershipDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.Student;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic for clubs: catalog, enrollment of students (respecting the maximum capacity)
 * and follow-up of their participation.
 */
public class ClubController {

    private static final int MAX_CAPACITY = 200;

    private final ClubDao clubDao;
    private final MembershipDao membershipDao;
    private final ProfessorDao professorDao;
    private final StudentDao studentDao;

    public ClubController(ClubDao clubDao, MembershipDao membershipDao, ProfessorDao professorDao,
                          StudentDao studentDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.professorDao = professorDao;
        this.studentDao = studentDao;
    }

    /** @return the new club id */
    public int register(Club club) {
        validate(club, 0);
        club.setActive(true);
        return clubDao.insert(club);
    }

    public void update(Club club) {
        Club current = getById(club.getId());
        validate(club, club.getId());
        if (club.getCapacity() < current.getActiveMembers()) {
            throw new BusinessException("El cupo no puede ser menor que los miembros activos ("
                    + current.getActiveMembers() + ").");
        }
        clubDao.update(club);
    }

    public void setActive(int clubId, boolean active) {
        Club club = getById(clubId);
        if (club.isActive() == active) {
            throw new BusinessException("El club ya está " + (active ? "activo" : "inactivo") + ".");
        }
        clubDao.setActive(clubId, active);
    }

    public Club getById(int clubId) {
        return clubDao.findById(clubId).orElseThrow(() -> new BusinessException("El club no existe."));
    }

    public List<Club> listAll() {
        return clubDao.findAll();
    }

    /**
     * Enrolls an active student in an active club that still has room.
     * A student can belong to several clubs, but only once to each one.
     */
    public void enroll(int studentId, int clubId) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no existe."));
        if (!"Activo".equals(student.getStatus())) {
            throw new BusinessException("Solo los estudiantes activos pueden inscribirse en clubes. Estado actual: "
                    + student.getStatus() + ".");
        }
        Club club = getById(clubId);
        if (!club.isActive()) {
            throw new BusinessException("El club " + club.getName() + " está inactivo.");
        }
        if (membershipDao.findActive(studentId, clubId).isPresent()) {
            throw new BusinessException(student.getFullName() + " ya es miembro activo de " + club.getName() + ".");
        }
        int id = membershipDao.enrollIfAvailable(studentId, clubId, LocalDate.now());
        if (id == -1) {
            throw new BusinessException("El club " + club.getName() + " no tiene cupos disponibles ("
                    + club.getCapacity() + " de " + club.getCapacity() + ").");
        }
    }

    public void withdraw(int studentId, int clubId, String reason) {
        Rules.limitText(reason, "El motivo", 1000);
        ClubMembership membership = membershipDao.findActive(studentId, clubId)
                .orElseThrow(() -> new BusinessException("El estudiante no es miembro activo de ese club."));
        membershipDao.withdraw(membership.id(), LocalDate.now(), reason);
    }

    public List<ClubMembership> members(int clubId, boolean activeOnly) {
        getById(clubId);
        return membershipDao.findByClub(clubId, activeOnly);
    }

    public List<ClubMembership> clubsOfStudent(int studentId) {
        return membershipDao.findByStudent(studentId);
    }

    private void validate(Club club, int excludeId) {
        Rules.requireText(club.getName(), "El nombre del club", 100);
        club.setName(club.getName().trim());
        if (clubDao.existsByName(club.getName(), excludeId)) {
            throw new BusinessException("Ya existe un club llamado " + club.getName() + ".");
        }
        Rules.limitText(club.getDescription(), "La descripción", 1000);
        Rules.limitText(club.getMeetingSchedule(), "El horario de reunión", 100);
        if (club.getCapacity() < 1 || club.getCapacity() > MAX_CAPACITY) {
            throw new BusinessException("El cupo máximo debe estar entre 1 y " + MAX_CAPACITY + ".");
        }
        if (club.getSupervisorId() == null) {
            throw new BusinessException("El club necesita un profesor encargado.");
        }
        Professor supervisor = professorDao.findById(club.getSupervisorId())
                .orElseThrow(() -> new BusinessException("El profesor encargado no existe."));
        if (!"Activo".equals(supervisor.getStatus())) {
            throw new BusinessException("El profesor encargado debe estar activo. Estado actual: "
                    + supervisor.getStatus() + ".");
        }
    }
}
