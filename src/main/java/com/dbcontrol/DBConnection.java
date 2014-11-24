package com.dbcontrol;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.dbcontrol.handlers.QueryHandler;
import com.dbcontrol.handlers.RunInTransaction;
import com.dbcontrol.results.DBFKData;
import com.dbcontrol.results.DBMetaData;
import com.dbcontrol.results.DBRow;
import com.dbcontrol.results.StoredProcedureResults;

/**
 * Common super-interface for methods shared between ConnectionWrapper and DBControl.
 * @author Derek Mulvihill - May 4, 2014
 */
public interface DBConnection extends AutoCloseable {
	List<DBRow> query(String sql, Object... params) throws SQLException;

	/**
	 * Run the provided SQL and pass each result as a DBRow through a QueryHandler.
	 */
	void queryHandle(String sql, Object[] params, QueryHandler handler)
			throws SQLException;

	/**
	 * Create a Future for running the provided Sql query in an asynchronous fashion.
	 */
	Future<List<DBRow>> queryAsync(String sql, Object... params);

	/**
	 * Update a record in the named table using the DBRow for metadata.<br>
	 * The record updated must have all of the same values from the provided DBRow and will be overwritten by values in the fieldValues.<br>
	 * Fails if multiple records are updated or if no records are updated.
	 */
	void update(String tableName, DBRow row, Map<String, Object> fieldValues)
			throws SQLException;

	/**
	 * Update a record in the named table using key-value pairs for the where statement.<br>
	 * Fails if multiple records are updated or if no records are updated.
	 */
	void update(String tableName, Map<String, Object> setValues,
			Map<String, Object> whereValues) throws SQLException;

	/**
	 * Insert a record into the named table using the DBRow for metadata.<br>
	 * The values for each field are set from the DBRow and overwritten by values in the fieldValues.
	 * @return the generated key if there was one, or null
	 */
	Object insert(String tableName, String generatedKeyField, DBRow row,
			Map<String, Object> fieldValues) throws SQLException;

	/**
	 * Insert a record into the named table with values from the provided fieldValues.
	 */
	Object directInsert(String tableName, Map<String, Object> fieldValues)
			throws SQLException;

	/**
	 * Delete a record from the named table that has all the same field/column values as the provided DBRow.<br>
	 * Fails if more or less than 1 record was affected.
	 */
	void delete(String tableName, DBRow row) throws SQLException;

	/**
	 * Execute an arbitrary sql statement that update the database.
	 * @return the number of rows affected
	 */
	int directExecute(String sql, Object... params) throws SQLException;

	/**
	 * Execute arbitrary sql that can alter database (Eg. create, alter, drop tables).
	 */
	void alterExecute(String sql) throws SQLException;

	/**
	 * Run a stored procedure.
	 */
	StoredProcedureResults callStoredProcedure(String storedProc,
			Object... params) throws SQLException;

	/**
	 * Close and release any connections.
	 */
	void close() throws SQLException;

	/**
	 * Create a DBMetaData object that represents fields/columns in the named table without retrieving any values/data from the table.
	 */
	DBMetaData getTableMetaData(String tableName) throws SQLException;

	/**
	 * Create a DBFKData object for the foreign keys in the named table.
	 */
	DBFKData getForeignKeyData(String tableName) throws SQLException;

	/**
	 * Run some code with a ConnectionWrapper that is in transaction and will be committed after the method returns without an Exception.
	 */
	<T, E extends Exception> T inTransaction(RunInTransaction<T, E> trans)
			throws SQLException, E;
}
