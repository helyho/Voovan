package org.hocate.db.sqlSegment;

import java.util.Vector;

import org.hocate.db.SQLBuilder;

public class Having extends SQLSegment{
	private Vector<String> conditions;
	
	public Having(SQLBuilder builder){
		super(builder);
		conditions = new Vector<String>();
	}
	
	public String finish(){
		return builder.toSQL();
	}
	
	public Having begin(String condition){
		conditions.add(condition);
		return this;
	}
	
	public Having and(String condition){
		if(conditions.size()==0)
			begin(condition);
		else
			conditions.add("and "+condition);
		return this;
	}
	
	public Having or(String condition){
		if(conditions.size()==0)
			begin(condition);
		else
			conditions.add("or "+condition);
		return this;
	}
	
	public Order order(){
		builder.setOrder(new Order(builder));
		return builder.getOrder();
	}
	
	public String toString(){
		return "having "+SQLBuilder.listToStr(conditions, ",");
	}
}
