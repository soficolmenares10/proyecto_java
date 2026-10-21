package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import java.util.List;
import java.util.Optional;

/** Data access operations for electronic invoices. */
public interface InvoiceDao {

    /** The next consecutive invoice number (only the number, without prefix). */
    long nextSequence();

    /**
     * Saves the invoice and its lines in one transaction.
     *
     * @return the generated invoice id
     */
    int insert(Invoice invoice, List<InvoiceLine> lines);

    Optional<Invoice> findById(int id);

    Optional<Invoice> findByNumber(String number);

    Optional<Invoice> findByPaymentId(int paymentId);

    List<InvoiceLine> findLines(int invoiceId);

    /** All the invoices, newest first. */
    List<Invoice> findAll();
}
