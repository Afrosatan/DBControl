package com.dbcontrol;

import com.dbcontrol.config.cancel.SQLCancellationDetector;
import com.dbcontrol.exceptions.RowsAffectedSQLException;
import com.dbcontrol.handlers.QueryHandler;
import com.dbcontrol.handlers.WithConnection;
import com.dbcontrol.handlers.WithConnectionClean;
import com.dbcontrol.results.*;
import com.dbcontrol.results.DBMetaData.DBFieldData;
import com.dbcontrol.results.DBMetaData.DBFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DBConnection implementation wrapper around a JDBC Connection.
 *
 * @author Derek Mulvihill - May 4, 2014
 */
public class JDBCDBConnection implements DBConnection {
    private static final Logger logger = LoggerFactory.getLogger(JDBCDBConnection.class);

    private final Connection connection;
    private int transactionDepth = 0;
    private final SQLCancellationDetector cancelDetector;

    public JDBCDBConnection(Connection connection, SQLCancellationDetector cancelDetector) {
        this.connection = connection;
        this.cancelDetector = cancelDetector;
    }

    @Override
    public List<DBRow> query(String sql, Object... params) throws SQLException {
        try (Statement statement = queryPre(sql, params)) {
            return DataUtil.getRowsFromResultSet(queryExecute(statement, sql));
        }
    }

