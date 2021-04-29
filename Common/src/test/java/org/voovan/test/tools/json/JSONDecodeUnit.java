package org.voovan.test.tools.json;

import junit.framework.TestCase;
import org.voovan.tools.json.JSONDecode;

import java.util.List;
import java.util.Map;

public class JSONDecodeUnit extends TestCase {

	public JSONDecodeUnit(String name) {
		super(name);
	}

	@SuppressWarnings("rawtypes")
	public void testRun() throws Exception{
		String jsonString = "/*asdfasdf*/"+
							" "+
								"\"bint\":32,"+
								"\"string\":\"helyho\","+
								"\"tb2\":{"+
									"\"bint\":56,"+
									"\"string\":\"bi\\\"ngo\","+
									"\"list\":["+
										"\"tb2 list\n item\""+
									"],"+
									"\"map\":{"+
										"\"tb2 map item\":\"tb2 map item\""+
									"}"+
								"},"+
								"\"list\":["+
									"\"listitem1\","+
									"\"listitem2\","+
									"\"listitem3\""+
								"],"+
								"\"map\":{"+
									"\"mapitem2\":\"mapitem2\","+
									"\"mapitem1\":\"mapitem1\""+
								"} " +
							" ";


		Map<String, Object> obj = (Map<String, Object>)JSONDecode.parse(jsonString);
		assertTrue((Integer)obj.size()==5);
		assertTrue((Integer)obj.get("bint")==32);
		assertEquals((String)obj.get("string"),"helyho");
		assertTrue(((List)obj.get("list")).size() == 3);
		assertTrue(((Map)obj.get("map")).size() == 2);
		Map<String, Object> tb2 = (Map<String, Object>)obj.get("tb2");
		assertTrue((Integer)tb2.get("bint")==56);
		assertEquals((String)tb2.get("string"),"bi\\\"ngo");
		assertTrue(((List)tb2.get("list")).size() == 1);
		assertTrue(((Map)tb2.get("map")).size() == 1);

		TestObject object = JSONDecode.fromJSON(jsonString, TestObject.class);
		assertTrue(object.getBint()==32);
		assertEquals(object.getString(),"helyho");
		assertTrue(object.getList().size() == 3);
		assertTrue(object.getMap().size() == 2);
		assertTrue(object.getTb2().getBint()==56);
		assertEquals(object.getTb2().getString(),"bi\\\"ngo");
		assertTrue(object.getTb2().getList().size() == 1);
		assertTrue(object.getTb2().getMap().size() == 1);

	}

}
