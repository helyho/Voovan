package org.voovan.test.tools.json;

import junit.framework.TestCase;
import org.voovan.tools.TEnv;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.JSONDecode;
import org.voovan.tools.json.JSONEncode;
import org.voovan.tools.log.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class JSONEncodeUnit extends TestCase {

	public JSONEncodeUnit(String name) {
		super(name);
	}

	public void testRun() throws Exception{
		String targetStr = "{\"bint\":32,\"string\":\"helyho\",\"tb2\":{\"bint\":56,\"string\":\"bingo\\u000d\\u000asrc\\u005cmain\\u005ckkk\",\"list\":[\"tb2 list item\"],\"map\":{\"tb2 map item\":\"tb2 map item\"}},\"list\":[\"listitem1\",\"listitem2\",\"listitem3\"],\"map\":{\"mapitem2\":\"mapitem2\",\"mapitem1\":\"mapitem1\"}}";

//		TEnv.sleep(10000);

		TestObject testObject = new TestObject();
		testObject.setString("helyho");
		testObject.setBint(32);
		testObject.getList().add("listitem1");
		testObject.getList().add("listitem2");
		testObject.getList().add("listitem3");
		testObject.getList().add(null);
		testObject.getList().add(null);
		testObject.getList().add(null);
		testObject.getList().add(null);
		testObject.getList().add(null);
		testObject.getMap().put("mapitem1", "mapitem1");
		testObject.getMap().put("mapitem2", "mapitem2");
		testObject.getTb2().setString("bingo\r\nsrc\\main\\kkk");
		testObject.getTb2().setBint(56);
		testObject.getTb2().getList().add("tb2 list item");
		testObject.getTb2().getMap().put("tb2 map item", "tb2 map item");
		long s = System.currentTimeMillis();
		for(int i=0;i<10000;i++) {
			JSONEncode.fromObject(testObject);
		}
		Logger.simple(System.currentTimeMillis() - s);
		String jsonStr = JSONEncode.fromObject(testObject);
		jsonStr = JSON.removeNullNode(jsonStr);
//		s = System.currentTimeMillis();
//		for(int i=0;i<10000;i++) {
//			JSONDecode.fromJSON(jsonStr, TestObject.class);
//		}
//		Logger.simple(System.currentTimeMillis() - s);
//////		assertEquals(jsonStr,targetStr);
//
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		ObjectOutputStream ot = new ObjectOutputStream(out);
//		byte[] bytes=null;
//		s = System.currentTimeMillis();
//		for(int i=0;i<10000;i++) {
//			ot.writeObject(testObject);
//			if(i==0) {
//				bytes = out.toByteArray();
//			}
//		}
//		Logger.simple(System.currentTimeMillis() - s);
//		ot.flush();
//		ot.close();
//
//		s = System.currentTimeMillis();
//		Object o;
//		for(int i=0;i<10000;i++) {
//			ByteArrayInputStream in = new ByteArrayInputStream(bytes);
//
//			try (ObjectInputStream oi = new ObjectInputStream(in)) {
//				o = oi.readObject();
//			}
//		}
//		Logger.simple(System.currentTimeMillis() - s);
	}
}
