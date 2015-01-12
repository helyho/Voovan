package org.hocate.test.script;

import java.io.IOException;
import java.util.Vector;

import org.hocate.biz.config.ConfigLoader;
import org.hocate.log.Logger;
import org.hocate.script.ScriptEntity;
import org.hocate.script.exception.ScriptException;
import org.hocate.script.scriptLoader.ScriptDbLoader;

public class ScriptDbLoaderTest {
	public static void main(String[] args) throws IOException, ScriptException {
		ConfigLoader configLoader = new ConfigLoader();
		ScriptDbLoader sdbl = (ScriptDbLoader) configLoader.createScriptManager().getScriptLoader();
		Vector<ScriptEntity> v = new Vector<ScriptEntity>();
		for(String mString : sdbl.packagePaths()){
			Logger.simple(mString);
		}
		Logger.simple("--------------------------------------------------");
		for(ScriptEntity entity : sdbl.scriptEntitys()){
			v.add(entity);
		}
		
		ScriptEntity entity1 = sdbl.getScriptEntity("org.hocate.test",2);
		Logger.simple(v.contains(entity1));
	}
}
