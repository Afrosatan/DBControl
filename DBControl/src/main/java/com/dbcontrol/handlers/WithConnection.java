package com.dbcontrol.handlers;

import java.sql.SQLException;

import com.dbcontrol.ConnectionWrapper;
import com.dbcontrol.DBControl;

/**
 * Companion interface to use with {@link DBControl#withConnection}.
 * @author Derek Mulvihill - Oct 28, 2013
 */
public interface WithConnection<T, E extends Exception> {
	T with(ConnectionWrapper connect) throws SQLException, E;
}
