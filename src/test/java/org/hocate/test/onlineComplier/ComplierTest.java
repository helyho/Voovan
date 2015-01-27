package org.hocate.test.onlineComplier;

import org.hocate.dynamicComplier.Complier;
import org.hocate.log.Logger;
import org.hocate.tools.TReflect;

public class ComplierTest {
	
	public static void main(String[] args) {
		String code = "package org.hocate.test;\r\n"
				+ "public class testSay {\r\n"
					+ " public void say(){\r\n"
						+ "System.out.println(\"helloword\");\r\n"
					+ "}\r\n"
				+ "}\r\n";
		Complier dc = new Complier();
		Logger.info("start .....");
		dc.compileCode( code);
		Logger.info("end .....");
		try {
			Object testSay = TReflect.newInstance("org.hocate.test.testSay");
			Logger.info(testSay);
			TReflect.invokeMethod(testSay, "say");
			Logger.info("end .....");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
