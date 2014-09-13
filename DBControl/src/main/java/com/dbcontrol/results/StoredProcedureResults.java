package com.dbcontrol.results;

import java.util.List;

/**
 * Return value and resultsets of a stored procedure call.
 * @author Derek Mulvihill - Oct 3, 2013
 */
public class StoredProcedureResults {
	public Integer returnValue;
	public List<List<DBRow>> resultSets;
}