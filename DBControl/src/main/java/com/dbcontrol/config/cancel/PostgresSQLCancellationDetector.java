package com.dbcontrol.config.cancel;

import java.sql.SQLException;

public class PostgresSQLCancellationDetector implements SQLCancellationDetector {

	@Override
	public boolean isSQLCanceled(SQLException exception) {
		return exception.getErrorCode() == 57014;
	}

}
