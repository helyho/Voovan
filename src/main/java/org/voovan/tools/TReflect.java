package org.voovan.tools;

import java.lang.reflect.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * 反射工具类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
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
			throws ReflectiveOperationException {
		try {
			return clazz.getDeclaredField(fieldName);
		}catch(NoSuchFieldException ex){
			return null;
		}
	}

	/**
	 * 查找类特定的Field
	 * 			不区分大小写,并且替换掉特殊字符
	 * @param clazz
	 * @param fieldName
	 * @return
	 * @throws ReflectiveOperationException
     */
	public static Field findFieldIgnoreCase(Class<?> clazz, String fieldName)
			throws ReflectiveOperationException{
		for(Field field : getFields(clazz)){
			if(field.getName().equalsIgnoreCase(fieldName)){
				return field;
			}
		}
		return null;
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
			throws ReflectiveOperationException {
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
			Object fieldValue) throws ReflectiveOperationException {
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
	public static Map<Field, Object> getFieldValues(Object obj)
			throws ReflectiveOperationException {
		HashMap<Field, Object> result = new HashMap<Field, Object>();
		Field[] fields = getFields(obj.getClass());
		for (Field field : fields) {
			if (!Modifier.isStatic(field.getModifiers())) {
				Object value = getFieldValue(obj, field.getName());
				if (value != null)
					result.put(field, value);
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
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws Exception   异常
	 */
	public static Method findMethod(Class<?> clazz, String name,
			Class<?>... paramTypes) throws ReflectiveOperationException  {
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
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws Exception		异常
	 */
	public static Object invokeMethod(Object obj, Method method, Object... parameters) throws ReflectiveOperationException {
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
	public static Object invokeMethod(Object obj, String name, Object... parameters) throws ReflectiveOperationException {
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
	public static <T> T newInstance(Class<T> clazz, Object ...parameters) throws ReflectiveOperationException {
		Class<?>[] parameterTypes = getParameters(parameters);
		Constructor<T> constructor = clazz.getConstructor(parameterTypes);
		return constructor.newInstance(parameters);
	}
	
	/**
	 * 构造新的对象
	 * @param className		类名称
	 * @param parameters	构造方法参数
	 * @return
	 * @throws Exception
	 */
	public static <T> T newInstance(String className, Object ...parameters) throws ReflectiveOperationException {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) Class.forName(className);
		return newInstance(clazz,parameters);
	}
	
	/**
	 * 将对象数组转换成,对象类型的数组
	 * @param objs	对象类型数组
	 * @return
	 */
	public static Class<?>[] getParameters(Object[] objs){
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
	 * @param mapObj		Map 对象
	 * @param ignoreCase    匹配属性名是否不区分大小写
	 * @return
	 * @throws ParseException 
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object getObjectFromMap(Class<?> clazz,
		Map<String, Object> mapObj,boolean ignoreCase) throws ReflectiveOperationException, ParseException {
		Object obj = null;

		// java标准对象
		if (!clazz.getName().contains(".")){
			obj = mapObj.values().iterator().next();
		}
		//java基本对象
		else if (clazz.getName().startsWith("java.lang")) {
			//取 Map.Values 里的递第一个值
			String value = mapObj.values().iterator().next().toString();
			obj = newInstance(clazz,  value);
		}
		//java 日期对象
		else if(isExtendsByClass(clazz,Date.class)){
			//取 Map.Values 里的递第一个值
			String value = mapObj.values().iterator().next().toString();
			SimpleDateFormat dateFormat = new SimpleDateFormat(TDateTime.STANDER_DATETIME_TEMPLATE);
			obj = dateFormat.parse(value.toString());
		}
		//Map 类型
		else if(isImpByInterface(clazz,Map.class)){
			Map mapObject = TObject.cast(newInstance(clazz));
			mapObject.putAll(mapObj);
			obj = mapObject;
		}
		//Collection 类型
		else if(isImpByInterface(clazz,Collection.class)){
			Collection listObject = TObject.cast(newInstance(clazz));
			listObject.addAll((Collection) TObject.cast(mapObj.values().iterator().next()));
			obj = listObject;
		}
		// 复杂对象
		else {
			obj = newInstance(clazz);
			for(String key : mapObj.keySet()){
				Object value = mapObj.get(key);

				Field field = null;
				if(ignoreCase) {
					field = findFieldIgnoreCase(clazz, key);
				}else{
					//精确匹配属性
					field = findField(clazz, key);
				}

				if(field!=null) {
					String fieldName = field.getName();
					Class<?> fieldType = field.getType();

					if(isImpByInterface(value.getClass(),Map.class)){
						value = getObjectFromMap(fieldType, TObject.cast(value), ignoreCase);
					}else {
						value = getObjectFromMap(fieldType, TObject.newMap("value", TObject.cast(value)), ignoreCase);
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
	public static Map<String, Object> getMapfromObject(Object obj) throws ReflectiveOperationException{
		
		Map<String, Object> mapResult = new HashMap<String, Object>();
		Map<Field, Object> fieldValues =  TReflect.getFieldValues(obj);
		//如果是 java 标准类型
		if(obj.getClass().getName().startsWith("java")){
			mapResult.put("value", obj);
		}
		//复杂对象类型
		else{
			for(Entry<Field,Object> entry : fieldValues.entrySet()){
				String key = entry.getKey().getName();
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
			if (interfaceItem.equals(interfaceClass)) {
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
		Class<?> superClass = type;
		do{
			if(superClass.equals(extendsClass)){
				return true;
			}
			superClass = superClass.getSuperclass();
		}while(superClass!=null && !superClass.equals(extendsClass) && !superClass.equals(Object.class));

		return false;
	}

	public static void main(String[] args) {
		System.out.println(isExtendsByClass(Date.class,Date.class));
	}
}
