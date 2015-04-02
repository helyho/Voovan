package org.hocate.test.json;

import java.util.Map;

import org.hocate.tools.json.JSONDecode;
import org.hocate.tools.log.Logger;
import org.hocate.test.TestObject;

public class JSONDecodeTest {
	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String jsonString = "/*asdfasdf*/\r\n{\"bint\":32,\"string\":\"helyho\",\"tb2\":{\"bint\":56,\"string\":\"bingo\",\"list\":[\"tb2 list item\"],\"map\":{\"tb2 map item\":\"tb2 map item\"}},\"list\":[\"listitem1\",\"listitem2\",\"listitem3\"],\"map\":{\"mapitem2\":\"mapitem2\",\"mapitem1\":\"mapitem1\"}}";
		@SuppressWarnings("unchecked")
		Map<String, Object> obj = (Map<String, Object>) JSONDecode
				.parse(jsonString);
		Logger.simple(obj);
		Object object = JSONDecode.fromJSON(jsonString, TestObject.class);
		Logger.simple(object);
	}
}
