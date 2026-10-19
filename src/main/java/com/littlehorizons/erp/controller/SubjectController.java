package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.model.Subject;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Business logic for the subject (materia) catalog. */
public class SubjectController {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{2,20}$");

    private final SubjectDao subjectDao;
    private final EnrollmentDao enrollmentDao;

    public SubjectController(SubjectDao subjectDao, EnrollmentDao enrollmentDao) {
        this.subjectDao = subjectDao;
        this.enrollmentDao = enrollmentDao;
    }

    /** @return the new subject id */
    public int register(Subject subject) {
        subject.setCode(subject.getCode() == null ? null : subject.getCode().trim().toUpperCase());
        if (subject.getCode() == null || !CODE_PATTERN.matcher(subject.getCode()).matches()) {
            throw new BusinessException("El código debe tener de 2 a 20 letras, números o guiones (ej: MAT-5).");
        }
        if (subjectDao.existsByCode(subject.getCode())) {
            throw new BusinessException("Ya existe una materia con el código " + subject.getCode() + ".");
        }
        boolean gradeExists = enrollmentDao.findAllGrades().stream()
                .anyMatch(grade -> grade.getId() == subject.getGradeId());
        if (!gradeExists) {
            throw new BusinessException("El grado seleccionado no existe.");
        }
        validateEditableFields(subject);
        subject.setActive(true);
        return subjectDao.insert(subject);
    }

    public void update(Subject subject) {
        if (subjectDao.findById(subject.getId()).isEmpty()) {
            throw new BusinessException("La materia no existe.");
        }
        validateEditableFields(subject);
        subjectDao.update(subject);
    }

    public void setActive(int subjectId, boolean active) {
        Subject subject = getById(subjectId);
        if (subject.isActive() == active) {
            throw new BusinessException("La materia ya está " + (active ? "activa" : "inactiva") + ".");
        }
        subjectDao.setActive(subjectId, active);
    }

    public Subject getById(int subjectId) {
        return subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("La materia no existe."));
    }

    public List<Subject> listByGrade(int gradeId) {
        return subjectDao.findByGrade(gradeId);
    }

    public List<Subject> listActiveByGrade(int gradeId) {
        return subjectDao.findByGrade(gradeId).stream()
                .filter(Subject::isActive)
                .collect(Collectors.toList());
    }

    private void validateEditableFields(Subject subject) {
        Rules.requireText(subject.getName(), "El nombre de la materia", 100);
        Rules.limitText(subject.getDescription(), "La descripción", 1000);
        if (subject.getWeeklyHours() < 1 || subject.getWeeklyHours() > 10) {
            throw new BusinessException("Las horas semanales deben estar entre 1 y 10.");
        }
    }
}
