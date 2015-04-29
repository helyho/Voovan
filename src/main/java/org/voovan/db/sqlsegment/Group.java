package org.voovan.db.sqlsegment;
import java.util.Vector;

import org.voovan.db.SQLBuilder;


public class Group extends SQLSegment {
	public Vector<String> groupColumns;
	public Group(SQLBuilder builder){
		super(builder);
		groupColumns = new Vector<String>();
	}
	
	public Group by(String column){
		groupColumns.add(column);
		return this;
	}
	
	public String finish(){
		return builder.toSQL();
	}
	
	public Having having(){
		builder.setHaving(new Having(builder));
		return builder.getHaving();
	}
	
	public String toString(){
		return "group By "+SQLBuilder.listToStr(groupColumns,",");
	}
}
