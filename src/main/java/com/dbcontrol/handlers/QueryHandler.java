package com.dbcontrol.handlers;

import com.dbcontrol.results.DBRow;

import java.sql.SQLException;

/**
 * Interface to implement to handle individual DBRows from a query instead of reading in a whole list.
 *
 * @author Derek Mulvihill - Oct 24, 2013
 */
public interface QueryHandler {
    void handleRow(DBRow row) throws SQLException;
}
