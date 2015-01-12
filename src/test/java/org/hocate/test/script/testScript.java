package org.hocate.test.script;

import org.hocate.log.Logger;
import org.hocate.script.ScriptRunner;

public class testScript {
	public static void main(String[] args) throws Exception {
		ScriptRunner scriptRunner = new ScriptRunner();
		Object object = scriptRunner.eval("org.hocate.util.UEnv.class");
		//scriptRunner.get("m");
		Logger.simple(object.getClass());
	}
}
