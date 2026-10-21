package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentRateDao;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.StudentRate;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Business logic for the payment concept catalog and for the custom amounts per student.
 */
public class ConceptController {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{2,20}$");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ConceptDao conceptDao;
    private final StudentRateDao rateDao;
    private final StudentDao studentDao;

    public ConceptController(ConceptDao conceptDao, StudentRateDao rateDao, StudentDao studentDao) {
        this.conceptDao = conceptDao;
        this.rateDao = rateDao;
        this.studentDao = studentDao;
    }

    // ------------------------------------------------------------------ concepts

    /** @return the new concept id */
    public int register(PaymentConcept concept) {
        concept.setCode(concept.getCode() == null ? null : concept.getCode().trim().toUpperCase());
        if (concept.getCode() == null || !CODE_PATTERN.matcher(concept.getCode()).matches()) {
            throw new BusinessException("El código debe tener de 2 a 20 letras, números, guion o guion bajo (ej: UNIFORME).");
        }
        if (conceptDao.existsByCode(concept.getCode())) {
            throw new BusinessException("Ya existe un concepto con el código " + concept.getCode() + ".");
        }
        validateEditableFields(concept);
        concept.setActive(true);
        return conceptDao.insert(concept);
    }

    public void update(PaymentConcept concept) {
        getById(concept.getId());
        validateEditableFields(concept);
        conceptDao.update(concept);
    }

    public void setActive(int conceptId, boolean active) {
        PaymentConcept concept = getById(conceptId);
        if (concept.isActive() == active) {
            throw new BusinessException("El concepto ya está " + (active ? "activo" : "inactivo") + ".");
        }
        conceptDao.setActive(conceptId, active);
    }

    public PaymentConcept getById(int conceptId) {
        return conceptDao.findById(conceptId).orElseThrow(() -> new BusinessException("El concepto no existe."));
    }

    public List<PaymentConcept> listAll() {
        return conceptDao.findAll();
    }

    // -------------------------------------------------------------- student rates

    /**
     * The amount a student must pay for a concept in a year: his or her custom amount if there
     * is an active one (sibling discount, scholarship), otherwise the base amount of the concept.
     */
    public BigDecimal effectiveAmount(int studentId, int conceptId, int academicYear) {
        PaymentConcept concept = getById(conceptId);
        return rateDao.findActive(studentId, conceptId, academicYear)
                .map(StudentRate::amount)
                .orElse(concept.getBaseAmount());
    }

    public void setStudentRate(int studentId, int conceptId, int academicYear, BigDecimal amount, String reason) {
        if (studentDao.findById(studentId).isEmpty()) {
            throw new BusinessException("El estudiante no existe.");
        }
        getById(conceptId);
        Rules.requireAcademicYear(academicYear);
        checkAmount(amount, "El valor personalizado");
        Rules.requireText(reason, "El motivo", 150);
        rateDao.save(studentId, conceptId, academicYear, amount, reason.trim());
    }

    public List<StudentRate> ratesOfStudent(int studentId) {
        return rateDao.findByStudent(studentId);
    }

    public void removeStudentRate(int studentId, int rateId) {
        StudentRate rate = rateDao.findById(rateId)
                .orElseThrow(() -> new BusinessException("El valor personalizado no existe."));
        if (rate.studentId() != studentId) {
            throw new BusinessException("Ese valor personalizado no pertenece a este estudiante.");
        }
        if (!rate.active()) {
            throw new BusinessException("Ese valor personalizado ya estaba desactivado.");
        }
        rateDao.deactivate(rateId);
    }

    // ------------------------------------------------------------------ helpers

    private void validateEditableFields(PaymentConcept concept) {
        Rules.requireText(concept.getName(), "El nombre del concepto", 100);
        Rules.limitText(concept.getDescription(), "La descripción", 1000);
        checkAmount(concept.getBaseAmount(), "El valor base");
        BigDecimal vat = concept.getVatPercent();
        if (vat == null || vat.signum() < 0 || vat.compareTo(HUNDRED) > 0 || vat.scale() > 2) {
            throw new BusinessException("El IVA debe estar entre 0 y 100 con máximo 2 decimales.");
        }
    }

    private void checkAmount(BigDecimal amount, String label) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0 || amount.scale() > 2) {
            throw new BusinessException(label + " debe ser mayor que cero, con máximo 2 decimales y menor a 100 millones.");
        }
    }
}
