package org.hocate.test.db;

import org.hocate.db.SQLBuilder;
import org.hocate.log.Logger;

public class SQLBuilderTest {
	public static void main(String[] args) {
		SQLBuilder builder = SQLBuilder.createSQLBuilder();
		
		String select = builder.select()
				.column("col_1").column("col_2").from().table("table_d").table("table_e")
				.where().and(" col_1='col1'").or("col_2='col2'")
				.group().by("col_1").having().begin("sum(col_1)>10").order().by("col_2").finish();
		
		Logger.simple(select);
		
		String delete = builder.delete().table("table_a").table("table_b").where().and("col1='1212'").finish();
		Logger.simple(delete);
		
		String update = builder.update().table("table_a").set("col_1", "adfadf").set("col_2", 1001).where().and("col_2=1000").finish();
		Logger.simple(update);
		
		String insert = builder.insert().table("table_a").column("col_1").column("col_2").Values().value("23123123").value(10001).finish();
		Logger.simple(insert);
	}
}
