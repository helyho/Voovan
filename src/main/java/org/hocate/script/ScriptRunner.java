package org.hocate.script;


import java.io.FileReader;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.hocate.tools.TObject;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * 脚本运行器
 * @author helyho
 *
 */
@SuppressWarnings("restriction")
public class ScriptRunner {

	private ScriptEngineManager sem;
	private ScriptEngine jsEngine;

	/**
	 * 构造函数
	 */
	public ScriptRunner() {
		sem = new ScriptEngineManager();
		jsEngine = sem.getEngineByName("Nashorn");
	}
	
	/**
	 * 获取脚本环境对象
	 * @param objName 脚本环境中对象名称
	 * @return
	 */
	public Object get(String objName) {
		 Object object = jsEngine.get(objName);;
		 if(object instanceof ScriptObjectMirror){
			 return new ScriptObject(TObject.cast(object));
		 }
		 else{
			 return object;
		 }
	}
	
	/**
	 * 插入 java 对象到脚本环境中
	 * @param name  对象名称 -- java 对象在脚本环境中的名称
	 * @param obj	JAVA 对象
	 */
	public void put(String name,Object obj) {
		jsEngine.put(name, obj);
	}
	
	/**
	 * 执行脚本字符串
	 * 
	 * @param scriptStr
	 * @return
	 * @throws ScriptException
	 */
	public Object eval(String scriptCode) throws Exception {
		return jsEngine.eval(scriptCode);
	}
	
	/**
	 * 执行脚本字符串
	 * 
	 * @param scriptStr
	 * @return
	 * @throws ScriptException
	 */
	public Object eval(FileReader reader) throws Exception {
		return jsEngine.eval(reader);
	}

	/**
	 * 绑定全局变量
	 * 
	 * @param name
	 * @param obj
	 */
	public void bindingGlobal(String name, Object obj) {
		jsEngine.getBindings(ScriptContext.GLOBAL_SCOPE).put(name, obj);
	}

	/**
	 * 绑定当前环境上下文变量
	 * 
	 * @param name
	 * @param obj
	 */
	public void bindingEngine(String name, Object obj) {
		jsEngine.getBindings(ScriptContext.ENGINE_SCOPE).put(name, obj);
	}
}
