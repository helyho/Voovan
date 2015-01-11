/**
 * JavaScript类加载对象封装公共类
 */


/**
 * 加载 js,使用 path 的方式
 * @param script
 */
function require(script){
	var entity = ScriptManager.getScriptEntity(script);
	print("\r\n"+entity)
	ScriptManager.evalScriptEntity(entity);
}

/**
 * 把 js 对象封装称可以被 java 调用的 java ScriptObject 对象
 * @param obj
 */
function warp(obj){
	return org.hocate.script.ScriptObject.warp(obj);
}

/**
 * 导入 Class
 * @param javaClassPath
 */
function importClass(javaClassPath){
	javaClassSplit = javaClassPath.class.getName().split(".");
	var className = javaClassSplit[javaClassSplit.length-1];
	ScriptManager.GlobalObject(className,javaClassPath);
}

