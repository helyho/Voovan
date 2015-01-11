package org.hocate.test.onlineComplier;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;
import org.hocate.dynamicComplier.Complier;


public class ComplierTest {
	
	public static void main(String[] args) {
		Logger logger = Logger.getLogger("test");
		String code = "package org.hocate.test;\r\n"
				+ "public class testSay {\r\n"
					+ " public void say(){\r\n"
						+ "System.out.println(\"helloword\");\r\n"
					+ "}\r\n"
				+ "}\r\n";
		Complier dc = new Complier();
		dc.compileCode( code);
		try {
			Class<?> testClazz = Class.forName("org.hocate.test.testSay");
			System.out.println(testClazz.getName());
			Object kk = testClazz.newInstance();
			logger.info(kk);
			Method m = kk.getClass().getMethod("say");
			m.invoke(kk);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
