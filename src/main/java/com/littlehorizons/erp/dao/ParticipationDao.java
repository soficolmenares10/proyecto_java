package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventParticipation;
import java.util.List;
import java.util.Optional;

/** Data access operations for the participants of events. */
public interface ParticipationDao {

    /** @return the generated participation id */
    int insert(EventParticipation participation);

    Optional<EventParticipation> find(int eventId, int studentId);

    List<EventParticipation> findByEvent(int eventId);

    /** The events a student took part in, newest first. */
    List<EventParticipation> findByStudent(int studentId);

    /** True if some participant of the event already has that position. */
    boolean isPositionTaken(int eventId, String position);

    boolean updatePosition(int participationId, String role, String position);

    boolean delete(int participationId);
}
