package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;
import java.util.List;
import java.util.Optional;

/** Data access operations for school events. */
public interface EventDao {

    /** @return the generated event id */
    int insert(SchoolEvent event);

    Optional<SchoolEvent> findById(int id);

    /** The events of one calendar year, ordered by date. */
    List<SchoolEvent> findByYear(int year);

    /** Updates name, description, type, date, place and organizer (the competition flag never changes). */
    boolean update(SchoolEvent event);

    boolean setActive(int eventId, boolean active);
}
