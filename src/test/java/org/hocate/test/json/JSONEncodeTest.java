package org.hocate.test.json;

import java.util.Map;

import org.hocate.json.JSONDecode;
import org.hocate.json.JSONEncode;
import org.hocate.test.TestObject;
import org.hocate.tools.TReflect;

public class JSONEncodeTest {
	
	public static void main(String[] args) throws Exception {
		TestObject testObject = new TestObject();
		testObject.setString("helyho");
		testObject.setBint(32);
		testObject.getList().add("listitem1");
		testObject.getList().add("listitem2");
		testObject.getList().add("listitem3");
		testObject.getMap().put("mapitem1", "mapitem1");
		testObject.getMap().put("mapitem2", "mapitem2");
		testObject.getTb2().setString("bingo");
		testObject.getTb2().setBint(56);
		testObject.getTb2().getList().add("tb2 list item");
		testObject.getTb2().getMap().put("tb2 map item", "tb2 map item");
		String x = JSONEncode.fromObject(testObject);
		System.out.println("JSON Str:"+x);
		@SuppressWarnings("unchecked")
		Map<String, Object> obj = (Map<String, Object>) JSONDecode.parse(x);
		System.out.println("Object"+obj);
		System.out.println(TReflect.getMapfromObject(testObject.getMap()));
	}
}
