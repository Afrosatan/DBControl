package com.dbcontrol.results;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata about the foreign keys for a table.
 * @author Derek Mulvihill - Sep 17, 2014
 */
public class DBFKData {
	private final Map<String, DBFKFieldData> fieldFK;

	public DBFKData(ResultSet rs) throws SQLException {
		Map<String, DBFKFieldData> fieldFK = new HashMap<String, DBFKFieldData>();
		while (rs.next()) {
			DBFKFieldData fkFieldData = new DBFKFieldData(
					rs.getString("PKTABLE_NAME"),
					rs.getString("PKCOLUMN_NAME"),
					rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"));
			fieldFK.put(fkFieldData.getForeignKeyColumnName().toLowerCase(),
					fkFieldData);
		}
		this.fieldFK = Collections.unmodifiableMap(fieldFK);
	}

	public Map<String, DBFKFieldData> getFKData() {
		return fieldFK;
	}

	public static class DBFKFieldData {
		private String pkTableName;
		private String pkColumnName;
		private String fkTableName;
		private String fkColumnName;

		private DBFKFieldData(String pkTableName, String pkColumnName,
				String fkTableName, String fkColumnName) {
			this.pkTableName = pkTableName;
			this.pkColumnName = pkColumnName;
			this.fkTableName = fkTableName;
			this.fkColumnName = fkColumnName;
		}

		public String getPrimaryKeyTableName() {
			return pkTableName;
		}

		public String getPrimaryKeyColumnName() {
			return pkColumnName;
		}

		public String getForeignKeyTableName() {
			return fkTableName;
		}

		public String getForeignKeyColumnName() {
			return fkColumnName;
		}
	}
}
