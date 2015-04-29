package org.voovan.test.json;

import org.voovan.test.TestObject;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

public class JSONTest {
	public static void main(String[] args) {
		TestObject testObject = new TestObject();
		testObject.setString("helyho");
		testObject.setBint(32);
		testObject.getList().add("listitem1");
		testObject.getList().add("listitem2 ");
		testObject.getList().add("listitem3");
		testObject.getMap().put("mapitem1", "mapitem1");
		testObject.getMap().put("mapitem2", "mapitem2");
		testObject.getTb2().setString("bingo");
		testObject.getTb2().setBint(56);
		testObject.getTb2().getList().add("tb2 list item");
		testObject.getTb2().getMap().put("tb2 map item", "tb2 map item");
		
		long start = System.currentTimeMillis();
		String jsonString = JSON.toJSON(testObject);
		Logger.simple("----Serial Time:"+(System.currentTimeMillis()-start));
		Logger.simple(jsonString);
		
		start = System.currentTimeMillis();
		Object object = JSON.parse(jsonString);
		Logger.simple("----Deserial Time:"+(System.currentTimeMillis()-start));
		Logger.simple(object);
		
		start = System.currentTimeMillis();
		TestObject tObject = JSON.toObject(jsonString,TestObject.class);
		Logger.simple("----Deserial Object Time:"+(System.currentTimeMillis()-start));
		Logger.simple(tObject);
	}
}
