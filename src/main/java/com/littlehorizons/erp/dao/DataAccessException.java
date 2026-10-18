package com.littlehorizons.erp.dao;

/**
 * Thrown by the DAOs when something fails while talking to the database.
 * The original SQLException is kept as the "cause" and is also written to error_log.txt.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
