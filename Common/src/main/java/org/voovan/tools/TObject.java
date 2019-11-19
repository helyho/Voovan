package org.voovan.tools;

import org.voovan.tools.reflect.TReflect;

import java.text.ParseException;
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
	 * 移除 Map 中 value 为空的 key
	 * @param map Map 对象
	 * @return 移除 Map 中 value 为空的 key 后的 map
	 */
	public static Map removeNullValueFromMap(Map map) {
		Iterator<Map.Entry> iterator = map.entrySet().iterator();
		while(iterator.hasNext()){
			Map.Entry entry = iterator.next();
			if(entry.getValue() == null) {
				iterator.remove();
			}
		}

		return map;
	}

	/**
	 * 初始化一个 List
	 * @param objs List 列表的每一个元素
	 * @return	初始化完成的List对象
	 */
	@SuppressWarnings("rawtypes")
	public static List asList(Object ...objs){
		List result = new ArrayList();
		for(Object o : objs) {
			result.add(o);
		}
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

	/**
	 * 将 Collection 转换成 Map
	 * 			key 位置坐标
	 *          value 数组值
	 * @param objs    	待转换的数组
	 * @param <T> 范型
	 * @return 转换后的 Map  [序号, 值]
	 */
	public static <T> Map<String, T> collectionToMap(Collection<T> objs){
		Map<String ,T> arrayMap = new LinkedHashMap<String ,T>();
		int i = 0;
		for(T t : objs){
			arrayMap.put(Integer.toString(++i), t);
		}
		return arrayMap;
	}

	/**
	 * 数组拼接
	 * @param firstArray		   首个数组
	 * @param firstArrayLength     首个数组长度
	 * @param lastArray			   拼接在后的数组
	 * @param lastArrayLength      拼接在后的数组长度
	 * @return 拼接后的数组
	 */
	public static Object[] arrayConcat(Object[] firstArray,int firstArrayLength, Object[] lastArray,int lastArrayLength) {
		if (lastArray.length == 0)
			return firstArray;
		Object[] target = new Object[firstArrayLength + lastArrayLength];
		System.arraycopy(firstArray, 0, target, 0, firstArrayLength);
		System.arraycopy(lastArrayLength, 0, target, firstArrayLength, lastArrayLength);
		return target;
	}


	public static int indexOfArray(Object[] source, Object mark){
		for(int i=0;i<source.length;i++){
			Object item = source[i];
			if(item.equals(mark)){
				return i;
			}
		}
		return -1;
	}

	public static int indexOfArray(Object[] source, Object[] mark){
			if(source.length == 0){
				return -1;
			}

			if(source.length < mark.length){
				return -1;
			}

			int index = -1;

			int i = 0;
			int j = 0;

			while(i <= (source.length - mark.length + j )  ){
				if(!source[i].equals(mark[j]) ){
					if(i == (source.length - mark.length + j )){
						break;
					}

					int pos = -1;
					for(int p = mark.length-1 ; p >= 0; p--){
						if(mark[p] == source[i+mark.length-j]){
							pos = p ;
						}
					}

					if( pos== -1){
						i = i + mark.length + 1 - j;
						j = 0 ;
					}else{
						i = i + mark.length - pos - j;
						j = 0;
					}
				}else{
					if(j == (mark.length - 1)){
						i = i - j + 1 ;
						j = 0;
						index  = i-j - 1;
						break;
					}else{
						i++;
						j++;
					}
				}
			}

			return index;
	}

	public static <T> T clone(T obj) throws ReflectiveOperationException, ParseException {
		Map dataMap = TReflect.getMapfromObject(obj);
		return (T)TReflect.getObjectFromMap(obj.getClass(),dataMap, false);
	}

}
