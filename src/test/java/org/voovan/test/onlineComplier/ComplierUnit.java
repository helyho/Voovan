package org.voovan.test.onlineComplier;

import org.voovan.dynamicComplier.Complier;
import org.voovan.tools.TReflect;
import org.voovan.tools.log.Logger;

import junit.framework.TestCase;

public class ComplierUnit extends TestCase {

	public ComplierUnit(String name) {
		super(name);
	}
	
	public void setUp(){
		String codeStr = "package org.hocate.test;\r\n\r\n"
				+ "import org.voovan.tools.TString;\r\n"
				+ "public class testSay {\r\n"
					+ "\t public String say(){\r\n"
						+ "\t\t System.out.println(\"helloword\");\r\n"
						+ "\t\t return TString.removePrefix(\"finished\"); \r\n"
					+ "\t }\r\n"
				+ "}\r\n";
		Logger.simple(codeStr);
		Complier dc = new Complier();
		dc.compileCode(codeStr);
	}
	
	public void testRun() throws Exception{
		Object testSay = TReflect.newInstance("org.hocate.test.testSay");
		assertEquals(testSay.getClass().getName(),"org.hocate.test.testSay");
		Object obj = TReflect.invokeMethod(testSay, "say");
		assertEquals(obj,"inished");
	}

}
