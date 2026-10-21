package com.littlehorizons.erp.model;

import java.time.LocalDate;

/**
 * The loan of a resource to a student or a professor.
 */
public record ResourceLoan(int id, int resourceId, String resourceCode, String resourceName,
                           BorrowerType borrowerType, int borrowerId, String borrowerName,
                           String borrowerDocument, LocalDate loanDate, LocalDate dueDate,
                           LocalDate returnDate, String status, String notes) {

    public static final String ACTIVE = "Activo";
    public static final String RETURNED = "Devuelto";
    public static final String OVERDUE = "Vencido";

    /** True if the loan is still open (the resource has not been returned). */
    public boolean isOpen() {
        return ACTIVE.equals(status) || OVERDUE.equals(status);
    }
}
