package com.dbcontrol.results;

import java.io.InputStream;

/**
 * Wrapper around an InputStream with a specified length use as a field value in DBControl/ConnectionWrapper.
 * @author Derek Mulvihill - Jun 21, 2014
 */
public class DBInputStreamWrapper {
	public final InputStream inputStream;
	public final int length;

	public DBInputStreamWrapper(InputStream inputStream, int length) {
		this.inputStream = inputStream;
		this.length = length;
	}
}
