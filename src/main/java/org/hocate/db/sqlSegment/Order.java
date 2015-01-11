package org.hocate.db.sqlSegment;
import java.util.Vector;

import org.hocate.db.SQLBuilder;


public class Order extends SQLSegment {
	
	public Vector<String> orderColumns;
	public Order(SQLBuilder builder){
		super(builder);
		orderColumns = new Vector<String>();
	}
	
	public Order by(String column){
		orderColumns.add(column);
		return this;
	}
	
	public String finish(){
		return builder.toSQL();
	}
	
	public String toString(){
		return "order By "+SQLBuilder.listToStr(orderColumns,",");
	}
}
