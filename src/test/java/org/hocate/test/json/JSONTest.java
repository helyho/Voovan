package org.hocate.test.json;

import org.hocate.json.JSON;
import org.hocate.test.TestObject;

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
		System.out.println("----Serial Time:"+(System.currentTimeMillis()-start));
		System.out.println(jsonString);
		
		start = System.currentTimeMillis();
		Object object = JSON.parse(jsonString);
		System.out.println("----Deserial Time:"+(System.currentTimeMillis()-start));
		System.out.println(object);
		
		start = System.currentTimeMillis();
		TestObject tObject = JSON.toObject(jsonString,TestObject.class);
		System.out.println("----Deserial Object Time:"+(System.currentTimeMillis()-start));
		System.out.println(tObject);
	}
}
