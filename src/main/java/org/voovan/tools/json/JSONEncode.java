package org.voovan.tools.json;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.voovan.tools.TObject;
import org.voovan.tools.TReflect;

/**
 * JSON打包类
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class JSONEncode {
	/**
	 * 分析自定义对象为JSON字符串
	 * 
	 * @param object  	自定义对象
	 * @return  		JSON字符串
	 * @throws Exception
	 */
	private static String complexObject(Object object) throws Exception
	{
		return mapObject(TReflect.getMapfromObject(object));
	}
	
	/**
	 * 分析Map对象为JSON字符串
	 * @param mapObject  	map对象
	 * @return 				JSON字符串
	 * @throws Exception
	 */
	private static String mapObject(Map<?,?> mapObject) throws Exception
	{
		String mapString = "{";
		String ContentString = "";
		
		Object[] keys = mapObject.keySet().toArray();
		
		for(Object mapkey:keys)
		{	
			Object mapValue = mapObject.get(mapkey);
			String Value = "";
			Value = fromObject(mapValue); 
			ContentString = ContentString+"\""+TObject.cast(mapkey)+"\":"+Value+",";
		}
		
		if(!ContentString.trim().equals(""))
			ContentString = ContentString.substring(0, ContentString.length()-1);

		mapString = mapString + ContentString + "}";

		return mapString;
	}
	
	/**
	 * 分析Collection对象为JSON字符串
	 * @param mapObject 		List对象
	 * @return 					JSON字符串
	 * @throws Exception 
	 */
	private static String CollectionObject(List<Object> listObject) throws Exception
	{
		return arrayObject(listObject.toArray());
	}

	/**
	 * 分析Array对象为JSON字符串
	 * @param arrayObject 		Array对象
	 * @return					JSON字符串
	 * @throws Exception 
	 */
	private static String arrayObject(Object[] arrayObject) throws Exception
	{
		String arrayString = "[";
		String ContentString = "";

		for(Object object:arrayObject)
		{
			String Value = "";		
			Value = fromObject(object); 			
			ContentString = ContentString+Value+",";
		}
		
		if(!ContentString.trim().equals(""))
			ContentString = ContentString.substring(0, ContentString.length()-1);
		
		arrayString = arrayString+ContentString + "]";
		return arrayString;
	}
	
	/**
	 * 将对象转换成JSON字符串
	 * @param object 			要转换的对象
	 * @return 类型:String 		对象对应的JSON字符串
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static String fromObject(Object object) throws Exception
	{
		String value = "";

		
		if(object instanceof String)
		{
			value = "\""+object.toString()+"\"";
		}
		else if(object.getClass().getName().startsWith("java.lang"))
		{
			value = object.toString();
		}
		else if(object instanceof Map)
		{
			Map<Object,Object> mapObject = (Map<Object,Object>)object;
			value = mapObject(mapObject);
		}
		
		else if(object instanceof Collection)
		{
			List<Object> listObject = (List<Object>)object;
			value = CollectionObject(listObject);
		}

		else if(object instanceof Object[])
		{
			Object[] arrayObject = TObject.cast(object);
			value = arrayObject(arrayObject);
		}
		else 
		{
			value = complexObject(object);
		}
			
		return value;
	}
}
