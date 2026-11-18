package com.littlehorizons.erp.dao;

/**
 * Error while reading or writing the database. The DAO writes the technical detail to
 * error_log.txt and keeps a clear message for the user, so the controllers and the views never
 * deal with SQLException.
 */
public class DataAccessException extends Exception {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
