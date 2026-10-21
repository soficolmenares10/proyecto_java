package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PendingCharge;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access operations for the charges billed to students. */
public interface ChargeDao {

    /** @return the generated charge id */
    int insert(PendingCharge charge);

    Optional<PendingCharge> findById(int id);

    /** The charges of a student that are still "Pendiente", oldest first. */
    List<PendingCharge> findPendingByStudent(int studentId);

    /** All the pending charges (of all students) that were due before the given date. */
    List<PendingCharge> findPendingDueBefore(LocalDate date);

    boolean exists(int studentId, int conceptId, int academicYear, String description);

    /** Deletes a charge only if it is still pending. */
    boolean deletePending(int chargeId);
}
