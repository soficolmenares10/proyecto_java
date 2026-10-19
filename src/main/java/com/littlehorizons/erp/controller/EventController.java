package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.ParticipationDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.SchoolEvent;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for events: calendar, participants and positions in competitions.
 */
public class EventController {

    private static final int LAST_RANK = 99;

    private final EventDao eventDao;
    private final ParticipationDao participationDao;
    private final ProfessorDao professorDao;
    private final StudentDao studentDao;

    public EventController(EventDao eventDao, ParticipationDao participationDao, ProfessorDao professorDao,
                           StudentDao studentDao) {
        this.eventDao = eventDao;
        this.participationDao = participationDao;
        this.professorDao = professorDao;
        this.studentDao = studentDao;
    }

    // ------------------------------------------------------------------ events

    /** @return the new event id */
    public int register(SchoolEvent event) {
        validate(event);
        event.setActive(true);
        return eventDao.insert(event);
    }

    public void update(SchoolEvent event) {
        getById(event.getId());
        validate(event);
        eventDao.update(event);
    }

    public void setActive(int eventId, boolean active) {
        SchoolEvent event = getById(eventId);
        if (event.isActive() == active) {
            throw new BusinessException("El evento ya está " + (active ? "activo" : "inactivo") + ".");
        }
        eventDao.setActive(eventId, active);
    }

    public SchoolEvent getById(int eventId) {
        return eventDao.findById(eventId).orElseThrow(() -> new BusinessException("El evento no existe."));
    }

    public List<SchoolEvent> listByYear(int year) {
        checkYear(year);
        return eventDao.findByYear(year);
    }

    // ------------------------------------------------------------ participants

    /**
     * Registers a student as participant of an event that already took place (or takes place today).
     * In events with competition, a "Competidor" gets a position (by default "Participante");
     * 1er, 2do and 3er lugar can be given to only one competitor each.
     */
    public void addParticipant(int eventId, int studentId, String role, String position, String notes) {
        SchoolEvent event = getById(eventId);
        if (!event.isActive()) {
            throw new BusinessException("El evento está inactivo.");
        }
        if (event.getDate().isAfter(LocalDate.now())) {
            throw new BusinessException("El evento será el " + event.getDate()
                    + ". Los participantes se registran cuando el evento ya ocurrió.");
        }
        if (studentDao.findById(studentId).isEmpty()) {
            throw new BusinessException("El estudiante no existe.");
        }
        if (participationDao.find(eventId, studentId).isPresent()) {
            throw new BusinessException("El estudiante ya está registrado en este evento.");
        }
        Rules.requireOneOf(role, event.isCompetition()
                ? EventParticipation.ROLES : EventParticipation.ROLES_WITHOUT_COMPETITION, "El rol");
        Rules.limitText(notes, "Las observaciones", 1000);

        String finalPosition = null;
        if (EventParticipation.COMPETITOR.equals(role)) {
            finalPosition = position == null ? EventParticipation.PARTICIPANT : position;
            checkPosition(eventId, finalPosition);
        }
        participationDao.insert(new EventParticipation(0, eventId, null, null, studentId, null, null,
                role, finalPosition, notes));
    }

    /** Sets (or changes) the position of a competitor of a competition event. */
    public void setPosition(int eventId, int studentId, String position) {
        SchoolEvent event = getById(eventId);
        if (!event.isCompetition()) {
            throw new BusinessException("Ese evento no tiene competencia, no se asignan posiciones.");
        }
        EventParticipation participation = participationDao.find(eventId, studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no está registrado en este evento."));
        if (!EventParticipation.COMPETITOR.equals(participation.role())) {
            throw new BusinessException("Solo los competidores reciben posición. Rol actual: "
                    + participation.role() + ".");
        }
        if (position.equals(participation.position())) {
            throw new BusinessException("El estudiante ya tiene la posición " + position + ".");
        }
        checkPosition(eventId, position);
        participationDao.updatePosition(participation.id(), participation.role(), position);
    }

    public void removeParticipant(int eventId, int studentId) {
        EventParticipation participation = participationDao.find(eventId, studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no está registrado en este evento."));
        participationDao.delete(participation.id());
    }

    /** Participants of an event: the podium first (1er, 2do, 3er lugar), then the rest by name. */
    public List<EventParticipation> participants(int eventId) {
        getById(eventId);
        return participationDao.findByEvent(eventId).stream()
                .sorted(Comparator.comparingInt(this::rank).thenComparing(EventParticipation::studentName))
                .collect(Collectors.toList());
    }

    public List<EventParticipation> eventsOfStudent(int studentId) {
        return participationDao.findByStudent(studentId);
    }

    // ----------------------------------------------------------------- helpers

    private int rank(EventParticipation participation) {
        if (participation.position() == null) {
            return LAST_RANK;
        }
        int index = EventParticipation.POSITIONS.indexOf(participation.position());
        return index < 0 ? LAST_RANK : index;
    }

    private void checkPosition(int eventId, String position) {
        Rules.requireOneOf(position, EventParticipation.POSITIONS, "La posición");
        if (EventParticipation.PODIUM.contains(position) && participationDao.isPositionTaken(eventId, position)) {
            throw new BusinessException("La posición " + position + " ya fue asignada a otro competidor.");
        }
    }

    private void validate(SchoolEvent event) {
        Rules.requireText(event.getName(), "El nombre del evento", 150);
        Rules.limitText(event.getDescription(), "La descripción", 1000);
        Rules.requireOneOf(event.getType(), SchoolEvent.TYPES, "El tipo de evento");
        if (event.getDate() == null) {
            throw new BusinessException("La fecha del evento es obligatoria.");
        }
        checkYear(event.getDate().getYear());
        Rules.limitText(event.getPlace(), "El lugar", 150);
        if (event.getOrganizerId() != null && professorDao.findById(event.getOrganizerId()).isEmpty()) {
            throw new BusinessException("El profesor organizador no existe.");
        }
    }

    private void checkYear(int year) {
        int maxYear = LocalDate.now().getYear() + 5;
        if (year < 2000 || year > maxYear) {
            throw new BusinessException("El año debe estar entre 2000 y " + maxYear + ".");
        }
    }
}
