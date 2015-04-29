package org.voovan.db;
import java.util.List;

import org.voovan.db.sqlsegment.*;
import org.voovan.tools.TObject;


public class SQLBuilder {
	private SelectColumns selectColumns;
	private InsertColumns InsertColumns;
	private InsertTable insertTable;
	private UpdateTable updateTable;
	private From from;
	private Where where;
	private Group group;
	private Having having;
	private Order order;
	private Sets sets;
	private Values values;
	private SQLType type;
	
	public enum SQLType{
		SELECT,DELETE,UPDATE,INSERT
	}
	
	private void reset(){
		 selectColumns = null;
		 InsertColumns = null;
		 insertTable = null;
		 updateTable = null;
		 from = null;
		 where = null;
		 group = null;
		 having = null;
		 order = null;
		 sets = null;
		 values = null;
	}
	
	public static SQLBuilder createSQLBuilder(){
		return new SQLBuilder();
	}
	
	public SelectColumns select(){
		reset();
		type = SQLType.SELECT;
		SelectColumns columns = new SelectColumns(this);
		this.setSelectColumns(columns);
		return columns;
	}
	
	public From delete(){
		reset();
		type = SQLType.DELETE;
		From from = new From(this);
		this.setFrom(from);
		return from;
	}
	
	public UpdateTable update(){
		reset();
		type = SQLType.UPDATE;
		UpdateTable UpdateTable = new UpdateTable(this);
		this.setUpdateTable(UpdateTable);
		return UpdateTable;
	}
	
	public InsertTable insert(){
		reset();
		type = SQLType.INSERT;
		InsertTable insertTable = new InsertTable(this);
		this.setInsertTable(insertTable);
		return insertTable;
	}
	
	public static String listToStr(List<String> items,String split){
		String itemsStr = "";
		for(String item : items){
			itemsStr += item+split;
		}
		return itemsStr.substring(0,itemsStr.length()-1);
	}
	
	public String toSQL(){
		String sql="";
		if(this.type == SQLType.SELECT){
			sql+="select \r\n";
			sql+=this.getSelectColumns()+" \r\n";
			sql+=this.getFrom()+" \r\n";
			sql+=TObject.nullDefault(this.getWhere(),"")+" \r\n";
			sql+=TObject.nullDefault(this.getGroup(),"")+" \r\n";
			sql+=TObject.nullDefault(this.getHaving(),"")+" \r\n";
			sql+=TObject.nullDefault(this.getOrder(),"")+" \r\n";
		}
		else if(this.type == SQLType.DELETE){
			sql+="delete \r\n";
			sql+=this.getFrom()+" \r\n";
			sql+=TObject.nullDefault(this.getWhere(),"")+" \r\n";
			sql+=TObject.nullDefault(this.getGroup(),"")+" \r\n";
			sql+=TObject.nullDefault(this.getHaving(),"")+" \r\n";
			sql+=TObject.nullDefault(this.getOrder(),"")+" \r\n";
			
		}
		else if(this.type == SQLType.UPDATE){
			sql+="update \r\n";
			sql+=this.getUpdateTable()+" \r\nset ";
			sql+=this.getSets()+" \r\n";
			sql+=this.getWhere()+" \r\n";
			
			
		}
		else if(this.type == SQLType.INSERT){
			sql+="insert into ";
			sql+=this.getInsertTable()+" \r\n";
			sql+=this.getInsertColumns()+" \r\n";
			sql+=this.getValues() + " \r\n";
		}
		return sql;
	}

	public SQLType getType() {
		return type;
	}

	public void setType(SQLType type) {
		this.type = type;
	}

	public SelectColumns getSelectColumns() {
		return selectColumns;
	}

	public void setSelectColumns(SelectColumns selectColumns) {
		this.selectColumns = selectColumns;
	}

	public InsertColumns getInsertColumns() {
		return InsertColumns;
	}

	public void setInsertColumns(InsertColumns insertColumns) {
		InsertColumns = insertColumns;
	}

	public InsertTable getInsertTable() {
		return insertTable;
	}

	public void setInsertTable(InsertTable insertTable) {
		this.insertTable = insertTable;
	}

	public UpdateTable getUpdateTable() {
		return updateTable;
	}

	public void setUpdateTable(UpdateTable updateTable) {
		this.updateTable = updateTable;
	}

	public From getFrom() {
		return from;
	}

	public void setFrom(From from) {
		this.from = from;
	}

	public Where getWhere() {
		return where;
	}

	public void setWhere(Where where) {
		this.where = where;
	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Having getHaving() {
		return having;
	}

	public void setHaving(Having having) {
		this.having = having;
	}

	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}

	public Sets getSets() {
		return sets;
	}

	public void setSets(Sets sets) {
		this.sets = sets;
	}

	public Values getValues() {
		return values;
	}

	public void setValues(Values values) {
		this.values = values;
	}
}
