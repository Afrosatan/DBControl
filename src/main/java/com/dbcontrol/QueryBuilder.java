package com.dbcontrol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper around a simple StringBuilder and an ArrayList for parameters intended
 * to make it easier to build and read Sql reduce code duplication.<br>
 *
 * @author Derek Mulvihill - Oct 18, 2014
 */
public class QueryBuilder {
    private StringBuilder sql = new StringBuilder();
    private List<Object> params = new ArrayList<>();

    public String getSql() {
        return sql.toString();
    }

    public Object[] getParams() {
        return params.toArray(new Object[params.size()]);
    }

    public List<Object> getParamList() {
        return params;
    }

    public QueryBuilder append(String sql, Object... params) {
        this.sql.append(sql);
        Collections.addAll(this.params, params);
        return this;
    }
}
