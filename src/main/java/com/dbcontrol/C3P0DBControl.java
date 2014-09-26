package com.dbcontrol;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.dbcontrol.config.DBControlConfig;
import com.dbcontrol.config.cancel.SQLCancellationDetector;
import com.dbcontrol.exceptions.DBException;
import com.dbcontrol.handlers.QueryHandler;
import com.dbcontrol.handlers.RunInTransaction;
import com.dbcontrol.handlers.WithConnection;
import com.dbcontrol.results.DBFKData;
import com.dbcontrol.results.DBMetaData;
import com.dbcontrol.results.DBRow;
import com.dbcontrol.results.StoredProcedureResults;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * DBControl implementation wrapper around a C3P0 pooled JDBC database connection.
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
	public <T, E extends Exception> T inTransaction(RunInTransaction<T, E> trans)
			throws SQLException, E {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.inTransaction(trans);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public <T, E extends Exception> T withConnection(WithConnection<T, E> with)
			throws SQLException, E {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return with.with(wrapper);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public List<DBRow> query(String sql, Object... params) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.query(sql, params);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public void queryHandle(String sql, Object[] params, QueryHandler handler)
			throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			wrapper.queryHandle(sql, params, handler);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public Future<List<DBRow>> queryAsync(final String sql,
			final Object... params) {
		return new Future<List<DBRow>>() {
			private volatile Future<List<DBRow>> cancel = null;
			private volatile boolean canceled = false;
			private volatile boolean done = false;

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				canceled = cancel.cancel(mayInterruptIfRunning);
				return canceled;
			}

			@Override
			public List<DBRow> get() throws InterruptedException,
					ExecutionException {
				ConnectionWrapper wrapper = null;
				try {
					wrapper = new JDBCConnectionWrapper(getConnection(),
							cancelDetector);
					cancel = wrapper.queryAsync(sql, params);
					return cancel.get();
				} catch (ExecutionException | InterruptedException ex) {
					throw ex;
				} catch (SQLException ex) {
					throw new ExecutionException(ex);
				} finally {
					done = true;
					if (wrapper != null) {
						try {
							wrapper.close();
						} catch (SQLException ex) {
							throw new ExecutionException(ex);
						}
					}
				}
			}

			@Override
			public List<DBRow> get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException {
				throw new RuntimeException("Get with timeout not supported");
			}

			@Override
			public boolean isCancelled() {
				return canceled;
			}

			@Override
			public boolean isDone() {
				return done;
			}
		};
	}

	@Override
	public void update(String tableName, DBRow row,
			Map<String, Object> fieldValues) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			wrapper.update(tableName, row, fieldValues);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public Object insert(String tableName, String generatedKeyField, DBRow row,
			Map<String, Object> fieldValues) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.insert(tableName, generatedKeyField, row,
					fieldValues);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public Object directInsert(String tableName, Map<String, Object> fieldValues)
			throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.directInsert(tableName, fieldValues);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public StoredProcedureResults callStoredProcedure(String storedProc,
			Object... params) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.callStoredProcedure(storedProc, params);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public void delete(String tableName, DBRow row) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			wrapper.delete(tableName, row);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public int directExecute(String sql, Object... params) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.directExecute(sql, params);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public void alterExecute(String sql) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			wrapper.alterExecute(sql);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public void close() throws SQLException {
		pool.close();
		pool = null;
	}

	@Override
	public DBMetaData getTableMetaData(String tableName) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.getTableMetaData(tableName);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}

	@Override
	public DBFKData getForeignKeyData(String tableName) throws SQLException {
		ConnectionWrapper wrapper = null;
		try {
			wrapper = new JDBCConnectionWrapper(getConnection(), cancelDetector);
			return wrapper.getForeignKeyData(tableName);
		} finally {
			if (wrapper != null) {
				wrapper.close();
			}
		}
	}
}
