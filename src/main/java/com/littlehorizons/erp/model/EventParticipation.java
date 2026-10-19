package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

/**
 * A student that took part in an event. Maps the table "participantes_evento".
 * In competitions, "position" holds the place obtained.
 */
public record EventParticipation(int id, int eventId, String eventName, LocalDate eventDate,
                                 int studentId, String studentDocument, String studentName,
                                 String role, String position, String notes) {

    public static final String COMPETITOR = "Competidor";
    public static final List<String> ROLES = List.of("Participante", COMPETITOR, "Organizador", "Asistente");
    /** Roles for events without competition. */
    public static final List<String> ROLES_WITHOUT_COMPETITION = List.of("Participante", "Organizador", "Asistente");

    public static final String FIRST = "1er lugar";
    public static final String SECOND = "2do lugar";
    public static final String THIRD = "3er lugar";
    public static final String PARTICIPANT = "Participante";
    public static final List<String> POSITIONS = List.of(FIRST, SECOND, THIRD, PARTICIPANT);
    /** The three positions that can be given to only one competitor per event. */
    public static final List<String> PODIUM = List.of(FIRST, SECOND, THIRD);
}
