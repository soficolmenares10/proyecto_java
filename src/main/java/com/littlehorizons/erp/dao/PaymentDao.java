package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access operations for payments. */
public interface PaymentDao {

    /** The next free receipt number of a year, for example REC-2026-000372. */
    String nextReceiptNumber(int year);

    /**
     * Saves a payment. If chargeId is not null, the charge is marked as paid in the same transaction.
     *
     * @return the generated payment id
     */
    int insert(Payment payment, Integer chargeId);

    Optional<Payment> findById(int id);

    Optional<Payment> findByReceipt(String receiptNumber);

    List<Payment> findByStudent(int studentId);

    /** Payments of all the students linked to the guardian (for example, all his or her children). */
    List<Payment> findByGuardian(int guardianId);

    List<Payment> findBetween(LocalDate from, LocalDate to);
}
