DBControl
=========

A simple JDBC wrapper library.

How To Start
=========

Download/clone the repository, using Maven install to build it locally, and include with Maven in your project:
```xml
<dependency>
	<groupId>com.reiterable</groupId>
	<artifactId>dbcontrol</artifactId>
	<version>1.0.0</version>
</dependency>
```

You will also need to include driver library depending on your database:

Postgres:
```xml
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<version>9.3-1101-jdbc41</version>
</dependency>
```

Apache Derby:
```xml
<dependency>
	<groupId>org.apache.derby</groupId>
	<artifactId>derby</artifactId>
	<version>10.10.1.1</version>
</dependency>
```

Microsoft SQL Server
```xml
<dependency>
	<groupId>net.sourceforge.jtds</groupId>
	<artifactId>jtds</artifactId>
	<version>1.2.8</version>
</dependency>
```

Other databases might need additional attention beyond just including the driver, these are the only three I've had to support with this project.

To start you will have to create a DBControl instance, the project has an implementation C3P0DBControl which uses the C3P0 project for pooling connections:
```
DBControl db = new C3P0DBControl();
```

Before you can start using the DBControl, you will have to call the init method with a DBControlConfig object.

Each database has some differences. There are static methods on DBControlConfig with default configuration settings for some databases (postgres, msSql, derby).

You will probably have to set a URL, username and password on the configuration object:
```
DBControlConfig config = DBControlConfig.postgres();
config.setUrl(DATABASE_URL);
config.setUsername(DATABASE_USERNAME);
config.setPassword(DATABASE_PASSWORD);
db.init(config);
```

Now you're ready to start running sql commands.

How to Use:
=========

The DBControl object exposes two methods: withConnection() and inTransaction() which do the same thing, except inTransaction() will rollback any changes if an Exception is thrown.

Both methods take a WithConnection object, which has a single method that you will need to implement to work with a DBConnection object that is capable of executing SQL.

WithConnection has generic type parameters for a return value and additional exception types. You can also extend WithConnectionClean which returns a null/Void and throws no additional exception types.

DBConnection instances rely on a single JDBC connection without pooling, and the DBConnection will only work within WithConnection.with().

The examples below using Java 8 lambda syntax for WithConnections.

The first thing you can do is a simple query, DBControl reads result sets back in DBRow objects for each result returned:
```
List<DBRow> rows = db.withConnection(connect -> connect.query("SELECT * FROM customer WHERE customer.Id = ?", 1234));
```
Most methods take var arg parameters and when provided use a PreparedStatement. Without parameters it falls back to a normal Statement.  

DBRows have metadata about the fields in the results and the result values mapped by field name. Getting and setting field values is case insensitive.

Methods on DBRow do runtime type checking and throw InvalidFieldExceptions if the wrong method is called for a field of a different type, or if trying to set the wrong type for a field.

```
for(DBRow row : rows) {
	System.out.println(row.getString("name"));
}
```

Update/Insert/Delete statements can be executed directly with the DBConnection.directExecute method:
```
db.withConnection(connect -> connect.directExecute("DELETE FROM customer WHERE customer.Id = ?", 1234));
```

Alternatively, if you have a DBRow object and want to update/insert/delete with a bit more safety and without writing as much SQL, you can use other methods:

Update
```
DBRow row = rows.get(0);
Map<String, Object> fvs = new HashMap<>();
fvs.put("name", "George Orwell");
db.withConnection(connect -> connect.update("customer", row, fvs));
//UPDATE customer SET name = 'George Orwell' WHERE --each field-value pair on the row matches
//Uses PrepareStatement parameters for field values and DBRow values
//If more or less than 1 row is updated, a RowsAffectedSQLException is thrown and the internal transaction rolls back
//NOTE: the DBRow 'row' will be unchanged and not contain the values from fvs
```
Insert
```
DBRow row = new DBRow(db.getTableMetaData("customer"));
Map<String, Object> fvs = new HashMap<>();
fvs.put("name", "George Costanza");
db.withConnection(connect -> connect.insert("customer", "Id", row, fvs));
//INSERT INTO customer (name) VALUES ('George Costanza')
//Uses PreparedStatement parameters for field values
//Also pulls field values from the DBRow (field values overrides DBRow values)
//Returns generated key value
//NOTE: the DBRow 'row' will be unchanged and not contain the values from fvs

//Insert without DBRow instance
Map<String, Object> fvs = new HashMap<>();
fvs.put("name", "George Costanza");
db.withConnection(connect -> connect.directInsert("customer", fvs));
```
Delete
```
DBRow row = rows.get(0);
db.withConnection(connect -> connect.delete("customer", row));
//DELETE FROM customer WHERE -- each field-value pair on the row matches
//If more or less than 1 row is deleted, a RowsAffectedSQLException is thrown and the internal transaction rolls back
```
Stored Procedures:
```
db.withConnection(connect -> {
    StoredProcedureResults results = connect.callStoredProcedure("renameCustomer", 1234, "George Washington");
    System.out.println("Stored Procedure Return Value: " + results.returnValue);
    for(List<DBRow> resultSet : results.resultSets) {
        for(DBRow row : resultSet) {
            System.out.println(row.getString("NameBefore") + " -> " + row.getString("NameAfter"));
        }
    }
});
```

Miscellaneous
=========

This project uses SLF4J for logging. It logs traces for SQL being executed and parameters.

Date and DateTime columns are mapped to Joda-Time LocalDate and LocalDateTime objects.
