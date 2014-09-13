package com.dbcontrol.handlers;

import java.sql.SQLException;

import com.dbcontrol.ConnectionWrapper;

/**
 * Implements RunInTransaction to a remove need for extra boilerplate for generics 
 * & a return line.
 * @author Derek Mulvihill - Jun 7, 2014
 */
public abstract class RunInTransactionClean implements
		RunInTransaction<Void, SQLException> {
	@Override
	public final Void run(ConnectionWrapper connect) throws SQLException {
		runTran(connect);
		return null;
	}

	public abstract void runTran(ConnectionWrapper connect) throws SQLException;
}