    @Override
    public int queryHandle(String sql, Object[] params, QueryHandler handler) throws SQLException {
        Statement statement = null;
        try {
            statement = queryPre(sql, params);
            ResultSet rs = queryExecute(statement, sql);

            DBMetaData dbm = new DBMetaData(rs.getMetaData());
            int count = 0;
            while (rs.next()) {
                handler.handleRow(new DBRow(dbm, rs));
                count++;
            }
            return count;
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public Future<List<DBRow>> queryAsync(final String sql, final Object... params) {
        return new Future<List<DBRow>>() {
            private volatile Statement statement = null;
            private volatile boolean canceled = false;
            private volatile boolean done = false;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                try {
                    statement.cancel();
                    canceled = true;
                    return true;
                } catch (SQLException ex) {
                    return false;
                }
            }

            @Override
            public List<DBRow> get() throws InterruptedException, ExecutionException {
                try {
                    try (Statement st = queryPre(sql, params)) {
                        statement = st;
                        return DataUtil.getRowsFromResultSet(queryExecute(statement, sql));
                    }
                } catch (SQLException ex) {
                    if (cancelDetector != null && cancelDetector.isSQLCanceled(ex)) {
                        logger.debug("Asynchronous SQL canceled", ex);
                        return null;
                    } else {
                        throw new ExecutionException(ex);
                    }
                } finally {
                    done = true;
                }
            }

            @Override
            public List<DBRow> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
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

    private Statement queryPre(String sql, Object... params) throws SQLException {
        logger.trace("SQL: " + sql);
        logger.trace("Parameters: " + Arrays.toString(params));
        Statement statement;
        if (params == null || params.length == 0) {
            statement = connection.createStatement();
        } else {
            PreparedStatement ps;
            statement = ps = connection.prepareStatement(sql);
            for (int i = 1; i <= params.length; i++) {
                Object param = params[i - 1];
                setPSObject(ps, i, DataUtil.getDBObject(param));
            }
        }
        return statement;
    }

    private ResultSet queryExecute(Statement statement, String sql) throws SQLException {
        ResultSet rs;
        if (statement instanceof PreparedStatement) {
            rs = ((PreparedStatement) statement).executeQuery();
        } else {
            rs = statement.executeQuery(sql);
        }
        return rs;
    }

    private int singleUpdate(final QueryBuilder sql) throws SQLException {
        logger.trace("SQL: " + sql.getSql());
        logger.trace("Parameters: " + Arrays.toString(sql.getParams()));

        return inTransaction(connect -> {
            try (PreparedStatement ps = connection.prepareStatement(sql.getSql())) {
                List<Object> params = sql.getParamList();
                for (int i = 1; i <= params.size(); i++) {
                    setPSObject(ps, i, params.get(i - 1));
                }
                int n = ps.executeUpdate();
                if (n == 0) {
                    throw new RowsAffectedSQLException("No rows affected during update, rolling back");
                } else if (n > 1) {
                    throw new RowsAffectedSQLException("Multiple rows affected during update, rolling back");
                }
                return n;
            }
        });
    }

    @Override
    public int update(String tableName, DBRow row, Map<String, Object> fieldValues) throws SQLException {
        QueryBuilder sql = new QueryBuilder();
        sql.append("UPDATE ");
        sql.append(tableName);
        sql.append(" SET ");
        boolean first = true;
        for (Entry<String, Object> entry : fieldValues.entrySet()) {
            Object obj = row.getObject(entry.getKey());
            if (obj == null) {
                if (entry.getValue() == null) {
                    continue;
                }
            } else if (entry.getValue() != null) {
                if (entry.getValue().equals(obj)) {
                    continue;
                }
            }
            if (first) {
                first = false;
            } else {
                sql.append(", ");
            }
            sql.append(entry.getKey());
            sql.append(" = ? ", DataUtil.getDBObject(entry.getValue()));
        }
        if (first) { // no changes
            return 0;
        }
        sql.append(" WHERE ");
        first = true;
        for (DBFieldData field : row.getMetadata().getFieldData().values()) {
            if (field.getType() == DBFieldType.CLOB) {
                continue; //don't compare clobs...
            }
            if (first) {
                first = false;
            } else {
                sql.append(" AND ");
            }
            Object obj = DataUtil.getDBObject(row.getObject(field.getName()));
            if (obj != null) {
                sql.append(field.getName());
                sql.append(" = ? ", obj);
            } else {
                sql.append(field.getName());
                sql.append(" IS NULL ");
            }
        }

        return singleUpdate(sql);
    }

    @Override
    public int update(String tableName, Map<String, Object> setValues, Map<String, Object> whereValues) throws SQLException {
        QueryBuilder sql = new QueryBuilder();
        sql.append("UPDATE ");
        sql.append(tableName);
        sql.append(" SET ");
        boolean first = true;
        for (Entry<String, Object> entry : setValues.entrySet()) {
            if (first) {
                first = false;
            } else {
                sql.append(", ");
            }
            sql.append(entry.getKey());
            sql.append(" = ? ", DataUtil.getDBObject(entry.getValue()));
        }
        sql.append(" WHERE ");
        first = true;
        for (Entry<String, Object> entry : whereValues.entrySet()) {
            if (first) {
                first = false;
            } else {
                sql.append(" AND ");
            }
            Object obj = DataUtil.getDBObject(entry.getValue());
            if (obj != null) {
                sql.append(entry.getKey());
                sql.append(" = ? ", obj);
            } else {
                sql.append(entry.getKey());
                sql.append(" IS NULL ");
            }
        }

        return singleUpdate(sql);
    }

    @Override
    public Object insert(String tableName, String generatedKeyField, DBRow row, Map<String, Object> fieldValuesParam) throws SQLException {
        Map<String, Object> fieldValues = new HashMap<>();
        for (Entry<String, Object> entry : fieldValuesParam.entrySet()) {
            fieldValues.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        for (DBFieldData field : row.getMetadata().getFieldData().values()) {
            if (!fieldValues.containsKey(field.getName())) {
                fieldValues
                        .put(field.getName(), row.getObject(field.getName()));
            }
        }
        Object key = directInsert(tableName, fieldValues);
        if (key != null && generatedKeyField != null) {
            //drivers return BigDecimals for generated keys
            if (row.getMetadata().getFieldData().get(generatedKeyField).getType() == DBFieldType.LONG && key instanceof BigDecimal) {
                key = ((BigDecimal) key).longValue();
            } else if (row.getMetadata().getFieldData().get(generatedKeyField).getType() == DBFieldType.INT && key instanceof BigDecimal) {
                key = ((BigDecimal) key).intValue();
            }
        }
        return key;
    }

    @Override
    public Object directInsert(String tableName, Map<String, Object> fieldValues) throws SQLException {
        final List<Object> parameters = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName);
        sql.append(" (");
        boolean first = true;
        List<String> fields = new ArrayList<>(fieldValues.keySet());
        for (String field : fields) {
            if (fieldValues.get(field) == null) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                sql.append(", ");
            }
            sql.append(field);
        }
        sql.append(") values (");
        first = true;
        for (String field : fields) {
            if (fieldValues.get(field) == null) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                sql.append(", ");
            }
            sql.append("?");
            parameters.add(DataUtil.getDBObject(fieldValues.get(field)));
        }
        sql.append(")");

        logger.trace("SQL: " + sql);
        logger.trace("Parameters: " + Arrays.toString(parameters.toArray()));

        class KeyStore {
            Object key;
        }
        final KeyStore key = new KeyStore();

        inTransaction(new WithConnectionClean() {
            @Override
            public void withConn(DBConnection connect) throws SQLException {
                try (PreparedStatement ps = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                    for (int i = 1; i <= parameters.size(); i++) {
                        setPSObject(ps, i, parameters.get(i - 1));
                    }
                    ps.executeUpdate();

                    try (ResultSet gkeys = ps.getGeneratedKeys()) {
                        if (gkeys.next()) {
                            key.key = gkeys.getObject(1);
                        }
                    }
                }
            }
        });

        return key.key;
    }

    @Override
    public int delete(String tableName, DBRow row) throws SQLException {
        final List<Object> parameters = new ArrayList<>();
        final StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(tableName);
        sql.append(" WHERE ");
        boolean first = true;
        List<String> fields = new ArrayList<>(row.getMetadata().getFieldData().keySet());
        for (String field : fields) {
            if (first) {
                first = false;
            } else {
                sql.append("AND ");
            }
            Object obj = DataUtil.getDBObject(row.getObject(field));
            if (obj != null) {
                sql.append(field);
                sql.append(" = ? ");
                parameters.add(obj);
            } else {
                sql.append(field);
                sql.append(" IS NULL ");
            }
        }

        logger.trace("SQL: " + sql);
        logger.trace("Parameters: " + Arrays.toString(parameters.toArray()));

        return inTransaction(connect -> {
            try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
                for (int i = 1; i <= parameters.size(); i++) {
                    setPSObject(ps, i, parameters.get(i - 1));
                }
                int n = ps.executeUpdate();
                if (n == 0) {
                    throw new RowsAffectedSQLException("No rows affected during delete.");
                } else if (n > 1) {
                    throw new RowsAffectedSQLException("Multiple rows affected during delete");
                }
                return n;
            }
        });
    }

