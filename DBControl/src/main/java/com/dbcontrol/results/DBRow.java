package com.dbcontrol.results;

import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.dbcontrol.exceptions.InvalidFieldException;
import com.dbcontrol.results.DBMetaData.DBFieldData;
import com.dbcontrol.results.DBMetaData.DBFieldType;

/**
 * A single row of results from a query, or a record from a database table.
 * @author Derek Mulvihill - Aug 1, 2013
 */
public class DBRow {
	private DBMetaData metadata = null;
	private Map<String, Object> values = new HashMap<>();

	public DBRow(DBMetaData dbm, ResultSet rs) throws SQLException {
		this.metadata = dbm;
		if (rs != null) {
			for (DBFieldData field : dbm.getFieldData().values()) {
				if (field.getType() == DBFieldType.DATE) {
					java.sql.Date value = (java.sql.Date) rs.getObject(field
							.getColumnNumber());
					if (value == null) {
						values.put(field.getName(), null);
					} else {
						values.put(field.getName(),
								new LocalDate(value.getTime()));
					}
				} else if (field.getType() == DBFieldType.DATETIME) {
					Timestamp value = (Timestamp) rs.getObject(field
							.getColumnNumber());
					if (value == null) {
						values.put(field.getName(), null);
					} else {
						values.put(field.getName(),
								new LocalDateTime(value.getTime()));
					}
				} else if (field.getType() == DBFieldType.CLOB) {
					Clob value = (Clob) rs.getObject(field.getColumnNumber());
					if (value == null) {
						values.put(field.getName(), null);
					} else {
						values.put(field.getName(),
								value.getSubString(1, (int) value.length()));
					}
				} else {
					values.put(field.getName(),
							rs.getObject(field.getColumnNumber()));
				}
			}
		} else {
			for (DBFieldData field : dbm.getFieldData().values()) {
				values.put(field.getName(), null);
			}
		}
	}

	/**
	 * Return the value from the corresponding getXXX function depending on the type of the given field.
	 */
	public Object getObject(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case CHAR:
		case VARCHAR:
		case NVARCHAR:
		case CLOB:
			return getString(field);
		case LONG:
			return getLong(field);
		case INT:
		case SHORT:
			return getInt(field);
		case FLOAT:
			return getFloat(field);
		case DOUBLE:
			return getDouble(field);
		case DECIMAL:
			return getDecimal(field);
		case DATE:
			return getDate(field);
		case DATETIME:
			return getDateTime(field);
		case BOOL:
			return getBool(field);
		case BINARY:
			return getBytes(field);
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a known representation");
		}
	}

	/**
	 * Change a key-value pair for this row. The class of value will be checked.
	 */
	public void setObject(String field, Object value) {
		DBFieldData data = getFieldData(field);
		if (value != null) {
			boolean correctType;
			switch (data.getType()) {
			case CHAR:
			case VARCHAR:
			case NVARCHAR:
			case CLOB:
				correctType = value instanceof String;
				break;
			case LONG:
				correctType = value instanceof Long;
				break;
			case INT:
			case SHORT:
				correctType = value instanceof Integer;
				break;
			case FLOAT:
				correctType = value instanceof Float;
				break;
			case DOUBLE:
				correctType = value instanceof Double;
				break;
			case DECIMAL:
				correctType = value instanceof BigDecimal;
				break;
			case DATE:
				correctType = value instanceof LocalDate;
				break;
			case DATETIME:
				correctType = value instanceof LocalDateTime;
				break;
			case BOOL:
				correctType = value instanceof Boolean;
				break;
			case BINARY:
				correctType = value instanceof byte[];
				break;
			default:
				correctType = false;
				break;
			}
			if (!correctType) {
				throw new InvalidFieldException(
						"Invalid field type for field [" + field + "]: "
								+ data.getType() + " "
								+ (value.getClass().getName()));
			}
		}
		values.put(field, value);
	}

	/**
	 * Returns the String value for CHAR, VARCHAR, NVARCHAR, and CLOB field types.
	 */
	public String getString(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case CHAR:
		case VARCHAR:
		case NVARCHAR:
		case CLOB:
			return (String) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a String representation");
		}
	}

	/**
	 * Returns the Long value for the LONG field type.
	 */
	public Long getLong(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case LONG:
			return (Long) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a Long representation");
		}
	}

	/**
	 * Returns the Integer value for the INT and SHORT field types.
	 */
	public Integer getInt(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case INT:
		case SHORT:
			return (Integer) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a Integer representation");
		}
	}

	/**
	 * Returns the Float value for the FLOAT field type.
	 */
	public Float getFloat(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case FLOAT:
			return (Float) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a Float representation");
		}
	}

	/**
	 * Returns the Double value for the DOUBLE field type.
	 */
	public Double getDouble(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case DOUBLE:
			return (Double) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a Double representation");
		}
	}

	/**
	 * Returns the BigDecimal value for the DECIMAL field type.
	 */
	public BigDecimal getDecimal(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case DECIMAL: {
			return (BigDecimal) values.get(data.getName());
		}
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a BigDecimal representation");
		}
	}

	/**
	 * Returns the LocalDate value for the DATE field type.
	 */
	public LocalDate getDate(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case DATE:
			return (LocalDate) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a LocalDate representation");
		}
	}

	/**
	 * Returns the LocalDateTime value for the DATETIME field type.
	 */
	public LocalDateTime getDateTime(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case DATETIME:
			return (LocalDateTime) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a LocalDateTime representation");
		}
	}

	/**
	 * Returns the Boolean value for the BOOL field type.
	 */
	public Boolean getBool(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case BOOL:
			return (Boolean) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a Boolean representation");
		}
	}

	/**
	 * Returns the byte[] value for the BINARY field type.
	 */
	public byte[] getBytes(String field) {
		DBFieldData data = getFieldData(field);
		switch (data.getType()) {
		case BINARY:
			return (byte[]) values.get(data.getName());
		default:
			throw new InvalidFieldException("Field [" + field
					+ "] does not have a byte[] representation");
		}
	}

	/**
	 * Get the DBFieldData instance for the provided field.
	 */
	public DBFieldData getFieldData(String field) {
		DBFieldData data = metadata.getFieldData().get(field.toLowerCase());
		if (data == null) {
			throw new InvalidFieldException("Field [" + field + "] not in row");
		}
		return data;
	}

	/**
	 * Metadata about the fields in the row.
	 */
	public DBMetaData getMetadata() {
		return metadata;
	}

	@Override
	public String toString() {
		return values.toString();
	}
}
