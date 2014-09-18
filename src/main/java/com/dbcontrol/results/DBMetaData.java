package com.dbcontrol.results;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Metadata about the fields from a query.
 * @author Derek Mulvihill - Aug 2, 2013
 */
public class DBMetaData {
	private final Map<String, DBFieldData> fieldData;

	public DBMetaData(ResultSetMetaData metaData) throws SQLException {
		Map<String, DBFieldData> fieldData = new HashMap<String, DBFieldData>();
		for (int i = 1; i <= metaData.getColumnCount(); i++) {
			DBFieldData field = new DBFieldData();
			field.number = i;
			field.casedName = metaData.getColumnLabel(i);
			field.name = field.casedName.toLowerCase();
			field.type = DBFieldType.getTypeFromJDBCType(metaData
					.getColumnType(i));
			field.precision = metaData.getPrecision(i);
			fieldData.put(field.name, field);
		}
		this.fieldData = Collections.unmodifiableMap(fieldData);
	}

	/**
	 * An unmodifiable Map from the field names to DBFieldData describing those fields.
	 */
	public Map<String, DBFieldData> getFieldData() {
		return fieldData;
	}

	/**
	 * Data about a specific column/field returned from a query.
	 * @author Derek Mulvihill - Aug 2, 2013
	 */
	public static class DBFieldData {
		private int number;
		private String name;
		private String casedName;
		private DBFieldType type;
		private int precision;

		private DBFieldData() {
		}

		/**
		 * The number of the column in the JDBC ResultSet. The first column is number 1.
		 */
		public int getColumnNumber() {
			return number;
		}

		/**
		 * The field name to be referenced in the code (if there was a SQL 'AS' clause, this will be that name).
		 */
		public String getName() {
			return name;
		}

		/**
		 * The same field name as {@link #getName()} without being converted to lower case.
		 */
		public String getCasedName() {
			return casedName;
		}

		/**
		 * The type of data returned by the query for this field.
		 */
		public DBFieldType getType() {
			return type;
		}

		/**
		 * For character fields, the maximum number of characters in the field.<br>
		 * For other fields, it's mostly just a size of the output. 
		 */
		public int getPrecision() {
			return precision;
		}
	}

	/**
	 * Enumeration of the JDBC types supported instead of relying on integers.
	 * @author Derek Mulvihill - Aug 6, 2013
	 */
	public enum DBFieldType {
		//Strings
		CHAR,
		VARCHAR,
		NVARCHAR,
		CLOB,
		//Longs
		LONG,
		//Integers
		INT,
		SHORT,
		//Floats
		FLOAT,
		//Doubles
		DOUBLE,
		//BigDecimals
		DECIMAL,
		//java.sql.Date (modified to jodatime LocalDate)
		DATE,
		//java.sql.Timestamp (modified to jodatime LocalDateTime)
		DATETIME,
		//Booleans
		BOOL,
		//byte[]
		BINARY,
		//
		;

		public static DBFieldType getTypeFromJDBCType(int columnType) {
			switch (columnType) {
			case Types.CHAR:
				return CHAR;
			case Types.VARCHAR:
				return VARCHAR;
			case Types.NVARCHAR:
				return NVARCHAR;
			case Types.CLOB:
				return CLOB;
			case Types.BIGINT:
				return LONG;
			case Types.INTEGER:
				return INT;
			case Types.TINYINT:
				return SHORT;
			case Types.FLOAT:
				return FLOAT;
			case Types.DOUBLE:
				return DOUBLE;
			case Types.NUMERIC:
			case Types.DECIMAL:
				return DECIMAL;
			case Types.DATE:
				return DATE;
			case Types.TIMESTAMP:
				return DATETIME;
			case Types.BIT:
				return BOOL;
			case Types.BINARY:
				return BINARY;
			}
			throw new RuntimeException("Unknown JDBC column type: "
					+ columnType);
		}
	}
}
