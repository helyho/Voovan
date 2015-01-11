package org.hocate.test.script;

import org.hocate.script.scriptLoader.ScriptFileLoader;

public class ScriptFileLoaderTest {

	public static void main(String[] args) {
		ScriptFileLoader sfl = new ScriptFileLoader("/Users/helyho/Work/Java/BuizPlatform/js");
		System.out.println("====================================");
		System.out.println(sfl.getScriptEntity("org.hocate.test"));
		for(String sp : sfl.packagePaths()){
			System.out.println(sp);
		}
	}
}
