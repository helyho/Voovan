package org.hocate.biz.config;

import org.hocate.script.ScriptManager;

public class Execute {
	public static void main(String[] args) {
		ConfigLoader configLoader = new ConfigLoader();
		ScriptManager scriptManager = configLoader.createScriptManager();
		if(args.length==1){
			scriptManager.evalScriptEntity(args[0]);
		}
		else if(args.length==2){
			scriptManager.evalScriptEntity(args[0],Float.valueOf(args[1]));
		}
	}
	
}
