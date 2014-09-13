package com.dbcontrol.handlers;

import java.sql.SQLException;

import com.dbcontrol.ConnectionWrapper;

/**
 * Interface to use a ConnectionWrapper in a transaction.
 * @author Derek Mulvihill - Oct 3, 2013
 */
public interface RunInTransaction<T, E extends Exception> {
	T run(ConnectionWrapper connect) throws SQLException, E;
}