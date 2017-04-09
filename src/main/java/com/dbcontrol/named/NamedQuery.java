package com.dbcontrol.named;

import com.dbcontrol.DBConnection;
import com.dbcontrol.QueryBuilder;
import com.dbcontrol.handlers.QueryHandler;
import com.dbcontrol.results.DBRow;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object representing a query with named parameters.<br>
 * SQL provided to this class must use # delimited parameters.<br>
 * Eg:<br>
 * <code>SELECT * FROM orders WHERE id = #orderId</code>
 * Parameters can only contain word characters (upper/lowercase A-Z and 0-9).
 *
 * @author Derek Mulvihill - Apr 09, 2017
 */
public class NamedQuery {
    /**
     * Create a NamedQuery instance.
     */
    public static NamedQuery create(String sql) {
        return new NamedQuery(sql);
    }

    private final String sql;
    private final Map<String, Object> parameters = new HashMap<>();

    public NamedQuery(String sql) {
        this.sql = sql;
    }

    public NamedQuery param(String name, Object parameter) {
        parameters.put(name, parameter);
        return this;
    }

    public List<DBRow> query(DBConnection connect) throws SQLException {
        QueryBuilder qb = toQueryBuilder();
        return connect.query(qb.getSql(), qb.getParams());
    }

    public int queryHandle(DBConnection connect, QueryHandler queryHandler) throws SQLException {
        QueryBuilder qb = toQueryBuilder();
        return connect.queryHandle(qb.getSql(), qb.getParams(), queryHandler);
    }

    public int directExecute(DBConnection connect) throws SQLException {
        QueryBuilder qb = toQueryBuilder();
        return connect.directExecute(qb.getSql(), qb.getParams());
    }

    public List<DBRow> alterExecute(DBConnection connect) throws SQLException {
        QueryBuilder qb = toQueryBuilder();
        return connect.alterExecute(qb.getSql(), qb.getParams());
    }

    /**
     * Creates a QueryBuilder object based on the SQL and parameters provided.
     *
     * @throws UnboundNameQueryParameterException If the SQL contains a hash prefixed named parameter, but the parameter hasn't been provided yet.
     */
    public QueryBuilder toQueryBuilder() {
        QueryBuilder qb = new QueryBuilder();
        Pattern p = Pattern.compile("#(\\w+)");
        Matcher m = p.matcher(sql);
        //index of the beginning of the SQL that still needs to be added to the query (beginning of the query or the character after the last parameter name)
        int startIndex = 0;
        while (m.find()) {
            String param = m.group(1);
            if (parameters.containsKey(param)) {
                qb.append(sql.substring(startIndex, m.start()) + "?", parameters.get(param));
            } else {
                throw new UnboundNameQueryParameterException(param);
            }
            startIndex = m.end();
        }
        qb.append(sql.substring(startIndex));
        return qb;
    }
}
