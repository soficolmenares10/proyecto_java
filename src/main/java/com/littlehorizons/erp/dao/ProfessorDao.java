package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Professor;
import java.util.List;
import java.util.Optional;

/** Data access operations for professors. */
public interface ProfessorDao {

    /** @return the generated professor id */
    int insert(Professor professor);

    Optional<Professor> findById(int id);

    Optional<Professor> findByDocument(String documentNumber);

    List<Professor> findAll();

    /** Finds professors whose document, name or specialization contains the text. */
    List<Professor> search(String text);

    boolean update(Professor professor);

    boolean updateStatus(int professorId, String status);

    /** @param excludeId professor id to ignore (0 when creating a new professor) */
    boolean existsByDocument(String documentNumber, int excludeId);

    boolean existsByEmail(String email, int excludeId);
}
