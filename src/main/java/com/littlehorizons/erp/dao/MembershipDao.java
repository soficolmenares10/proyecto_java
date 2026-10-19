package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClubMembership;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access operations for club memberships. */
public interface MembershipDao {

    /**
     * Adds a student to a club only if the club still has room. The check and the insert happen
     * in one transaction, locking the club row, so two people cannot take the last spot at once.
     *
     * @return the generated membership id, or -1 if the club is full
     */
    int enrollIfAvailable(int studentId, int clubId, LocalDate joinDate);

    /** The active membership of a student in a club, if any. */
    Optional<ClubMembership> findActive(int studentId, int clubId);

    List<ClubMembership> findByClub(int clubId, boolean activeOnly);

    /** All the memberships (active or withdrawn) of a student. */
    List<ClubMembership> findByStudent(int studentId);

    /** @return false if the membership was not active */
    boolean withdraw(int membershipId, LocalDate withdrawalDate, String reason);
}
