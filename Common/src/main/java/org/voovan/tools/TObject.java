package org.voovan.tools;

import java.util.*;

/**
 * 对象工具类
 *
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TObject {
	/**
	 * 类型转换
	 * 	JDK 1.8 使用效果最好,否则有可能会转换失败
	 *
	 * @param <T> 范型
	 * @param obj 被转换对象
	 * @return	转换后的对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj){
		return (T)obj;
	}

	/**
	 * 转换成指定类型
	 * 	JDK 1.8 使用效果最好,否则有可能会转换失败
	 * @param <T> 范型
	 * @param obj   被转换对象
	 * @param t		指定的类型
	 * @return		转换后的对象
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj,Class<T> t){
		return (T)obj;
	}

	/**
	 * 空值默认值
	 * @param <T> 范型
	 * @param source	检测对象
	 * @param defValue		null 值替换值
	 * @return	如果非 null 则返回 source，如果为 null 则返回 defValue。
	 */
	public static <T>T nullDefault(T source,T defValue){
		return source!=null?source:defValue;
	}

	/**
	 * 初始化一个 List
	 * @param objs List 列表的每一个元素
	 * @return	初始化完成的List对象
	 */
	@SuppressWarnings("rawtypes")
	public static List asList(Object ...objs){
		List result = new ArrayList();
		result.addAll(Arrays.asList(objs));
		return result;
	}

	/**
	 * 初始化一个 Map
	 * @param objs		每两个参数组成一个键值对，来初始化一个 Map. 如:key1,value1,key2,value2.....
	 * @return	初始化完成的Map对象
	 */
	@SuppressWarnings("rawtypes")
	public static Map asMap(Object ...objs){
		Map<Object,Object> map = new LinkedHashMap<Object,Object>();
		for(int i=1;i<objs.length;i+=2){
			map.put(objs[i-1], objs[i]);
		}
		return map;
	}

	/**
	 * 将 Map 的值转换成 List
	 * @param map 需转换的 Map 对象
	 * @return 转后的 Value 的 list
	 */
	public static List<?> mapValueToList(Map<?,?> map){
		ArrayList<Object> result = new ArrayList<Object>();
		for(Map.Entry<?,?> entry : map.entrySet()){
			result.add(entry.getValue());
		}
		return result;
	}

	/**
	 * 将 Map 的值转换成 List
	 * @param map 需转换的 Map 对象
	 * @return 转后的 key 的 list
	 */
	public static List<?> mapKeyToList(Map<?,?> map){
		ArrayList<Object> result = new ArrayList<Object>();
		for(Map.Entry<?,?> entry : map.entrySet()){
			result.add(entry.getKey());
		}
		return result;
	}

	/**
	 * 将数组转换成 Map
	 * 			key 位置坐标
	 *          value 数组值
	 * @param objs    	待转换的数组
	 * @param <T> 范型
	 * @return 转换后的 Map  [序号, 值]
	 */
	public static <T> Map<String, T> arrayToMap(T[] objs){
		Map<String ,T> arrayMap = new LinkedHashMap<String ,T>();
		for(int i=0;i<objs.length;i++){
			arrayMap.put(Integer.toString(i+1), objs[i]);
		}
		return arrayMap;
	}
}
