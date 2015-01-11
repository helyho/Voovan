package org.hocate.db.sqlSegment;

import org.hocate.db.SQLBuilder;

public class InsertTable extends SingleTable {
	public InsertTable(SQLBuilder builder) {
		super(builder);
	}

	public InsertColumns table(String table){
		this.table = table;
		builder.setInsertColumns(new InsertColumns(builder));
		return builder.getInsertColumns();
	}
}
