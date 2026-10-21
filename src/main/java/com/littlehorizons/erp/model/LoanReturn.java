package com.littlehorizons.erp.model;

/** Result of returning a resource: the loan that was closed and how many days late it was returned. */
public record LoanReturn(ResourceLoan loan, long daysLate) {
}
