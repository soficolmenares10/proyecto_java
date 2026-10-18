package com.littlehorizons.erp.controller;

/**
 * Thrown when the user tries something that breaks a business rule
 * (for example: duplicated document, enrolling an inactive student).
 * Its message is written for the end user and is shown by the views.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