    @Override
    public int directExecute(String sql, Object... params) throws SQLException {
        logger.trace("Direct SQL: " + sql);
        logger.trace("Parameters: " + Arrays.toString(params));
        if (params == null || params.length == 0) {
            try (Statement statement = connection.createStatement()) {
                return statement.executeUpdate(sql);
            }
        } else {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 1; i <= params.length; i++) {
                    setPSObject(ps, i, DataUtil.getDBObject(params[i - 1]));
                }
                return ps.executeUpdate();
            }
        }
    }

    @Override
    public void alterExecute(String sql) throws SQLException {
        logger.trace("Alter SQL: " + sql);
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @Override
    public StoredProcedureResults callStoredProcedure(String storedProc, Object... params) throws SQLException {
        StringBuilder sql = new StringBuilder("{ ? = call ");
        sql.append(storedProc);
        sql.append("(");
        if (params != null) {
            boolean first = true;
            for (@SuppressWarnings("unused") Object param : params) {
                if (first) {
                    first = false;
                } else {
                    sql.append(",");
                }
                sql.append("?");
            }
        }
        sql.append(") }");

        logger.trace("SQL: " + sql);
        logger.trace("Parameters: " + Arrays.toString(params));

        try (CallableStatement cs = connection.prepareCall(sql.toString())) {
            int oi = 1;
            cs.registerOutParameter(oi++, Types.INTEGER);
            if (params != null) {
                for (Object param : params) {
                    setPSObject(cs, oi++, DataUtil.getDBObject(param));
                }
            }
            cs.execute();

            StoredProcedureResults results = new StoredProcedureResults();
            while (cs.getMoreResults() || cs.getUpdateCount() != -1) {
                ResultSet rs = cs.getResultSet();
                if (rs != null) {
                    if (results.resultSets == null) {
                        results.resultSets = new ArrayList<>();
                    }
                    results.resultSets.add(DataUtil.getRowsFromResultSet(rs));
                }
            }
            results.returnValue = cs.getInt(1);
            return results;
        }
    }

    private void startTransaction() throws SQLException {
        if (transactionDepth == 0) {
            connection.setAutoCommit(false);
        }
        transactionDepth++;
    }

    private void commitTransaction() throws SQLException {
        if (transactionDepth == 1) {
            // no try/catch - if the commit fails, it should rollback which will
            // reset the autocommit flag and transaction depth
            connection.commit();
            connection.setAutoCommit(true);
            transactionDepth--;
        } else if (transactionDepth == 0) {
            throw new SQLException("Transaction depth passed on commit");
        } else {
            transactionDepth--;
        }
    }

    private void rollbackTransaction() throws SQLException {
        if (transactionDepth == 1) {
            try {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
                // if setting the autocommit fails after failing to rollback, 
                // we really should just exit the application because it's totally screwed most likely...
                transactionDepth--;
            }
        } else if (transactionDepth == 0) {
            throw new SQLException("Transaction depth passed on rollback");
        } else {
            transactionDepth--;
        }
    }

    @Override
    public <T, E extends Exception> T inTransaction(WithConnection<T, E> trans) throws SQLException, E {
        try {
            startTransaction();
            T retval = trans.with(this);
            commitTransaction();
            return retval;
        } catch (Throwable th) {
            logger.error("Error in transaction, rolling back", th);
            rollbackTransaction();
            throw th;
        }
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    @Override
    public DBMetaData getTableMetaData(String tableName) throws SQLException {
        Statement st = null;
        try {
            st = connection.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM " + tableName + " WHERE 1 = 2");
            return new DBMetaData(rs.getMetaData());
        } finally {
            if (st != null) {
                st.close();
            }
        }
    }

    @Override
    public DBFKData getForeignKeyData(String tableName) throws SQLException {
        try (ResultSet rs = connection.getMetaData().getImportedKeys(null, null, tableName)) {
            return new DBFKData(rs);
        }
    }

    private void setPSObject(PreparedStatement ps, int i, Object object) throws SQLException {
        if (object != null && object instanceof DBInputStreamWrapper) {
            DBInputStreamWrapper wrap = (DBInputStreamWrapper) object;
            ps.setBinaryStream(i, wrap.inputStream, wrap.length);
        } else {
            ps.setObject(i, object);
        }
    }
}
