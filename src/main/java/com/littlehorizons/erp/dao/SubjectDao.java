package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
import java.util.List;
import java.util.Optional;

/** Data access operations for subjects (materias). */
public interface SubjectDao {

    /** @return the generated subject id */
    int insert(Subject subject);

    Optional<Subject> findById(int id);

    Optional<Subject> findByCode(String code);

    List<Subject> findAll();

    List<Subject> findByGrade(int gradeId);

    /** Updates name, description and weekly hours (the code and the grade never change). */
    boolean update(Subject subject);

    boolean setActive(int subjectId, boolean active);

    boolean existsByCode(String code);
}
