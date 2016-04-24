package com.dbcontrol.exceptions;

/**
 * Exception for some errors in DBControl.
 *
 * @author Derek Mulvihill - Oct 3, 2013
 */
public class DBException extends Exception {
    public DBException(String message) {
        super(message);
    }

    public DBException(String message, Exception ex) {
        super(message, ex);
    }
}
