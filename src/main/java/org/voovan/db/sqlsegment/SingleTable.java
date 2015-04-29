package org.voovan.db.sqlsegment;

import org.voovan.db.SQLBuilder;

public class SingleTable extends SQLSegment {
	protected String table;
	
	public SingleTable(SQLBuilder builder){
		super(builder);
		table = "";
	}
	
	public String toString(){
		return table;
	}
}
