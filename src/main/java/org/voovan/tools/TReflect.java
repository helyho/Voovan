package org.voovan.tools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 反射工具类
 * 
 * @author helyho
 *
 */
public class TReflect {

	/**
	 * 获得类所有的Field
	 * 
	 * @param clazz
	 * @return
	 */
	public static Field[] getFields(Class<?> clazz) {
		return clazz.getDeclaredFields();
	}

	/**
	 * 查找类特定的Field
	 * 
	 * @param clazz
	 * @param fieldName
	 * @return
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 */
	public static Field findField(Class<?> clazz, String fieldName)
			throws NoSuchFieldException, SecurityException {
		return clazz.getDeclaredField(fieldName);
	}

	/**
	 * 获取类中指定Field的值
	 * 
	 * @param obj
	 * @param fieldName
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	static public <T> T getFieldValue(Object obj, String fieldName)
			throws Exception {
		Field field = findField(obj.getClass(), fieldName);
		field.setAccessible(true);
		return (T) field.get(obj);
	}

	/**
	 * 更新对象中指定的Field的值
	 * 		注意:对 private 等字段有效
	 * 
	 * @param obj
	 * @param fieldName
	 * @param fieldValue
	 * @throws Exception
	 */
	public static void setFieldValue(Object obj, String fieldName,
			Object fieldValue) throws Exception {
		Field field = findField(obj.getClass(), fieldName);
		field.setAccessible(true);
		field.set(obj, fieldValue);
	}

	/**
	 * 将对象中的field和其值组装成Map 静态字段(static修饰的)不包括
	 * 
	 * @param obj
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> getFieldValues(Object obj)
			throws Exception {
		HashMap<String, Object> result = new HashMap<String, Object>();
		Field[] fields = getFields(obj.getClass());
		for (Field field : fields) {
			if (!Modifier.isStatic(field.getModifiers())) {
				String key = field.getName();
				Object value = getFieldValue(obj, key);
				if (value != null)
					result.put(key, value);
			}
		}
		return result;
	}

	/**
	 * 查找类中的方法
	 * @param clazz        类对象
	 * @param name		   方法名	
	 * @param paramTypes   参数类型
	 * @return			   方法对象
	 * @throws Exception   异常
	 */
	public static Method findMethod(Class<?> clazz, String name,
			Class<?>... paramTypes) throws Exception {
		return clazz.getDeclaredMethod(name, paramTypes);
	}
	
	/**
	 * 获取类的方法集合
	 * @param clazz		类对象
	 * @return
	 */
	public static Method[] getMethods(Class<?> clazz) {
		return clazz.getMethods();
	}
	
	/**
	 * 获取类的特定方法的集合
	 * 		类中可能存在同名方法
	 * @param clazz		类对象
	 * @param name		方法名	
	 * @return
	 */
	public static Method[] getMethods(Class<?> clazz,String name) {
		ArrayList<Method> methods = new ArrayList<Method>();
		Method[] allMethod = clazz.getMethods();
		
		for(Method method : allMethod){
			if(method.getName().equals(name) )
			methods.add(method);
		}
		return methods.toArray(new Method[0]);
	}
	
	/**
	 * 使用对象执行它的一个方法
	 * 		对对象执行一个指定Method对象的方法
	 * @param obj				执行方法的对象
	 * @param method			方法对象
	 * @return					方法返回结果
	 * @throws Exception		异常
	 */
	public static Object invokeMethod(Object obj, Method method, Object... parameters) throws Exception {
		method.setAccessible(true);
		return method.invoke(obj, parameters);
	}

	/**
	 * 使用对象执行方法
	 * 对对象执行一个通过 方法名和参数列表选择的方法
	 * @param obj				执行方法的对象
	 * @param name				执行方法名
	 * @param parameters		方法参数
	 * @return					方法返回结果
	 * @throws Exception		异常
	 */
	public static Object invokeMethod(Object obj, String name, Object... parameters) throws Exception {
		Class<?>[] parameterTypes = getParameters(parameters);
		Method method = findMethod(obj.getClass(), name, parameterTypes);
		method.setAccessible(true);
		return method.invoke(obj, parameters);
	}

	/**
	 * 构造新的对象
	 * 	通过参数中的构造参数对象parameters,选择特定的构造方法构造
	 * @param clazz			类对象
	 * @param parameters	构造方法参数
	 * @return
	 * @throws Exception
	 */
	public static Object newInstance(Class<?> clazz, Object ...parameters) throws Exception {
		Class<?>[] parameterTypes = getParameters(parameters);
		Constructor<?> constructor = clazz.getConstructor(parameterTypes);
		return constructor.newInstance(parameters);
	}
	
	/**
	 * 构造新的对象
	 * @param className		类名称
	 * @param parameters	构造方法参数
	 * @return
	 * @throws Exception
	 */
	public static Object newInstance(String className, Object ...parameters) throws Exception {
		Class<?> clazz = Class.forName(className);
		Class<?>[] parameterTypes = getParameters(parameters);
		Constructor<?> constructor = clazz.getConstructor(parameterTypes);
		return constructor.newInstance(parameters);
	}
	
