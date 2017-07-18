package org.voovan.test.tools.compiler;

import junit.framework.TestCase;
import org.voovan.tools.compiler.DynamicCompiler;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

public class DynamicCompilerUnit extends TestCase {

	public DynamicCompilerUnit(String name) {
		super(name);
	}
	
	public void setUp(){
		String codeStr = "package org.hocate.test1;\r\n\r\n"
				+ "import org.voovan.tools.TString;\r\n"
				+ "public class testSay {\r\n"
					+ "\t public String say(){\r\n"
						+ "\t\t System.out.println(\"helloword\");\r\n"
						+ "\t\t return TString.removePrefix(\"finished\"); \r\n"
					+ "\t }\r\n"
				+ "}\r\n";
		Logger.simple(codeStr);
		DynamicCompiler dc = new DynamicCompiler();
		dc.compileCode(codeStr);
	}
	
	public void testRun() throws Exception{
		Class testSayClass = DynamicCompiler.getClassByName("org.hocate.test1.testSay");
		Object testSay = TReflect.newInstance(testSayClass);
		assertEquals(testSay.getClass().getName(),"org.hocate.test1.testSay");
		Object obj = TReflect.invokeMethod(testSay, "say");
		assertEquals(obj,"inished");
	}

}
