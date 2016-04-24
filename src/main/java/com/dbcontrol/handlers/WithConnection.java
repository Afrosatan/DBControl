package com.dbcontrol.handlers;

import com.dbcontrol.DBConnection;

import java.sql.SQLException;

/**
 * Interface to implement to interact with a DBConnection.
 *
 * @author Derek Mulvihill - Oct 28, 2013
 */
public interface WithConnection<T, E extends Exception> {
    T with(DBConnection connect) throws SQLException, E;
}
