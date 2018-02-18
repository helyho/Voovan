package org.voovan.tools.reflect;


import org.voovan.tools.TDateTime;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.TUnsafe;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.annotation.NotJSON;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.rmi.NotBoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

	private static Map<String, Field> fields = new HashMap<String ,Field>();
	private static Map<String, Method> methods = new HashMap<String ,Method>();
	private static Map<String, Constructor> constructors = new HashMap<String ,Constructor>();
	private static Map<String, Field[]> fieldArrays = new HashMap<String ,Field[]>();
	private static Map<String, Method[]> methodArrays = new HashMap<String ,Method[]>();
	private static Map<String, Constructor[]> constructorArrays = new HashMap<String ,Constructor[]>();
	private static Map<String, Boolean> classHierarchy = new HashMap<String ,Boolean>();

	/**
	 * 获得类所有的Field
	 *
	 * @param clazz 类对象
	 * @return Field数组
	 */
	public static Field[] getFields(Class<?> clazz) {
		String mark = clazz.getCanonicalName();
		Field[] fields = null;

		if(fieldArrays.containsKey(mark)){
			fields = fieldArrays.get(mark);
		}else {
			ArrayList<Field> fieldArray = new ArrayList<Field>();
			for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
				Field[] tmpFields = clazz.getDeclaredFields();
				fieldArray.addAll(Arrays.asList(tmpFields));
			}
			fields = fieldArray.toArray(new Field[]{});
			fieldArrays.put(mark, fields);
			fieldArray.clear();
		}

		return fields;
	}

	/**
	 * 查找类特定的Field
	 *
	 * @param clazz   类对象
	 * @param fieldName field 名称
	 * @return field 对象
	 * @throws NoSuchFieldException 无 Field 异常
	 * @throws SecurityException 安全性异常
	 */
	public static Field findField(Class<?> clazz, String fieldName)
			throws ReflectiveOperationException {

		String mark = clazz.getCanonicalName()+"#"+fieldName;

		if(fields.containsKey(mark)){
			return fields.get(mark);
		}else {
			Field field = null;

			for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
				try {
					field = clazz.getDeclaredField(fieldName);
					break;
				}catch(ReflectiveOperationException e){
					field = null;
				}
			}

			fields.put(mark, field);

			return field;
		}
	}

	/**
	 * 查找类特定的Field
	 * 			不区分大小写,并且替换掉特殊字符
	 * @param clazz   类对象
	 * @param fieldName Field 名称
	 * @return Field 对象
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static Field findFieldIgnoreCase(Class<?> clazz, String fieldName)
			throws ReflectiveOperationException{

		String mark = clazz.getCanonicalName()+"#"+fieldName;

		if(fields.containsKey(mark)){
			return fields.get(mark);
		}else {
			for (Field field : getFields(clazz)) {
				if (field.getName().equalsIgnoreCase(fieldName) || field.getName().equalsIgnoreCase(TString.underlineToCamel(fieldName))) {
					fields.put(mark, field);
					return field;
				}
			}
		}
		return null;
	}

	/**
	 * 获取范型类型
	 * @param type 类型对象
	 * @return Class[] 对象
	 */
	public static Class[] getGenericClass(ParameterizedType type) {
		Class[] result = null;
		Type[] actualType = type.getActualTypeArguments();
		result = new Class[actualType.length];

		for(int i=0;i<actualType.length;i++){
			if(actualType[i] instanceof Class){
				result[i] = (Class)actualType[i];
			} else if(actualType[i] instanceof Type){
				String classStr = actualType[i].toString();
				classStr = TString.fastReplaceAll(classStr, "<.*>", "");
				try {
					result[i] = Class.forName(classStr);
				} catch(Exception e){
					result[i] = Object.class;
				}
			} else{
				result[i] = Object.class;
			}
		}
		return result;
	}


	/**
	 * 获取 Field 的范型类型
	 * @param field  field 对象
	 * @return 返回范型类型数组
	 * @throws ClassNotFoundException 类找不到异常
	 */
	public static Class[] getFieldGenericType(Field field) throws ClassNotFoundException {
		Type fieldType = field.getGenericType();
		if(fieldType instanceof ParameterizedType){
			return getGenericClass((ParameterizedType)fieldType);
		}
		return null;
	}

	/**
	 * 获取类中指定Field的值
	 * @param <T> 范型
	 * @param obj  对象
	 * @param fieldName Field 名称
	 * @return Field 的值
	 * @throws ReflectiveOperationException 反射异常
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
	 * @param obj  对象
	 * @param fieldName field 名称
	 * @param fieldValue field 值
	 * @throws ReflectiveOperationException 反射异常
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
	 * @param obj 对象
	 * @return 所有 field 名称-值拼装的 Map
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static Map<Field, Object> getFieldValues(Object obj)
			throws ReflectiveOperationException {
		HashMap<Field, Object> result = new HashMap<Field, Object>();
		Field[] fields = getFields(obj.getClass());
		for (Field field : fields) {
			if (!Modifier.isStatic(field.getModifiers()) &&
					field.getAnnotation(NotJSON.class)==null &&
					field.getAnnotation(NotSerialization.class)==null) {
				Object value = getFieldValue(obj, field.getName());
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
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static Method findMethod(Class<?> clazz, String name,
									Class<?>... paramTypes) throws ReflectiveOperationException {
		String mark = clazz.getCanonicalName()+"#"+name;
		for(Class<?> paramType : paramTypes){
			mark = mark + "$" + paramType.getCanonicalName();
		}

		if(methods.containsKey(mark)){
			return methods.get(mark);
		}else {
			Method method = null;

			for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
				try {
					method = clazz.getDeclaredMethod(name, paramTypes);
					break;
				}catch(ReflectiveOperationException e){
					method = null;
				}
			}

			methods.put(mark, method);
			return method;
		}
	}

	/**
	 * 查找类中的方法(使用参数数量)
	 * @param clazz        类对象
	 * @param name		   方法名
	 * @param paramCount   参数数量
	 * @return			   方法对象
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static Method[] findMethod(Class<?> clazz, String name,
									  int paramCount) throws ReflectiveOperationException {
		Method[] methods = null;

		String mark = clazz.getCanonicalName()+"#"+name+"@"+paramCount;

		if(methodArrays.containsKey(mark)){
			return methodArrays.get(mark);
		} else {
			ArrayList<Method> methodList = new ArrayList<Method>();
			Method[] allMethods = getMethods(clazz, name);
			for (Method method : allMethods) {
				if (method.getParameterTypes().length == paramCount) {
					methodList.add(method);
				}
			}
			methods = methodList.toArray(new Method[]{});
			methodArrays.put(mark,methods);
			methodList.clear();
		}

		return methods;
	}

	/**
	 * 获取类的方法集合
	 * @param clazz		类对象
	 * @return Method 对象数组
	 */
	public static Method[] getMethods(Class<?> clazz) {

		Method[] methods = null;

		String mark = clazz.getCanonicalName();

		if(methodArrays.containsKey(mark)){
			return methodArrays.get(mark);
		} else {
			List<Method> methodList = new ArrayList<Method>();
			for (; clazz!=null && clazz != Object.class; clazz = clazz.getSuperclass()) {
				Method[] tmpMethods = clazz.getDeclaredMethods();
				methodList.addAll(Arrays.asList(tmpMethods));
			}
			methods = methodList.toArray(new Method[]{});
			methodList.clear();
			methodArrays.put(mark,methods);
		}
		return methods;
	}

	/**
	 * 获取类的特定方法的集合
	 * 		类中可能存在同名方法
	 * @param clazz		类对象
	 * @param name		方法名
	 * @return Method 对象数组
	 */
	public static Method[] getMethods(Class<?> clazz,String name) {

		Method[] methods = null;

		String mark = clazz.getCanonicalName()+"#"+name;

		if(methodArrays.containsKey(mark)){
			return methodArrays.get(mark);
		} else {
			ArrayList<Method> methodList = new ArrayList<Method>();
			Method[] allMethods = getMethods(clazz);
			for (Method method : allMethods) {
				if (method.getName().equals(name))
					methodList.add(method);
			}
			methods = methodList.toArray(new Method[0]);
			methodList.clear();
			methodArrays.put(mark,methods);
		}
		return methods;
	}

	/**
	 * 获取方法的参数返回值的范型类型
	 * @param method  method 对象
	 * @param parameterIndex 参数索引(大于0)参数索引位置[第一个参数为0,以此类推], (-1) 返回值
	 * @return 返回范型类型数组
	 * @throws ClassNotFoundException 类找不到异常
	 */
	public static Class[] getMethodParameterGenericType(Method method,int parameterIndex) throws ClassNotFoundException {
		Class[] result = null;
		Type parameterType;

		if(parameterIndex == -1){
			parameterType = method.getGenericReturnType();
		}else{
			parameterType = method.getGenericParameterTypes()[parameterIndex];
		}

		if(parameterType instanceof ParameterizedType){
			return getGenericClass((ParameterizedType)parameterType);
		}
		return null;
	}

	/**
	 * 使用对象执行它的一个方法
	 * 		对对象执行一个指定Method对象的方法
	 * @param obj				执行方法的对象
	 * @param method			方法对象
	 * @param parameters        多个参数
	 * @param <T> 范型
	 * @return					方法返回结果
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static <T> T invokeMethod(Object obj, Method method, Object... parameters)
			throws ReflectiveOperationException {
		return (T)method.invoke(obj, parameters);
	}

	/**
	 * 使用对象执行方法
	 * 推荐使用的方法,这个方法会自动寻找参数匹配度最好的方法并执行
	 * 对对象执行一个通过 方法名和参数列表选择的方法
	 * @param obj				执行方法的对象,如果调用静态方法直接传 Class 类型的对象
	 * @param name				执行方法名
	 * @param args		方法参数
	 * @param <T> 范型
	 * @return					方法返回结果
	 * @throws ReflectiveOperationException		反射异常
	 */
	public static <T> T invokeMethod(Object obj, String name, Object... args)
			throws ReflectiveOperationException {
		if(args==null){
			args = new Object[0];
		}
		Class<?>[] parameterTypes = getArrayClasses(args);
		Method method = null;
		Class objClass = (obj instanceof Class) ? (Class)obj : obj.getClass();
		try {
			method = findMethod(objClass, name, parameterTypes);
			method.setAccessible(true);
			return (T)method.invoke(obj, args);
		}catch(Exception e){
			Exception lastExecption = e;

			if(e instanceof NoSuchMethodException || method == null) {
				//找到这个名称的所有方法
				Method[] methods = findMethod(objClass, name, parameterTypes.length);
				for (Method similarMethod : methods) {
					Type[] methodParamTypes = similarMethod.getGenericParameterTypes();
					//匹配参数数量相等的方法
					if (methodParamTypes.length == args.length) {
						try{
							return (T)similarMethod.invoke(obj, args);
						} catch (Exception ex){
							//不处理
						}

						try {
							Object[] convertedParams = new Object[args.length];
							for (int i = 0; i < methodParamTypes.length; i++) {
								Type parameterType = methodParamTypes[i];

								//如果范型类型没有指定则使用 Object 作为默认类型
								if(parameterType instanceof TypeVariable){
									parameterType = Object.class;
								}

								//参数类型转换
								String value = "";

								//这里对参数类型是 Object或者是范型 的提供支持
								if (parameterType != Object.class && args[i] != null) {
									Class argClass = args[i].getClass();

									//复杂的对象通过 JSON转换成字符串,再转换成特定类型的对象
									if (args[i] instanceof Collection ||
											args[i] instanceof Map ||
											argClass.isArray() ||
											!TReflect.isBasicType(argClass)) {
										//增加对于基本类型 Array 的支持
										if(argClass.isArray() && TReflect.isBasicType(argClass.getComponentType())) {
											convertedParams[i]  = args[i];
											continue;
										} else {
											value = JSON.toJSON(args[i]);
										}
									} else {
										value = args[i].toString();
									}
									convertedParams[i] = TString.toObject(value, parameterType);
								} else {
									convertedParams[i] = args[i];
								}
							}
							method = similarMethod;
							method.setAccessible(true);
							return (T)method.invoke(obj, convertedParams);
						} catch (Exception ex) {
							lastExecption = (Exception) ex.getCause();
							continue;
						}
					}
				}
			}

			if ( !(lastExecption instanceof ReflectiveOperationException) ) {
				lastExecption = new ReflectiveOperationException(lastExecption.getMessage(), lastExecption);
			}

			throw (ReflectiveOperationException)lastExecption;
		}
	}

	/**
	 * 构造新的对象
	 * 	通过参数中的构造参数对象parameters,选择特定的构造方法构造
	 * @param <T>           范型
	 * @param clazz			类对象
	 * @param args	构造方法参数
	 * @return 新的对象
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static <T> T newInstance(Class<T> clazz, Object ...args)
			throws ReflectiveOperationException {
		if(args==null){
			args = new Object[0];
		}
		Class<?>[] parameterTypes = getArrayClasses(args);
		Constructor<T> constructor = null;

		String mark = clazz.getCanonicalName();
		for(Class<?> paramType : parameterTypes){
			mark = mark + "$" + paramType.getCanonicalName();
		}

		try {

			if(constructors.containsKey(mark)){
				constructor = constructors.get(mark);
			}else {
				if (args.length == 0) {
					try {
						constructor = clazz.getConstructor();
					}catch (Exception e) {
						return (T) TUnsafe.getUnsafe().allocateInstance(clazz);
					}
				} else {
					constructor = clazz.getConstructor(parameterTypes);
				}

				constructors.put(mark, constructor);
			}

			return constructor.newInstance(args);

		}catch(Exception e){
			Exception lastExecption = e;
			if(constructor==null) {
				Constructor[] constructors = null;

				//缓存构造函数
				mark = clazz.getCanonicalName();
				if(constructorArrays.containsKey(mark)){
					constructors = constructorArrays.get(mark);
				}else {
					constructors = clazz.getConstructors();
					constructorArrays.put(mark, constructors);
				}

				for (Constructor similarConstructor : constructors) {
					Class[] methodParamTypes = similarConstructor.getParameterTypes();
					//匹配参数数量相等的方法
					if (methodParamTypes.length == args.length) {

						try{
							return (T) similarConstructor.newInstance(args);
						} catch (Exception ex){
							//不处理
						}

						try {
							Object[] convertedParams = new Object[args.length];
							for (int i = 0; i < methodParamTypes.length; i++) {
								Class parameterType = methodParamTypes[i];
								//参数类型转换
								String value = "";

								Class parameterClass = args[i].getClass();

								//复杂的对象通过 JSON转换成字符串,再转换成特定类型的对象
								if (args[i] instanceof Collection ||
										args[i] instanceof Map ||
										parameterClass.isArray() ||
										!TReflect.isBasicType(parameterClass)) {
									value = JSON.toJSON(args[i]);
								} else {
									value = args[i].toString();
								}

								convertedParams[i] = TString.toObject(value, parameterType);
							}
							constructor = similarConstructor;

							return constructor.newInstance(convertedParams);
						} catch (Exception ex) {
							continue;
						}
					}
				}
			}

			if ( !(lastExecption instanceof ReflectiveOperationException) ) {
				lastExecption = new ReflectiveOperationException(lastExecption.getMessage(), lastExecption);
			}

			//尝试使用 Unsafe 分配
			try{
				return allocateInstance(clazz);
			}catch(Exception ex) {
				throw e;
			}
		}

	}

	/**
	 * 构造新的对象
	 * @param <T> 范型
	 * @param className		类名称
	 * @param parameters	构造方法参数
	 * @return 新的对象
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static <T> T newInstance(String className, Object ...parameters) throws ReflectiveOperationException {
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) Class.forName(className);
		return newInstance(clazz,parameters);
	}

	/**
	 * 采用 Unsafe 构造一个对象,无须参数
	 * @param clazz 对象类型
	 * @param <T> 范型
	 * @return 新的对象
	 * @throws InstantiationException 实例化异常
	 */
	public static <T> T allocateInstance(Class<T> clazz) throws InstantiationException {
		return (T) TUnsafe.getUnsafe().allocateInstance(clazz);
	}

	/**
	 * 将对象数组转换成,对象类型的数组
	 * @param objs	对象类型数组
	 * @return 类数组
	 */
	public static Class<?>[] getArrayClasses(Object[] objs){
		if(objs == null){
			return new Class<?>[0];
		}

		Class<?>[] parameterTypes= new Class<?>[objs.length];
		for(int i=0;i<objs.length;i++){
			if(objs[i]==null){
				parameterTypes[i] = Object.class;
			}else {
				parameterTypes[i] = objs[i].getClass();
			}
		}
		return parameterTypes;
	}

	/**
	 * 将Map转换成指定的对象
	 *
	 * @param type			类对象
	 * @param mapArg		Map 对象
	 * @param ignoreCase    匹配属性名是否不区分大小写
	 * @return 转换后的对象
	 * @param <T> 范型
	 * @throws ReflectiveOperationException 反射异常
	 * @throws ParseException 解析异常
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T>T getObjectFromMap(Type type, Map<String, ?> mapArg, boolean ignoreCase)
			throws ReflectiveOperationException, ParseException {
		T obj = null;
		Class<?> clazz = null;
		Class[] genericType = null;

		if(type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			clazz = (Class)parameterizedType.getRawType();
			genericType = getGenericClass(parameterizedType);
		}else if(type instanceof Class){
			clazz = (Class)type;
		}

		if(mapArg==null){
			return null;
		}

		Object singleValue = null;

		if(mapArg.size()==1){
			singleValue = mapArg.values().iterator().next();
		}

		// java标准对象
		if (clazz.isPrimitive()){
			if(singleValue.getClass() !=  clazz) {
				obj = TString.toObject(singleValue.toString(), clazz);
			} else {
				obj = (T)singleValue;
			}
		}
		//对象类型
		else if(clazz == Object.class){
			if(singleValue!=null){
				obj = (T)singleValue;
			}else {
				obj = (T) mapArg;
			}
		}
		//java 日期对象
		else if(isExtendsByClass(clazz, Date.class)){
			//取 Map.Values 里的递第一个值
			String value = singleValue==null?null:singleValue.toString();
			SimpleDateFormat dateFormat = new SimpleDateFormat(TDateTime.STANDER_DATETIME_TEMPLATE);
			Date dateObj = singleValue!=null?dateFormat.parse(value.toString()):null;
			obj = (T)TReflect.newInstance(clazz,dateObj.getTime());
		}
		//Map 类型
		else if(isImpByInterface(clazz, Map.class)){
			//不可构造的类型使用最常用的类型
			if(Modifier.isAbstract(clazz.getModifiers()) && Modifier.isInterface(clazz.getModifiers())){
				clazz = HashMap.class;
			}
			Map mapObject = (Map)newInstance(clazz);

			if(genericType!=null) {
				Iterator iterator = mapArg.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry entry = (Entry) iterator.next();
					Map keyOfMap = null;
					Map valueOfMap = null;

					if (entry.getKey() instanceof Map) {
						keyOfMap = (Map) entry.getKey();
					} else {
						keyOfMap = TObject.asMap("value", entry.getKey());
					}

					if (entry.getValue() instanceof Map) {
						valueOfMap = (Map) entry.getValue();
					} else {
						valueOfMap = TObject.asMap("value", entry.getValue());
					}

					Object keyObj = getObjectFromMap(genericType[0], keyOfMap, ignoreCase);
					Object valueObj = getObjectFromMap(genericType[1], valueOfMap, ignoreCase);
					mapObject.put(keyObj, valueObj);
				}
			}else{
				mapObject.putAll(mapArg);
			}
			obj = (T)mapObject;
		}
		//Collection 类型
		else if(isImpByInterface(clazz, Collection.class)){
			//不可构造的类型使用最常用的类型
			if(Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers())){
				clazz = ArrayList.class;
			}
			Collection collectionObject = (Collection)newInstance(clazz);

			if(singleValue!=null){
				if(genericType!=null){
					for (Object listItem : (Collection)singleValue) {
						Map valueOfMap = null;
						if (listItem instanceof Map) {
							valueOfMap = (Map) listItem;
						} else {
							valueOfMap = TObject.asMap("value", listItem);
						}

						Object item = getObjectFromMap(genericType[0], valueOfMap, ignoreCase);
						collectionObject.add(item);
					}
				}else{
					collectionObject.addAll((Collection)singleValue);
				}
			}
			obj = (T)collectionObject;
		}
		//Array 类型
		else if(clazz.isArray()){
			Class arrayClass = clazz.getComponentType();
			Object tempArrayObj = Array.newInstance(arrayClass, 0);
			return (T)((Collection)singleValue).toArray((Object[])tempArrayObj);
		}
		//java BigDecimal对象
		else if (isExtendsByClass(clazz, BigDecimal.class)) {
			//取 Map.Values 里的递第一个值
			String value = singleValue==null?null:singleValue.toString();
			obj = (T)(singleValue==null?null:new BigDecimal(value));
		}
		//java基本对象
		else if (TReflect.isBasicType(clazz)) {
			//取 Map.Values 里的递第一个值
			String value = singleValue==null?null:singleValue.toString();
			obj = (T)(singleValue==null?null:newInstance(clazz,  value));
		}
		//对 Atom 类型的处理
		else if (clazz == AtomicLong.class || clazz == AtomicInteger.class || clazz == AtomicBoolean.class) {
			if(singleValue==null){
				obj = null;
			} else {
				obj = (T) TReflect.newInstance(clazz, singleValue);
			}
		}
		// 复杂对象
		else {
			obj = (T)newInstance(clazz);
			for(Entry<String,?> argEntry : mapArg.entrySet()){
				String key = argEntry.getKey();
				Object value = argEntry.getValue();

				Field field = null;
				if(ignoreCase) {
					//忽略大小写匹配
					field = findFieldIgnoreCase(clazz, key);
				}else{
					//精确匹配属性
					field = findField(clazz, key);
				}

				if(field!=null) {
					String fieldName = field.getName();
					Class fieldType = field.getType();
					Type fieldGenericType = field.getGenericType();
					try {

						//value 和 fieldType class类型不同时，且　value 不为空时处理
						if(value != null && fieldType != value.getClass()) {
							//通过 JSON 将,String类型的 value转换,将 String 转换成 Collection, Map 或者 复杂类型 对象作为参数
							if( value instanceof String &&
									(
											isImpByInterface(fieldType, Map.class) ||
													isImpByInterface(fieldType, Collection.class) ||
													!TReflect.isBasicType(fieldType)
									)
									){
								value = TString.toObject(value.toString(), fieldType);
							}

							//对于 目标对象类型为 Map 的属性进行处理,查找范型,并转换为范型定义的类型
							else if (isImpByInterface(fieldType, Map.class) && value instanceof Map) {
								value = getObjectFromMap(fieldGenericType, (Map<String,?>)value, ignoreCase);
							}
							//对于 目标对象类型为 Collection 的属性进行处理,查找范型,并转换为范型定义的类型
							else if (isImpByInterface(fieldType, Collection.class) && value instanceof Collection) {
								value = getObjectFromMap(fieldGenericType, TObject.asMap("value", value), ignoreCase);
							}
							//对于 目标对象类型不是 Map,则认定为复杂类型
							else if (!isImpByInterface(fieldType, Map.class)) {
								if(value instanceof Map) {
									value = getObjectFromMap(fieldType, (Map<String, ?>) value, ignoreCase);
								}else{
									value = getObjectFromMap(fieldType, TObject.asMap("value", value), ignoreCase);
								}
							}else{
								throw new ReflectiveOperationException("Conver field object error! Exception type: " +
										fieldType.getName() +
										", Object type: "+
										value.getClass().getName());
							}
						}
						setFieldValue(obj, fieldName, value);
					}catch(Exception e){
						throw new ReflectiveOperationException("Fill object " + obj.getClass().getCanonicalName() +
								"#"+fieldName+" failed",e);
					}
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
	 * @return 转后的 Map
	 * @throws ReflectiveOperationException 反射异常
	 */
	public static Map<String, Object> getMapfromObject(Object obj) throws ReflectiveOperationException{

		Map<String, Object> mapResult = new HashMap<String, Object>();

		//如果是 java 标准类型
		if(obj==null || TReflect.isBasicType(obj.getClass())){
			mapResult.put(null, obj);
		}
		//java 日期对象
		else if(isExtendsByClass(obj.getClass(),Date.class)){
			mapResult.put(null,TDateTime.format((Date) obj, TDateTime.STANDER_DATETIME_TEMPLATE));
		}
		//对 Collection 类型的处理
		else if(obj instanceof Collection){
			Collection collection = (Collection) newInstance(obj.getClass());
			synchronized (obj) {
				Object[] objectArray = ((Collection) obj).toArray(new Object[0]);
				for (Object collectionItem : objectArray) {
					Map<String, Object> item = getMapfromObject(collectionItem);
					collection.add((item.size() == 1 && item.containsKey(null)) ? item.get(null) : item);
				}
			}
			mapResult.put(null, collection);
		}
		//对 Array 类型的处理
		else if(obj.getClass().isArray()){
			Class arrayClass = obj.getClass().getComponentType();
			Object targetArray = Array.newInstance(arrayClass, Array.getLength(obj));

			for(int i=0;i<Array.getLength(obj);i++) {
				Object arrayItem = Array.get(obj, i);
				Map<String, Object> item = getMapfromObject(arrayItem);
				Array.set(targetArray, i, (item.size()==1 && item.containsKey(null)) ? item.get(null) : item);
			}
			mapResult.put(null, targetArray);
		}
		//对 Atom 类型的处理
		else if (obj instanceof AtomicLong || obj instanceof AtomicInteger || obj instanceof AtomicBoolean) {
			mapResult.put(null, TReflect.invokeMethod(obj, "get"));
		}
		//对 BigDecimal 类型的处理
		else if (obj instanceof BigDecimal) {
			mapResult.put(null, obj.toString());
		}
		//对 Map 类型的处理
		else if(obj instanceof Map){
			Map mapObject = (Map)obj;

			Map map = (Map)newInstance(obj.getClass());
			synchronized (obj) {
				Iterator iterator = mapObject.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<?, ?> entry = (Entry<?, ?>) iterator.next();
					Map<String, Object> keyItem = getMapfromObject(entry.getKey());
					Map<String, Object> valueItem = getMapfromObject(entry.getValue());
					Object key = (keyItem.size() == 1 && keyItem.containsKey(null)) ? keyItem.get(null) : keyItem;
					Object value = (valueItem.size() == 1 && valueItem.containsKey(null)) ? valueItem.get(null) : valueItem;
					map.put(key, value);
				}
			}
			mapResult.put(null, map);
		}
		//复杂对象类型
		else{
			Map<Field, Object> fieldValues =  TReflect.getFieldValues(obj);
			for(Entry<Field,Object> entry : fieldValues.entrySet()){
				String key = entry.getKey().getName();
				Object value = entry.getValue();
				if(value == null){
					mapResult.put(key, value);
				}else if(!key.contains("$")){
					Class valueClass = entry.getValue().getClass();
					if(TReflect.isBasicType(valueClass)){
						mapResult.put(key, value);
					}else {
						//如果是复杂类型则递归调用
						Map resultMap = getMapfromObject(value);
						if(resultMap.size()==1 && resultMap.containsKey(null)){
							mapResult.put(key, resultMap.get(null));
						}else{
							mapResult.put(key, resultMap);
						}
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
	 * @return 是否实现某个接口
	 */
	public static boolean isImpByInterface(Class<?> type,Class<?> interfaceClass){
		if(type==interfaceClass){
			return true;
		}

		String marker = type.toString() + "@" + interfaceClass.toString();
		if(classHierarchy.containsKey(marker)){
			return classHierarchy.get(marker);
		}

		Class<?>[] interfaces= type.getInterfaces();
		for (Class<?> interfaceItem : interfaces) {
			if (interfaceItem == interfaceClass) {
				classHierarchy.put(marker, true);
				return true;
			}
			else {
				return isImpByInterface(interfaceItem,interfaceClass);
			}
		}

		classHierarchy.put(marker, false);
		return false;
	}



	/**
	 * 判断某个类型是否继承于某个类
	 * 		包括判断其父类
	 * @param type			判断的类型
	 * @param extendsClass	用于判断的父类类型
	 * @return 是否继承于某个类
	 */
	public static boolean isExtendsByClass(Class<?> type,Class<?> extendsClass){
		if(extendsClass == type){
			return true;
		}

		String marker = type.toString() + "#" + extendsClass.toString();
		if(classHierarchy.containsKey(marker)){
			return classHierarchy.get(marker);
		}

		Class<?> superClass = type;
		do{
			if(superClass == extendsClass){
				classHierarchy.put(marker, true);
				return true;
			}
			superClass = superClass.getSuperclass();
		}while(superClass!=null && Object.class != superClass);

		classHierarchy.put(marker, false);
		return false;
	}

	/**
	 * 类检查器
	 * 		是否符合 filters 中的约束条件, 注解/类/接口等
	 * @param clazz    Class 对象
	 * @param filters  过滤器
	 * @return true: 符合约束, false: 不符合约束
	 */
	public static boolean classChecker(Class clazz, Class[] filters){
		int matchCount = 0;
		List<Annotation> annontations = TObject.asList(clazz.getAnnotations());

		if(clazz.isAnonymousClass()) {
			return false;
		}

		for(Class filterClazz : filters){
			if(clazz == filterClazz){
				break;
			}

			if(filterClazz.isAnnotation() && clazz.isAnnotationPresent(filterClazz)){
				matchCount++;
			}else if(filterClazz.isInterface() && TReflect.isImpByInterface(clazz, filterClazz)){
				matchCount++;
			}else if(TReflect.isExtendsByClass(clazz, filterClazz)){
				matchCount++;
			}
		}

		if(matchCount < filters.length){
			return false;
		}else{
			return true;
		}
	}

	/**
	 * 获取类的继承树上的所有父类
	 * @param type 类型 Class
	 * @return 所有父类
	 */
	public static Class[] getAllExtendAndInterfaceClass(Class<?> type){
		if(type == null){
			return null;
		}

		ArrayList<Class> classes = new ArrayList<Class>();

		Class<?> superClass = type;
		do{
			superClass = superClass.getSuperclass();
			classes.addAll(Arrays.asList(superClass.getInterfaces()));
			classes.add(superClass);
		}while(superClass!=null && Object.class != superClass);
		return classes.toArray(new Class[]{});
	}

	/**
	 * 获取类的 json 形式的描述
	 * @param clazz  Class 类型对象
	 * @return 类的 json 形式的描述
	 */
	public static String getClazzJSONModel(Class clazz){
		StringBuilder jsonStrBuilder = new StringBuilder();
		if(TReflect.isBasicType(clazz)){
			jsonStrBuilder.append(clazz.getName());
		} else if(clazz.isArray()){
			String clazzName = clazz.getName();
			clazzName = clazzName.substring(clazzName.lastIndexOf(".")+1,clazzName.length()-2)+"[]";
			jsonStrBuilder.append(clazzName);
		} else {
			jsonStrBuilder.append("{");
			for (Field field : TReflect.getFields(clazz)) {
				jsonStrBuilder.append("\"");
				jsonStrBuilder.append(field.getName());
				jsonStrBuilder.append("\":");
				String filedValueModel = getClazzJSONModel(field.getType());
				if(filedValueModel.startsWith("{") && filedValueModel.endsWith("}")) {
					jsonStrBuilder.append(filedValueModel);
					jsonStrBuilder.append(",");
				} else if(filedValueModel.startsWith("[") && filedValueModel.endsWith("]")) {
					jsonStrBuilder.append(filedValueModel);
					jsonStrBuilder.append(",");
				} else {
					jsonStrBuilder.append("\"");
					jsonStrBuilder.append(filedValueModel);
					jsonStrBuilder.append("\",");
				}
			}
			jsonStrBuilder.deleteCharAt(jsonStrBuilder.length()-1);
			jsonStrBuilder.append("}");
		}
		return jsonStrBuilder.toString();
	}

	/**
	 * 过滤对象的属性, 产生一个 Map
	 *      未包含的属性的值将会以 null 返回
	 * @param obj 对象
	 * @param fields 保留的属性
	 * @return 最后产生的 Map
	 * @throws NotBoundException 属性为找到的异常
	 */
	public static Map<String, Object> fieldFilter(Object obj, String ... fields) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		for(String fieldFilter : fields){
			int firstIndex = fieldFilter.indexOf("[");
			String field = firstIndex == -1? fieldFilter : fieldFilter.substring(0, firstIndex);
			;
			Object value = null;

			//Map
			if(obj instanceof Map){
				Map paramMap = (Map)obj;
				value = paramMap.get(field);
			}
			//List/Array
			else if(obj.getClass().isArray() || obj instanceof List) {
				if(obj.getClass().isArray()){
					obj = TObject.asList((Object[])obj);
				}
				for(Object subObj : (List)obj){
					fieldFilter(subObj, fields);
				}
			}
			//complex object
			else {
				try {
					value = TReflect.getFieldValue(obj, field);
				} catch (ReflectiveOperationException e) {
					value = null;
				}
			}

			if(firstIndex>1) {
				Map<String, Object> subResultMap = new HashMap<String, Object>();
				String subFieldStr= fieldFilter.substring(firstIndex);
				subFieldStr = TString.removeSuffix(subFieldStr);
				subFieldStr = TString.removePrefix(subFieldStr);
				String[] subFieldArray = subFieldStr.split(",");
				for(String subField : subFieldArray) {
					Map<String, Object> data = fieldFilter(value, subField);
					subResultMap.putAll(data);
				}
				value = subResultMap;
			}

			resultMap.put(field, value);
		}
		return resultMap;
	}

	/**
	 * 判读是否是基本类型(null, boolean, byte, char, double, float, int, long, short, string)
	 * @param clazz Class 对象
	 * @return true: 是基本类型, false:非基本类型
	 */
	public static boolean isBasicType(Class clazz){
		if(clazz == null ||
				clazz.isPrimitive() ||
				clazz.getName().startsWith("java.lang")
				){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 判读是否是 JDK 中定义的类(java包下的所有类)
	 * @param clazz Class 对象
	 * @return true: 是JDK 中定义的类, false:非JDK 中定义的类
	 */
	public static boolean isSystemType(Class clazz){
		List<String> systemPackages = TObject.asList("java.","sun.","javax.","com.sun","com.oracle");


		if( clazz.isPrimitive()){
			return true;
		}

		//排除的包中的 class 不加载
		for(String systemPackage : systemPackages){
			if(clazz.getName().startsWith(systemPackage)){
				return true;
			}
		}

		return false;
	}
}
