package com.dbcontrol.named;

/**
 * Exception for a named query parameter that has been found in the SQL, but not in the parameter bindings.
 *
 * @author Derek Mulvihill - Apr 09, 2017
 */
public class UnboundNameQueryParameterException extends RuntimeException {
    public UnboundNameQueryParameterException(String param) {
        super("Unbound query parameter: " + param);
    }
}
