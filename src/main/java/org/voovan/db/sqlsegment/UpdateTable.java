package org.voovan.db.sqlsegment;

import org.voovan.db.SQLBuilder;

public class UpdateTable extends SingleTable {
	public UpdateTable(SQLBuilder builder) {
		super(builder);
	}

	public Sets table(String table){
		this.table = table;
		builder.setSets(new Sets(builder));
		return builder.getSets();
	}
}
