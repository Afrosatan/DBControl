package com.dbcontrol;

import com.dbcontrol.config.DBControlConfig;
import com.dbcontrol.config.cancel.SQLCancellationDetector;
import com.dbcontrol.exceptions.DBException;
import com.dbcontrol.handlers.WithConnection;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * DBControl implementation wrapper around a C3P0 pooled JDBC database connection.
 *
 * @author Derek Mulvihill - Aug 1, 2013
 */
public class C3P0DBControl implements DBControl {
    private ComboPooledDataSource pool = null;
    private SQLCancellationDetector cancelDetector;

    private Connection getConnection() throws SQLException {
        Connection connect = pool.getConnection();
        if (connect == null) {
            throw new SQLException("Connection was not acquired");
        }
        return connect;
    }

    @Override
    public void init(DBControlConfig config) throws SQLException, DBException {
        if (pool != null) {
            throw new DBException("Connection already initialized");
        }

        pool = new ComboPooledDataSource();
        try {
            pool.setDriverClass(config.getDriverClass());
        } catch (PropertyVetoException ex) {
            throw new DBException("driver vetoed", ex);
        }
        pool.setJdbcUrl(config.getUrl());
        pool.setUser(config.getUsername());
        pool.setPassword(config.getPassword());
        pool.setMinPoolSize(5);
        pool.setAcquireIncrement(5);
        pool.setMaxPoolSize(20);
        // pool.setTestConnectionOnCheckin(true);
        pool.setTestConnectionOnCheckout(true);
        if (config.getIdleConnectionTestSeconds() != null
                && config.getIdleConnectionTestSeconds() > 0) {
            pool.setIdleConnectionTestPeriod(config
                    .getIdleConnectionTestSeconds());
        }
        pool.setPreferredTestQuery(config.getTestQuery());
        pool.setAcquireRetryAttempts(3);
        cancelDetector = config.getSQLCancellationDetector();

        Connection connect = null;
        try {
            connect = getConnection();
        } finally {
            if (connect != null) {
                connect.close();
            }
        }
    }

    @Override
    public <T, E extends Exception> T inTransaction(WithConnection<T, E> trans) throws SQLException, E {
        try (DBConnection connect = new JDBCDBConnection(getConnection(), cancelDetector)) {
            return connect.inTransaction(trans);
        }
    }

    @Override
    public <T, E extends Exception> T withConnection(WithConnection<T, E> with) throws SQLException, E {
        try (DBConnection connect = new JDBCDBConnection(getConnection(), cancelDetector)) {
            return with.with(connect);
        }
    }

    @Override
    public void close() {
        pool.close();
        pool = null;
    }
}
