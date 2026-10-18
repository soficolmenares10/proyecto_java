package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import java.util.List;
import java.util.Optional;

/**
 * Data access operations for students (DAO pattern).
 * Controllers depend on this interface, not on the JDBC class.
 */
public interface StudentDao {

    /**
     * Saves a new student and links it to its first guardian (as primary contact)
     * in one transaction.
     *
     * @return the generated student id
     */
    int insert(Student student, int primaryGuardianId);

    Optional<Student> findById(int id);

    Optional<Student> findByDocument(String documentNumber);

    List<Student> findAll();

    /** Finds students whose document or full name contains the text. */
    List<Student> search(String text);

    List<Student> findByGuardianId(int guardianId);

    boolean update(Student student);

    boolean updateStatus(int studentId, String status);

    /** @param excludeId student id to ignore (use 0 when creating a new student) */
    boolean existsByDocument(String documentNumber, int excludeId);
}
