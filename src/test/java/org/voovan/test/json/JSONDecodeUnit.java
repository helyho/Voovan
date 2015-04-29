package org.voovan.test.json;

import java.util.List;
import java.util.Map;

import org.voovan.tools.TObject;
import org.voovan.tools.json.JSONDecode;

import junit.framework.TestCase;

public class JSONDecodeUnit extends TestCase {

	public JSONDecodeUnit(String name) {
		super(name);
	}

	@SuppressWarnings("rawtypes")
	public void testRun() throws Exception{
		String jsonString = "/*asdfasdf*/"+
							"{"+
								"\"bint\":32,"+
								"\"string\":\"helyho\","+
								"\"tb2\":{"+
									"\"bint\":56,"+
									"\"string\":\"bingo\","+
									"\"list\":["+
										"\"tb2 list item\""+
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
								"}"+
							"}";
		Map<String, Object> obj = TObject.cast(JSONDecode.parse(jsonString));
		assert((Integer)obj.get("bint")==32);
		assert((String)obj.get("string")=="helyho");
		assert(((List)obj.get("list")).size() == 3);
		assert(((Map)obj.get("map")).size() == 2);
		Map<String, Object> tb2 = TObject.cast(obj.get("tb2"));
		assert((Integer)tb2.get("bint")==56);
		assert((String)tb2.get("string")=="bingo");
		assert(((List)tb2.get("list")).size() == 1);
		assert(((Map)tb2.get("map")).size() == 1);
		
		TestObject object = JSONDecode.fromJSON(jsonString, TestObject.class);
		assert(object.getBint()==32);
		assert(object.getString()=="helyho");
		assert(object.getList().size() == 3);
		assert(object.getMap().size() == 2);
		assert(object.getTb2().getBint()==56);
		assert(object.getTb2().getString()=="bingo");
		assert(object.getTb2().getList().size() == 1);
		assert(object.getTb2().getMap().size() == 1);

	}

}
