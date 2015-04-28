package org.hocate.db.sqlsegment;

import org.hocate.db.SQLBuilder;

public class InsertColumns  extends Columns{
	
	
	public InsertColumns(SQLBuilder builder) {
		super(builder);
	}

	public InsertColumns column(String column){
		columns.add(column);
		return this;
	}
	
	public Values Values(){
		builder.setValues(new Values(builder));
		return builder.getValues();
	}
	
	public String toString(){
		return "("+super.toString()+")";
	}
}
