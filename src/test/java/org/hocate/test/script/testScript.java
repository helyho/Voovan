package org.hocate.test.script;

import org.hocate.script.ScriptRunner;

public class testScript {
	public static void main(String[] args) throws Exception {
		ScriptRunner scriptRunner = new ScriptRunner();
		Object object = scriptRunner.eval("org.hocate.util.UEnv.class");
		//scriptRunner.get("m");
		System.out.println(object.getClass());
	}
}
