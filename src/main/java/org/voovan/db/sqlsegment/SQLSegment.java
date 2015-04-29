package org.voovan.db.sqlsegment;

import org.voovan.db.SQLBuilder;

public class SQLSegment {
	protected SQLBuilder builder;
	public SQLSegment(SQLBuilder builder){
		this.builder = builder;
	}
}
