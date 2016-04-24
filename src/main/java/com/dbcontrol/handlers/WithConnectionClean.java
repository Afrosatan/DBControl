package com.dbcontrol.handlers;

import com.dbcontrol.DBConnection;

import java.sql.SQLException;

/**
 * Implements WithConnection to a remove need for extra boilerplate for generics and a return line.
 *
 * @author Derek Mulvihill - Jun 7, 2014
 */
public abstract class WithConnectionClean implements WithConnection<Void, SQLException> {
    @Override
    public final Void with(DBConnection connect) throws SQLException {
        withConn(connect);
        return null;
    }

    public abstract void withConn(DBConnection connect) throws SQLException;
}
