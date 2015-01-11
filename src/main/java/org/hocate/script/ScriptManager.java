package org.hocate.script;

import javax.script.ScriptException;

import org.hocate.script.scriptLoader.ScriptLoader;

/**
 * 脚本管理器 初始化脚本环境
 * 
 * @author helyho
 *
 */
public class ScriptManager {
	private ScriptRunner runner;
	private ScriptLoader loader;
	private ScriptEntityPool scriptEntityPool;
	private boolean isInit;

	/**
	 * 构造函数
	 * 
	 * @param loader
	 *            scriptLoader对象,脚本读取器
	 */
	public ScriptManager(ScriptLoader loader) {
		this.scriptEntityPool = new ScriptEntityPool(this,60);
		this.loader = loader;
		runner = new ScriptRunner();
		this.isInit = false;
	}
	
	/**
	 * 构造函数
	 * @param loader			scriptLoader对象,脚本读取器
	 * @param refreshDelayTime	脚本刷新时间
	 */
	public ScriptManager(ScriptLoader loader,int refreshDelayTime) {
		this.scriptEntityPool = new ScriptEntityPool(this,refreshDelayTime);
		this.loader = loader;
		runner = new ScriptRunner();
		this.isInit = false;
	}

	/**
	 * 获取脚本运行器
	 * 
	 * @return
	 */
	public ScriptRunner getScriptRunner() {
		return runner;
	}

	/**
	 * 获取脚本读取器
	 * 
	 * @return
	 */
	public ScriptLoader getScriptLoader() {
		return loader;
	}

	/**
	 * 获取脚本环境对象
	 * 
	 * @param name
	 *            objName 脚本环境中对象名称
	 * @return
	 */
	public Object getObject(String name) {
		return runner.get(name);
	}

	/**
	 * 插入java对象到脚本环境中
	 * 
	 * @param name
	 *            对象名称 -- java 对象在脚本环境中的名称
	 * @param obj
	 *            JAVA 对象
	 */
	public void putObject(String name, Object object) {
		runner.put(name, object);
	}

	/**
	 * 执行脚本代码
	 * 
	 * @param scriptCode
	 * @return
	 */
	public Object evalCode(String scriptCode) {
		try {
			return runner.eval(scriptCode);
		} catch (Exception e) {
			ScriptException exception = new ScriptException(e.getMessage()+"\r\n"+scriptCode+"\r\n");
			exception.printStackTrace();
			return null;
		}
	}

	/**
	 * 执行脚本实体
	 * 
	 * @param entity
	 *            脚本实体
	 * @return
	 */
	public Object evalScriptEntity(ScriptEntity scriptEntity) {
		try {
			return runner.eval(scriptEntity.getSourceCode());
		} catch (Exception e) {
			ScriptException exception = new ScriptException(e.getMessage()+"\r\n [ScripFile] "+scriptEntity.getSourcePath());
			exception.printStackTrace();
			return null;
		}
	}

	/**
	 * 执行脚本实体 脚本path
	 * 
	 * @param packagePath
	 *            脚本path
	 * @return
	 */
	public Object evalScriptEntity(String packagePath) {
		ScriptEntity scriptEntity = getScriptEntity(packagePath);
		return this.evalScriptEntity(scriptEntity);
	}
	
	/**
	 * 执行脚本实体 脚本path
	 * 
	 * @param packagePath
	 * 				脚本path
	 * @param version
	 *         		脚本版本   
	 * @return
	 */
	public Object evalScriptEntity(String packagePath,float version) {
		ScriptEntity scriptEntity = getScriptEntity(packagePath,version);
		return this.evalScriptEntity(scriptEntity);
	}

	/**
	 * 获取一个脚本实体
	 * 
	 * @param packagePath
	 *            脚本实体路径
	 * @param version
	 *            脚本实体版本
	 * @return
	 */
	public ScriptEntity getScriptEntity(String packagePath, float version) {
		if(!scriptEntityPool.isExistsEntity(packagePath,version)){
			scriptEntityPool.addEntity(loader.getScriptEntity(packagePath, version));
		}
		return scriptEntityPool.getEntity(packagePath, version);
	}

	/**
	 * 获取一个脚本实体 (脚本实体的最新版本)
	 * 
	 * @param packagePath
	 *            脚本实体路径
	 * @return
	 */
	public ScriptEntity getScriptEntity(String packagePath) {
		ScriptEntity scriptEntity = loader.getScriptEntity(packagePath);
		scriptEntityPool.addEntity(scriptEntity);
		return scriptEntity;
	}

	/**
	 * 初始化脚本,只能执行一次
	 * 
	 * @param packagePaths
	 */
	public void initScript(String[] packagePaths) {
		if (isInit == false) {
			for (String packagePath : packagePaths) {
				this.evalScriptEntity(packagePath);
			}
			isInit = true;
		}
	}

	/**
	 * 初始化 java 对象到脚本执行容器中 作用域 Global
	 * @param name 对象名
	 * @param obj 对象实体
	 */
	public void GlobalObject(String name, Object obj) {
		runner.bindingGlobal(name, obj);
	}
	
}
