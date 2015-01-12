package org.hocate.test.script;

import org.hocate.log.Logger;
import org.hocate.script.scriptLoader.ScriptFileLoader;

public class ScriptFileLoaderTest {

	public static void main(String[] args) {
		ScriptFileLoader sfl = new ScriptFileLoader("/Users/helyho/Work/Java/BuizPlatform/js");
		Logger.simple("====================================");
		Logger.simple(sfl.getScriptEntity("org.hocate.test"));
		for(String sp : sfl.packagePaths()){
			Logger.simple(sp);
		}
	}
}
