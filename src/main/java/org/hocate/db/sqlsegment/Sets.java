package org.hocate.db.sqlsegment;
import java.util.Vector;

import org.hocate.db.SQLBuilder;
import org.hocate.tools.TSQL;


public class Sets extends SQLSegment {
	public Vector<String> sets;
	public Sets(SQLBuilder builder){
		super(builder);
		sets = new Vector<String>();
	}
	
	public  Sets set(String column,Object value){
		sets.add(column+"="+TSQL.getSQLString(value));
		return this;
	}
	
	public Where where(){
		builder.setWhere(new Where(builder));
		return builder.getWhere();
	}
	
	public String toString(){
		return SQLBuilder.listToStr(sets, ",");
	}
}
