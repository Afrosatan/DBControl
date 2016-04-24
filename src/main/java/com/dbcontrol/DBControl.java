package com.dbcontrol;

import com.dbcontrol.config.DBControlConfig;
import com.dbcontrol.exceptions.DBException;
import com.dbcontrol.handlers.WithConnection;

import java.sql.SQLException;

/**
 * Interface that for an object that is capable of creating DBConnection instances for interacting with a database.
 *
 * @author Derek Mulvihill - May 4, 2014
 */
public interface DBControl extends AutoCloseable {
    /**
     * Set up the database connection. Before this is called, all other methods on this instance should fail.
     *
     * @throws SQLException if there is an error connecting to the database
     * @throws DBException  if the DBControl has already been initialized or other errors
     */
    void init(DBControlConfig config) throws SQLException, DBException;

    /**
     * Run some code with a ConnectionWrapper.
     */
    <T, E extends Exception> T withConnection(WithConnection<T, E> with) throws SQLException, E;

    /**
     * Run some code with a ConnectionWrapper that is in transaction and will be committed after the method returns without an Exception.
     */
    <T, E extends Exception> T inTransaction(WithConnection<T, E> trans) throws SQLException, E;

    /**
     * Close and release any connections.
     */
    void close() throws SQLException;
}
