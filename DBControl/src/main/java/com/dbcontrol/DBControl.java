package com.dbcontrol;

import java.sql.SQLException;

import com.dbcontrol.config.DBControlConfig;
import com.dbcontrol.exceptions.DBException;
import com.dbcontrol.handlers.WithConnection;

/**
 * Extension of DBConnection that exposes an interface it initialize the connection amongst other things.<br>
 * Implementations of DBControl can be pooled and each method call can be run against a different underlying database connection.
 * @author Derek Mulvihill - May 4, 2014
 */
public interface DBControl extends DBConnection {
	/**
	 * Set up the database connection. Before this is called, all other methods on this instance should fail.
	 * @throws SQLException if there is an error connecting to the database
	 * @throws DBException if the DBControl has already been initialized or other errors
	 */
	void init(DBControlConfig config) throws SQLException, DBException;

	/**
	 * Run some code with a ConnectionWrapper.
	 */
	<T, E extends Exception> T withConnection(WithConnection<T, E> with)
			throws SQLException, E;
}
