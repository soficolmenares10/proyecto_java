package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.util.Validator;
import java.time.LocalDate;
import java.util.List;

/** Business logic for professors. */
public class ProfessorController {

    private final ProfessorDao professorDao;

    public ProfessorController(ProfessorDao professorDao) {
        this.professorDao = professorDao;
    }

    /** @return the new professor id */
    public int register(Professor professor) {
        validate(professor, 0);
        professor.setStatus("Activo");
        return professorDao.insert(professor);
    }

    public void update(Professor professor) {
        if (professorDao.findById(professor.getId()).isEmpty()) {
            throw new BusinessException("El profesor no existe.");
        }
        validate(professor, professor.getId());
        professorDao.update(professor);
    }

    public void changeStatus(int professorId, String newStatus) {
        Professor professor = professorDao.findById(professorId)
                .orElseThrow(() -> new BusinessException("El profesor no existe."));
        Rules.requireOneOf(newStatus, Professor.STATUSES, "El estado");
        if (newStatus.equals(professor.getStatus())) {
            throw new BusinessException("El profesor ya está en estado " + newStatus + ".");
        }
        professorDao.updateStatus(professorId, newStatus);
    }

    public Professor getByDocument(String documentNumber) {
        return professorDao.findByDocument(documentNumber)
                .orElseThrow(() -> new BusinessException(
                        "No existe un profesor con el documento " + documentNumber + "."));
    }

    public boolean isDocumentTaken(String documentNumber) {
        return professorDao.existsByDocument(documentNumber, 0);
    }

    public List<Professor> listAll() {
        return professorDao.findAll();
    }

    public List<Professor> search(String text) {
        if (!Validator.isNotBlank(text)) {
            throw new BusinessException("Escriba un texto para buscar.");
        }
        return professorDao.search(text.trim());
    }

    private void validate(Professor professor, int excludeId) {
        Rules.requireText(professor.getFirstName(), "El primer nombre", 100);
        Rules.limitText(professor.getMiddleName(), "El segundo nombre", 100);
        Rules.requireText(professor.getLastName(), "El primer apellido", 100);
        Rules.limitText(professor.getSecondLastName(), "El segundo apellido", 100);
        Rules.requireOneOf(professor.getDocumentType(), Professor.DOCUMENT_TYPES, "El tipo de documento");
        if (!Validator.isValidDocument(professor.getDocumentNumber())) {
            throw new BusinessException("El número de documento debe tener de 5 a 20 letras o números.");
        }
        if (professorDao.existsByDocument(professor.getDocumentNumber(), excludeId)) {
            throw new BusinessException("Ya existe un profesor con el documento "
                    + professor.getDocumentNumber() + ".");
        }
        if (!Validator.isValidEmail(professor.getEmail())) {
            throw new BusinessException("El correo es obligatorio y debe ser válido.");
        }
        Rules.limitText(professor.getEmail(), "El correo", 100);
        if (professorDao.existsByEmail(professor.getEmail(), excludeId)) {
            throw new BusinessException("Ya existe un profesor con el correo " + professor.getEmail() + ".");
        }
        Rules.optionalPhone(professor.getPhone(), "El teléfono");
        Rules.limitText(professor.getSpecialization(), "La especialización", 100);
        LocalDate hireDate = professor.getHireDate();
        if (hireDate == null || hireDate.isAfter(LocalDate.now()) || hireDate.getYear() < 1970) {
            throw new BusinessException("La fecha de contratación debe estar entre 1970 y hoy.");
        }
    }
}
