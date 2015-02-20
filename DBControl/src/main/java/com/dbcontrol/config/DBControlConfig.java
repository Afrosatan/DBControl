package com.dbcontrol.config;

import com.dbcontrol.config.cancel.MSSQLCancellationDetector;
import com.dbcontrol.config.cancel.PostgresSQLCancellationDetector;
import com.dbcontrol.config.cancel.SQLCancellationDetector;

/**
 * Configurations for initializing a DBControl.
 * @author Derek Mulvihill - Aug 27, 2014
 */
public class DBControlConfig {
	private String url;
	private String username;
	private String password;
	private String driverClass;
	private String testQuery;
	private Integer idleConnectionTestSeconds;
	private SQLCancellationDetector sqlCancellationDetector;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClass() {
		return driverClass;
	}

	public void setDriverClass(String driverClass) {
		this.driverClass = driverClass;
	}

	public String getTestQuery() {
		return testQuery;
	}

	public void setTestQuery(String testQuery) {
		this.testQuery = testQuery;
	}

	public Integer getIdleConnectionTestSeconds() {
		return idleConnectionTestSeconds;
	}

	public void setIdleConnectionTestSeconds(Integer idleConnectionTestSeconds) {
		this.idleConnectionTestSeconds = idleConnectionTestSeconds;
	}

	public SQLCancellationDetector getSQLCancellationDetector() {
		return sqlCancellationDetector;
	}

	public void setSQLCancellationDetector(
			SQLCancellationDetector sqlCancellationDetector) {
		this.sqlCancellationDetector = sqlCancellationDetector;
	}

	/**
	 * Create a DBControl with defaults for microsoft sql server.
	 */
	public static DBControlConfig msSql() {
		DBControlConfig config = new DBControlConfig();
		config.setDriverClass("net.sourceforge.jtds.jdbc.Driver");
		config.setTestQuery("SELECT 1");
		config.setIdleConnectionTestSeconds(60);
		config.setSQLCancellationDetector(new MSSQLCancellationDetector());
		return config;
	}

	/**
	 * Create a DBControl with defaults for an embedded Apache derby instance that maps to a local folder.<br>
	 * NOTE: You can pass in a path like "test/dbone" and it will create the database in that directory hierarchy.<br>
	 * NOTE: You can also pass absolute path starting like "/test/dbtwo" and it will be relative to the root drive.<br>
	 * NOTE: You can also pass a different drive like "g:/test/dbthree" and it will be on the g drive.<br>
	 * NOTE: You can also ignore path and use a memory only database like "memory:dbfour" - the database will not be persisted.<br>
	 * NOTE: You can also connect to a READ-ONLY database on the classpath like "classpath:dbfive" - this requires some set up.<br> 
	 * @param databaseName The name you want to use for the database
	 */
	public static DBControlConfig derby(String databaseName) {
		DBControlConfig config = new DBControlConfig();
		config.setUrl("jdbc:derby:" + databaseName + ";");
		config.setDriverClass("org.apache.derby.jdbc.EmbeddedDriver");
		config.setTestQuery("SELECT 1 FROM SYSIBM.SYSDUMMY1");
		config.setIdleConnectionTestSeconds(60);
		return config;
	}

	public static DBControlConfig postgres() {
		DBControlConfig config = new DBControlConfig();
		config.setDriverClass("org.postgresql.Driver");
		config.setTestQuery("SELECT 1");
		config.setSQLCancellationDetector(new PostgresSQLCancellationDetector());
		return config;
	}
}
