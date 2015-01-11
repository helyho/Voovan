package org.hocate.script.scriptLoader;

import java.util.List;

import org.hocate.script.ScriptEntity;

/**
 * 脚本读取器
 * @author helyho
 *
 */
public interface ScriptLoader {
	/**
	 * 获取一个脚本实体 (指定版本)
	 * 			曾今获取过的实体将会被返回,不查询数据库,效率高.
	 * @param packagePath 脚本实体路径
	 * @return
	 */
	public ScriptEntity getScriptEntity(String packagePath,float version);
	
	/**
	 * 获取一个脚本实体 (脚本实体的最新版本)
	 * 			有历史也会查询数据库,效率低
	 * @param packagePath 脚本实体路径
	 * @return
	 */
	public ScriptEntity getScriptEntity(String packagePath);
	
	
	/**
	 * 获取脚本路径,用户从getScriptEntity函数读取脚本
	 * @return
	 */
	public List<String> packagePaths();
	
	/**
	 * 获取脚本实体列表
	 * @return
	 */
	public List<ScriptEntity> scriptEntitys();
	
	/**
	 * 
	 */
	public void reLoad();
}
