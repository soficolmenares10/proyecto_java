package com.littlehorizons.erp.service;

/**
 * Thrown when the invoice document (PDF) could not be created.
 */
public class InvoiceGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvoiceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
