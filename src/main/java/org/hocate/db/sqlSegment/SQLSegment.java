package org.hocate.db.sqlSegment;

import org.hocate.db.SQLBuilder;

public class SQLSegment {
	protected SQLBuilder builder;
	public SQLSegment(SQLBuilder builder){
		this.builder = builder;
	}
}
