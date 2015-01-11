package org.hocate.db.sqlSegment;
import java.util.Vector;

import org.hocate.db.SQLBuilder;
import org.hocate.tools.TSQL;


public class Values extends SQLSegment {
	public Vector<String> values;
	public Values(SQLBuilder builder){
		super(builder);
		values = new Vector<String>();
	}
	
	public  Values value(Object value){
		values.add(TSQL.getSQLString(value));
		return this;
	}
	
	public String finish(){
		return builder.toSQL();
	}
	
	public String toString(){
		return "values ("+SQLBuilder.listToStr(values, ",")+")";
	}
}
