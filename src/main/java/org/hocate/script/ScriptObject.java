package org.hocate.script;

import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import org.hocate.tools.TObject;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import jdk.nashorn.internal.runtime.Context;

/**
 * JavaScript脚本对象
 * @author helyho
 *
 */
@SuppressWarnings("restriction")
public class ScriptObject {
	private ScriptObjectMirror scriptObjectMirror;
	
	/**
	 * 脚本对象构造函数
	 * @param scriptObjectMirror nashorn 引擎对象
	 */
	public  ScriptObject(ScriptObjectMirror scriptObjectMirror) {
		this.scriptObjectMirror = scriptObjectMirror;
	}
	
	/**
	 * 获取对象的属性
	 * @param propertyName 属性名称
	 * @return 属性值
	 */
	public Object getProperty(String propertyName){
		if (contains(propertyName)){
			 Object object = scriptObjectMirror.getMember(propertyName);
			 return ScriptObject.warp(object);
		}
		return null;
	}
	
	/**
	 * 执行对象的JS方法
	 * @param methodName  方法名称
	 * @param args        方法参数
	 * @return 方法的返回结果
	 */
	public Object callMethod(String methodName,Object ...args){
		if (contains(methodName)){
		 Object object = scriptObjectMirror.callMember(methodName, args);
		 return ScriptObject.warp(object);
		}
		return null;
	}
	
	/**
	 * 判断对象包含某个属性
	 * @param propertyName 属性名称
	 * @return true:存在,false:不存在
	 */
	public boolean contains(String propertyName) {
		return scriptObjectMirror.hasMember(propertyName);
	}
	
	/**
	 * 获取对象属性集合
	 * @return 对象属性集合
	 */
	public Map<String, Object> propertyMap() {
		HashMap<String,Object> propertyMap = new HashMap<String,Object>();
		for(Entry<String, Object> entry : scriptObjectMirror.entrySet()){
			propertyMap.put(entry.getKey(), entry.getValue());
		}
		return propertyMap;
	}
	
	/**
	 * 设置对象属性
	 * @param propertyName 属性名称
	 * @param object       属性值
	 * @return  true:成功,false:失败
	 */
	public boolean setProperty(String propertyName,Object object) {
		if(scriptObjectMirror.put(propertyName, object)==null){
			return false;
		}
		else{
			return true;
		}
	}
	
	/**
	 * 移除对象的某个属性
	 * @param propertyName	属性名称
	 * @return	true:成功,false:失败
	 */
	public boolean remove(String propertyName){
		return scriptObjectMirror.delete(propertyName);
	}
	
	/**
	 * 将对象封装成ScriptObjectMirror,主要用于接受从脚本通过函数调用传入到 java 方法中的参数的封装.
	 * @param object 脚本传入的参数对象
	 * @return 如果类型为 jdk.nashorn.internal.runtime.ScriptObject 则封装称ScriptObjectMirror,否则返回原始对象
	 */
	public static Object warp(Object object) {
		jdk.nashorn.internal.runtime.ScriptObject global = Context.getGlobal();
		Object warpObject = ScriptObjectMirror.wrap(object, global);
		if(warpObject instanceof ScriptObjectMirror){
			return new ScriptObject(TObject.cast(warpObject));
		}else{
			return warpObject;
		}
	}
}
