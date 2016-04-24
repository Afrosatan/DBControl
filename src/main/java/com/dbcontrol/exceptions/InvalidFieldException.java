package com.dbcontrol.exceptions;

/**
 * Thrown when an invalid field is requested from a DBRow.
 *
 * @author Derek Mulvihill - Aug 6, 2013
 */
public class InvalidFieldException extends RuntimeException {
    public InvalidFieldException(String message) {
        super(message);
    }
}
