package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.Validator;
import java.util.List;

/**
 * Business logic for guardians and for linking them to students.
 */
public class GuardianController {

    private final GuardianDao guardianDao;
    private final StudentDao studentDao;

    public GuardianController(GuardianDao guardianDao, StudentDao studentDao) {
        this.guardianDao = guardianDao;
        this.studentDao = studentDao;
    }

    /** @return the new guardian id */
    public int register(Guardian guardian) {
        validate(guardian, 0);
        return guardianDao.insert(guardian);
    }

    public void update(Guardian guardian) {
        if (guardianDao.findById(guardian.getId()).isEmpty()) {
            throw new BusinessException("El acudiente no existe.");
        }
        validate(guardian, guardian.getId());
        guardianDao.update(guardian);
    }

    public Guardian getByDocument(String documentNumber) {
        return guardianDao.findByDocument(documentNumber)
                .orElseThrow(() -> new BusinessException(
                        "No existe un acudiente con el documento " + documentNumber + "."));
    }

    public boolean isDocumentTaken(String documentNumber) {
        return guardianDao.existsByDocument(documentNumber, 0);
    }

    public List<Guardian> listAll() {
        return guardianDao.findAll();
    }

    public List<Guardian> getGuardiansOfStudent(int studentId) {
        return guardianDao.findByStudentId(studentId);
    }

    /** All the students of one guardian (for example, brothers and sisters). */
    public List<Student> getStudentsOfGuardian(int guardianId) {
        return studentDao.findByGuardianId(guardianId);
    }

    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        if (studentDao.findById(studentId).isEmpty()) {
            throw new BusinessException("El estudiante no existe.");
        }
        if (guardianDao.findById(guardianId).isEmpty()) {
            throw new BusinessException("El acudiente no existe.");
        }
        boolean alreadyLinked = guardianDao.findByStudentId(studentId).stream()
                .anyMatch(guardian -> guardian.getId() == guardianId);
        if (alreadyLinked) {
            throw new BusinessException("Ese acudiente ya está asociado a este estudiante.");
        }
        guardianDao.linkToStudent(studentId, guardianId, primary, emergencyContact);
    }

    public void unlink(int studentId, int guardianId) {
        boolean removed = guardianDao.unlink(studentId, guardianId);
        if (!removed) {
            throw new BusinessException("No se pudo quitar: el estudiante debe conservar al menos un acudiente "
                    + "y el acudiente debe estar asociado a él.");
        }
    }

    private void validate(Guardian guardian, int excludeId) {
        Rules.requireText(guardian.getFirstName(), "El primer nombre", 100);
        Rules.limitText(guardian.getMiddleName(), "El segundo nombre", 100);
        Rules.requireText(guardian.getLastName(), "El primer apellido", 100);
        Rules.limitText(guardian.getSecondLastName(), "El segundo apellido", 100);
        Rules.requireOneOf(guardian.getDocumentType(), Guardian.DOCUMENT_TYPES, "El tipo de documento");
        if (!Validator.isValidDocument(guardian.getDocumentNumber())) {
            throw new BusinessException("El número de documento debe tener de 5 a 20 letras o números.");
        }
        if (guardianDao.existsByDocument(guardian.getDocumentNumber(), excludeId)) {
            throw new BusinessException("Ya existe un acudiente con el documento "
                    + guardian.getDocumentNumber() + ".");
        }
        Rules.requireOneOf(guardian.getRelationship(), Guardian.RELATIONSHIPS, "El parentesco");
        if (!Validator.isValidPhone(guardian.getPrimaryPhone())) {
            throw new BusinessException("El teléfono principal es obligatorio y debe ser válido.");
        }
        Rules.optionalPhone(guardian.getSecondaryPhone(), "El teléfono secundario");
        Rules.optionalEmail(guardian.getEmail(), "El correo");
        Rules.limitText(guardian.getEmail(), "El correo", 100);
        Rules.limitText(guardian.getAddress(), "La dirección", 255);
        Rules.limitText(guardian.getOccupation(), "La ocupación", 100);
    }
}
