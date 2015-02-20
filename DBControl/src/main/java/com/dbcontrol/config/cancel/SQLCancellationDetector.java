package com.dbcontrol.config.cancel;

import java.sql.SQLException;

/**
 * Interface to implement for inspecting SQLExceptions to test if they represent that a statement was cancelled.
 * @author Derek Mulvihill - Sep 13, 2014
 */
public interface SQLCancellationDetector {
	boolean isSQLCanceled(SQLException exception);
}
