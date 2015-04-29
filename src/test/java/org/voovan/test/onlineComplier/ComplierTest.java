package org.voovan.test.onlineComplier;

import org.voovan.dynamicComplier.Complier;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

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
