package org.voovan.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TObject {
	/**
	 * 类型转换
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj){
		return (T)obj;
	}
	
	/**
	 * 转换成指定类型
	 * @param obj
	 * @param t
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object obj,Class<T> t){
		return (T)obj;
	}
	
	/**
	 * 空指针默认值
	 * @param source
	 * @param value
	 * @return
	 */
	public static <T>T nullDefault(T source,T defValue){
		return source!=null?source:defValue;
	}
	
	/**
	 * 初始化一个 List
	 * @param objs
	 * @return
	 */
	public static List<Object> newList(Object ...objs){
		Vector<Object> list = new Vector<Object>();
		for(Object obj:objs){
			list.add(obj);
		}
		return list;
	}
	
	/**
	 * 初始化一个 Map
	 * @param objs		key1,value1,key2,value2.....
	 * @return
	 */
	public static Map<Object,Object> newMap(Object ...objs){
		HashMap<Object,Object> map = new HashMap<Object,Object>();
		for(int i=1;i<objs.length;i+=2){
			map.put(objs[i-1], objs[i]);
		}
		return map;
	}
}
