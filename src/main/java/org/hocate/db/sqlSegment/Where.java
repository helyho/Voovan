package org.hocate.db.sqlSegment;
import java.util.ArrayList;
import java.util.List;

import org.hocate.db.SQLBuilder;
import org.hocate.tools.TSQL;


public class Where extends SQLSegment {
	private ArrayList<String> conditions;
	
	public Where(SQLBuilder builder){
		super(builder);
		conditions = new ArrayList<String>();
	}
	
	public Where begin(String condition){
		conditions.add(condition);
		return this;
	}
	
	public Where and(String condition){
		if(conditions.isEmpty()){
			begin(condition);
		}
		else{
			conditions.add("and "+condition);
		}
		return this;
	}
	
	public Where or(String condition){
		if(conditions.isEmpty()){
			begin(condition);
		}
		else{
			conditions.add("or "+condition);
		}
		return this;
	}
	
	public Where and(){
		if(!conditions.isEmpty()){
			conditions.add("and ");
		}
		return this;
	}
	
	public Where or(){
		if(!conditions.isEmpty()){
			conditions.add("or ");
		}
		return this;
	}
	
	public Where in(String column,List<String> inItems){
		String inStr = column+" in (";
		for(String inItem : inItems){
			inStr+= TSQL.getSQLString(inItem)+",";
		}
		inStr = inStr.substring(0,inStr.length()-1)+")";
		conditions.add(inStr);
		return this;
	}
	
	public Where exists(String column,SQLBuilder sqlBuilder){
		String inStr = column+" exists ("+sqlBuilder.toString()+")";
		conditions.add(inStr);
		return this;
	}
	
	public String finish(){
		return builder.toSQL();
	}
	
	public Group group(){
		builder.setGroup(new Group(builder));
		return builder.getGroup();
	}
	
	public String toString(){
		return "where "+SQLBuilder.listToStr(conditions," ");
	}
	
}
