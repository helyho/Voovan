package org.voovan.http.message.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * HTTP 的 header 对象 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Header {
	private HashMap<String, String> headers;
	
	/**
	 * 构造函数
	 */
	public Header(){
		headers = new HashMap<String,String>();
	} 
	
	/**
	 * 获取 Header  的 Map 对象
	 * @return
	 */
	public HashMap<String,String> getHeaders() {
		return headers;
	}

	/**
	 * 通过 name  删除 Header值
	 * @param header
	 * @return
	 */
	public String remove(String header){
		return headers.remove(header);
	}
	
	/**
	 * 通过 name  判断 Header 是否存在
	 * @param header
	 * @return
	 */
	public boolean contain(String header){
		return headers.containsKey(header);
	}
	
	/**
	 * 通过 name 获取 Header值
	 * @param header
	 * @return
	 */
	public String get(String header){
		return headers.get(header);
	}
	
	/**
	 * 填充 Header
	 * @param header
	 * @param value
	 * @return
	 */
	public String put(String header,String value){
		return headers.put(header,value);
	}
	
	/**
	 * 填充 Header
	 * @param header
	 * @param value
	 * @return
	 */
	public void putAll(Map<String, String> valueMap){
		headers.putAll(valueMap);
	}
	
	/**
	 * Header的个数
	 * @return
	 */
	public int size(){
		return headers.size();
	}
	
	/**
	 * 清空头
	 */
	public void clear(){
		headers.clear();
	}
	
	@Override 
	public String toString(){
		String headerContent = "";
		for(Entry<String,String> headerItemEntry : this.headers.entrySet()){
			String key = headerItemEntry.getKey();
			String value = headerItemEntry.getValue();
			if(!key.isEmpty() && Character.isUpperCase(key.charAt(0))){
				headerContent = headerContent+key+": "+value+"\r\n";
			}
		}
		return headerContent;
	}
}