	/**
	 * 将对象数组转换成,对象类型的数组
	 * @param objs	对象类型数组
	 * @return
	 */
	private static Class<?>[] getParameters(Object[] objs){
		Class<?>[] parameterTypes= new Class<?>[objs.length];
		for(int i=0;i<objs.length;i++){
			parameterTypes[i] = objs[i].getClass();
		}
		return parameterTypes;
	}

	/**
	 * 将Map转换成指定的对象
	 * 
	 * @param clazz			类对象
	 * @param mapField		Map 对象
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object getObjectFromMap(Class<?> clazz,
		Map<String, Object> mapField) throws Exception {
		Object obj = null;

		// java标准对象
		if (clazz.getName().startsWith("java.lang")) {
			//取 Map.Values 里的递第一个值
			String value = mapField.values().iterator().next().toString();
			obj = newInstance(clazz,  value );
		}
		//java 日期对象
		else if(clazz.getName().equals("java.util.Date")){
			//取 Map.Values 里的递第一个值
			String value = mapField.values().iterator().next().toString();
			String pattern = "yyyy-MM-dd HH:mm:ss";
			SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
			obj = dateFormat.parse(value.toString());
		}
		// 复杂对象
		else {
			obj = newInstance(clazz);
			Field[] fields = getFields(clazz);
			for (Field field : fields) {
				field.setAccessible(true);
				if (mapField.containsKey(field.getName())) {
					Object value = mapField.get(field.getName());
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();
					//标准类型不做判断直接填写相应的值到
					//日期类型
					if(fieldName.equals("java.util.Date")){
						String pattern = "yyyy-MM-dd HH:mm:ss";
						SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
						value = dateFormat.parse(value.toString());
					}
					//Map 类型
					else if(isImpByInterface(fieldType,Map.class)){
						Map mapObject = TObject.cast(newInstance(fieldType));
						mapObject.putAll(TObject.cast(value));
						value = mapObject;
					}
					//Collection 类型
					else if(isImpByInterface(fieldType,Collection.class)){
						Collection listObject = TObject.cast(newInstance(fieldType));
						listObject.addAll(TObject.cast(value));
						value = listObject;
					}
					//如果没有匹配到类型且参数是 Map 类型,就判定是复杂类型,递归调用方法
					else if(isImpByInterface(value.getClass(),Map.class)){
						value = getObjectFromMap(fieldType, TObject.cast(value));
					}
					setFieldValue(obj, fieldName, value);
				}
			}
		}
		return obj;
	}
	
	/**
	 * 将对象转换成 Map
	 * 			key 对象属性名称
	 * 			value 对象属性值
	 * @param obj      待转换的对象
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Object> getMapfromObject(Object obj) throws Exception{
		
		Map<String, Object> mapResult = new HashMap<String, Object>();
		Map<String, Object> fieldValues =  TReflect.getFieldValues(obj);
		//如果是 java 标准类型
		if(obj.getClass().getName().startsWith("java")){
			mapResult.put("value", obj);
		}
		//复杂对象类型
		else{
			for(Entry<String,Object> entry : fieldValues.entrySet()){
				String key = entry.getKey();
				Object value = entry.getValue();
				String valueClass = entry.getValue().getClass().getName();
				if(!key.contains("$")){
					if(valueClass.startsWith("java")){
						mapResult.put(key, value);
					}else {
						//如果是复杂类型则递归调用
						mapResult.put(key, getMapfromObject(value));
					}
				}
			}
		}
		return mapResult;
	}
	
	/**
	 * 判断某个类型是否实现了某个接口
	 * 		包括判断其父接口
	 * @param type               被判断的类型
	 * @param interfaceClass     检查是否实现了次类的接口
	 * @return
	 */
	public static boolean isImpByInterface(Class<?> type,Class<?> interfaceClass){
		Class<?>[] interfaces= type.getInterfaces();
		for (Class<?> interfaceItem : interfaces) {
			if (interfaceItem==interfaceClass) {
				return true;
			}
			else{
				return isImpByInterface(interfaceItem,interfaceClass);
			}
		}
		return false;
	}
	
	/**
	 * 判断某个类型是否继承于某个类
	 * 		包括判断其父类
	 * @param type			判断的类型
	 * @param extendsClass	用于判断的父类类型
	 * @return
	 */
	public static boolean isExtendsByClass(Class<?> type,Class<?> extendsClass){
		Class<?> superClass = type.getSuperclass();
		while(!superClass.equals(extendsClass) && !superClass.equals(Object.class)){
			superClass = superClass.getSuperclass();
		}
		
		if(superClass.equals(extendsClass)){
			return true;
		}
		else{
			return false;
		}
	}
}
