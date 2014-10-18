package com.dbcontrol.config.cancel;

import java.sql.SQLException;

/**
 * Detects canceled SQLExceptions for Microsoft SQL Server.
 * @author Derek Mulvihill - Sep 13, 2014
 */
public class MSSQLCancellationDetector implements SQLCancellationDetector {
	@Override
	public boolean isSQLCanceled(SQLException exception) {
		return exception.getErrorCode() == 0
				&& "HY008".equals(exception.getSQLState());
	}
}
