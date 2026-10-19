package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import java.util.List;
import java.util.Optional;

/** Data access operations for clubs. */
public interface ClubDao {

    /** @return the generated club id */
    int insert(Club club);

    Optional<Club> findById(int id);

    /** All the clubs ordered by name; each one includes its number of active members. */
    List<Club> findAll();

    boolean update(Club club);

    boolean setActive(int clubId, boolean active);

    /** @param excludeId club id to ignore (0 when creating a new club) */
    boolean existsByName(String name, int excludeId);
}
