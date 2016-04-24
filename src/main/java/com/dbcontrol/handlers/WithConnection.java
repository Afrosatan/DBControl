package com.dbcontrol.handlers;

import com.dbcontrol.ConnectionWrapper;
import com.dbcontrol.DBControl;

import java.sql.SQLException;

/**
 * Companion interface to use with {@link DBControl#withConnection}.
 *
 * @author Derek Mulvihill - Oct 28, 2013
 */
public interface WithConnection<T, E extends Exception> {
    T with(ConnectionWrapper connect) throws SQLException, E;
}
