package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;
import java.util.List;
import java.util.Optional;

/** Data access operations for the payment concept catalog. */
public interface ConceptDao {

    /** @return the generated concept id */
    int insert(PaymentConcept concept);

    Optional<PaymentConcept> findById(int id);

    List<PaymentConcept> findAll();

    /** Updates name, description, base amount, recurrence and IVA (the code never changes). */
    boolean update(PaymentConcept concept);

    boolean setActive(int conceptId, boolean active);

    boolean existsByCode(String code);
}
