package org.hocate.db.sqlsegment;
import java.util.Vector;

import org.hocate.db.SQLBuilder;


public class Columns extends SQLSegment {
	protected Vector<String> columns;
	
	public Columns(SQLBuilder builder){
		super(builder);
		columns = new Vector<String>();
	}

	public String toString(){
		return SQLBuilder.listToStr(columns,",");
	}
	
}
