package org.hocate.script;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * 脚本实体缓存池
 * 	对于配置 canRelaod=true 的脚本实体(ScriptEntity),自动刷新变更的脚本,并执行到容器中.
 * @author helyho
 *
 */
public class ScriptEntityPool {
	private static Logger logger = Logger.getLogger(ScriptEntityPool.class);
	
	private Map<Integer,ScriptEntity> scriptEntityMap;
	private ScriptManager scriptManager;
	
	/**
	 * 构造函数
	 * @param scriptManager			脚本管理器
	 * @param refreshTime			脚本管理器自动刷新事件
	 */
	public ScriptEntityPool(ScriptManager scriptManager,int refreshTime){
		scriptEntityMap = new Hashtable<Integer,ScriptEntity>();
		Timer refreshTimer =new Timer();
		this.scriptManager = scriptManager;
		refreshTimer.schedule(new RefreshTask(scriptEntityMap), refreshTime*1000, refreshTime*1000);
	}
	
	/**
	 * 内部类,脚本刷新任务业务
	 * @author helyho
	 *
	 */
	public class RefreshTask extends TimerTask{
		private Map<Integer,ScriptEntity> scriptEntityMap;
		public RefreshTask(Map<Integer,ScriptEntity> scriptEntityMap){
			this.scriptEntityMap = scriptEntityMap;
		}
		@Override
		public void run() {
			logger.debug("Begin to reload script entity.");
			Collection<ScriptEntity> scriptEntities = scriptEntityMap.values();
			for(ScriptEntity scriptEntity : scriptEntities){
				if(scriptEntity.canReload() && scriptEntity.isChanged()){
					scriptEntity.loadSourceCode();
					scriptManager.evalScriptEntity(scriptEntity);
					logger.debug("This script Entity : "+scriptEntity.getPackagePath()+" succesed.");
				}else {
					
					logger.debug("This script Entity don't need Reload. confident is [canReload="+scriptEntity.canReload()+"] [isChanged="+scriptEntity.isChanged()+"]");
				}
			}
			logger.debug("End to reload script entity.");
		}
	}
	
	/**
	 * 判断脚本实体是否存在
	 * @param packagePath  包路径
	 * @param version      脚本实体版本
	 * @return
	 */
	public boolean isExistsEntity(String packagePath,float version){
		int hashCode = ScriptEntity.genHashCode(packagePath, version);
		return scriptEntityMap.containsKey(hashCode);
	}
	
	/**
	 * 判断脚本实体是否存在
	 * @param scripEntity 脚本实体
	 * @return
	 */
	public boolean isExistsEntity(ScriptEntity scripEntity){
		return scriptEntityMap.containsKey(scripEntity.hashCode());
	}
	
	/**
	 * 获取脚本实体
	 * @param packagePath  包路径
	 * @param version      脚本实体版本
	 * @return
	 */
	public ScriptEntity getEntity(String packagePath,float version ){
		return scriptEntityMap.get(ScriptEntity.genHashCode(packagePath, version));
	}
	
	/**
	 * 增加脚本实体
	 * @param scripEntity 脚本实体
	 */
	public void addEntity(ScriptEntity scripEntity) {
		if(scripEntity!=null){
			scriptEntityMap.put(scripEntity.hashCode(), scripEntity);
		}
	}
	
	/**
	 * 移除脚本实体
	 * @param scripEntity 脚本实体
	 */
	public void removeEntity(ScriptEntity scripEntity) {
		scriptEntityMap.remove(scripEntity.hashCode());
	}
	
	/**
	 * 移除脚本实体
	 * @param packagePath  包路径
	 * @param version      脚本实体版本
	 */
	public void removeEntity(String packagePath,float version) {
		int hashCode = ScriptEntity.genHashCode(packagePath, version);
		scriptEntityMap.remove(hashCode);
	}
}
