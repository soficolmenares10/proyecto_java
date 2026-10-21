package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.ResourceLoan;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Data access operations for resource loans. It works with the two loan tables of the
 * database (students and professors) as if they were one.
 */
public interface LoanDao {

    /**
     * Registers a loan and marks the resource as "Prestado", in one transaction. The resource row is
     * locked, so the same resource cannot be lent twice at the same time.
     *
     * @return the generated loan id, or -1 if the resource was not available
     */
    int lend(int resourceId, BorrowerType type, int borrowerId, LocalDate loanDate, LocalDate dueDate, String notes);

    /**
     * Registers the return of the loan and makes the resource available again, in one transaction.
     *
     * @return false if the loan was not open
     */
    boolean giveBack(BorrowerType type, int loanId, LocalDate returnDate);

    /** Changes to "Vencido" all the active loans whose due date has passed. @return how many changed */
    int markOverdue(LocalDate today);

    Optional<ResourceLoan> findOpenByResource(int resourceId);

    List<ResourceLoan> findOpen();

    List<ResourceLoan> findOverdue();

    List<ResourceLoan> findByBorrower(BorrowerType type, int borrowerId);

    List<ResourceLoan> findByResource(int resourceId);

    long countOpenByBorrower(BorrowerType type, int borrowerId);
}
