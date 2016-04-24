package com.dbcontrol.config.cancel;

import java.sql.SQLException;

/**
 * Detects canceled SQLExceptions for PostgreSQL.
 *
 * @author Derek Mulvihill - Sep 13, 2014
 */
public class PostgresSQLCancellationDetector implements SQLCancellationDetector {
    @Override
    public boolean isSQLCanceled(SQLException exception) {
        return exception.getErrorCode() == 57014;
    }
}
