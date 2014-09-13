package com.dbcontrol.exceptions;

/**
 * Exception for some errors in DBControl.
 * @author Derek Mulvihill - Oct 3, 2013
 */
public class DBException extends Exception {
	private static final long serialVersionUID = -6958744448681566915L;

	public DBException(String message) {
		super(message);
	}

	public DBException(String message, Exception ex) {
		super(message, ex);
	}
}
