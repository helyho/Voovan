package org.hocate.test.db;

import org.hocate.db.SQLBuilder;

public class SQLBuilderTest {
	public static void main(String[] args) {
		SQLBuilder builder = SQLBuilder.createSQLBuilder();
		
		String select = builder.select()
				.column("col_1").column("col_2").from().table("table_d").table("table_e")
				.where().and(" col_1='col1'").or("col_2='col2'")
				.group().by("col_1").having().begin("sum(col_1)>10").order().by("col_2").finish();
		
		System.out.println(select);
		
		String delete = builder.delete().table("table_a").table("table_b").where().and("col1='1212'").finish();
		System.out.println(delete);
		
		String update = builder.update().table("table_a").set("col_1", "adfadf").set("col_2", 1001).where().and("col_2=1000").finish();
		System.out.println(update);
		
		String insert = builder.insert().table("table_a").column("col_1").column("col_2").Values().value("23123123").value(10001).finish();
		System.out.println(insert);
	}
}
