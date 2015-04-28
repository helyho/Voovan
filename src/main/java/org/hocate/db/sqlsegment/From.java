package org.hocate.db.sqlsegment;
import java.util.Vector;

import org.hocate.db.SQLBuilder;


public class From extends SQLSegment {
	private Vector<String> tables;
	
	public From(SQLBuilder builder){
		super(builder);
		tables = new Vector<String>();
	}
	
	public From table(String table){
		tables.add(table);
		return this;
	}
	
	public Where where(){
		builder.setWhere(new Where(builder));
		return builder.getWhere();
	}

	public String finish(){
		return builder.toSQL();
	}
	
	public String toString(){
		return "from "+SQLBuilder.listToStr(tables,",");
	}
}
