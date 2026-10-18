package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import java.util.List;
import java.util.Optional;

/**
 * Data access operations for guardians and for the student-guardian link.
 */
public interface GuardianDao {

    /** @return the generated guardian id */
    int insert(Guardian guardian);

    Optional<Guardian> findById(int id);

    Optional<Guardian> findByDocument(String documentNumber);

    List<Guardian> findAll();

    /** Guardians of one student; primaryForStudent is filled in each one. */
    List<Guardian> findByStudentId(int studentId);

    boolean update(Guardian guardian);

    /** @param excludeId guardian id to ignore (use 0 when creating a new guardian) */
    boolean existsByDocument(String documentNumber, int excludeId);

    /** Links a guardian to a student. If primary is true, the previous primary contact is demoted. */
    void linkToStudent(int studentId, int guardianId, boolean primary, boolean emergencyContact);

    /**
     * Removes the link. A student must keep at least one guardian.
     *
     * @return false if the link was not removed because it is the last guardian (or does not exist)
     */
    boolean unlink(int studentId, int guardianId);
}
