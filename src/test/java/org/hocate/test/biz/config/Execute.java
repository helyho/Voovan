package org.hocate.test.biz.config;

import java.sql.SQLException;

import org.hocate.biz.config.ConfigLoader;
import org.hocate.script.ScriptManager;

public class Execute {
	
	public static void main(String[] args) throws SQLException  {
		ConfigLoader cl = new ConfigLoader();
		ScriptManager sManager = cl.createScriptManager();
		sManager.evalScriptEntity("org.hocate.test");
	}
}
