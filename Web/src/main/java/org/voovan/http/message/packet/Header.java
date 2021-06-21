package org.voovan.http.message.packet;

import org.voovan.http.message.HttpStatic;
import org.voovan.tools.FastThreadLocal;
import org.voovan.tools.json.JSON;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * HTTP 的 header 对象
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Header {
	private static FastThreadLocal<StringBuilder> THREAD_STRING_BUILDER = FastThreadLocal.withInitial(()->new StringBuilder(512));

	private Map<String, String> headers;

	/**
	 * 构造函数
	 */
	public Header(){
		headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	}

	/**
	 * 获取 Header  的 Map 对象
	 * @return HTTP-Header 转换候的 Map
	 */
	public Map<String,String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	/**
	 * 通过 name  删除 Header值
	 * @param header header 的 name
	 * @return 移除的header 的 name
	 */
	public String remove(String header){
		return headers.remove(header);
	}

	/**
	 * 通过 name  判断 Header 是否存在
	 * @param header header 的 name
	 * @return 是否存在
	 */
	public boolean contain(String header){
		return headers.containsKey(header);
	}

	/**
	 * 通过 name 获取 Header值
	 * @param header header 的 name
	 * @return header 的值
	 */
	public String get(String header){
		return headers.get(header);
	}

	/**
	 * 填充 Header
	 * @param header header 的 name
	 * @param value header 的 nam值
	 * @return header 的 name
	 */
	public String put(String header,String value){
		return headers.put(header,value);
	}

	/**
	 * 填充 Header, 如果不存在则设置
	 * @param header header 的 name
	 * @param value header 的 nam值
	 * @return header 的 name
	 */
	public String putIfAbsent(String header,String value){
		return headers.putIfAbsent(header,value);
	}

	/**
	 * 填充 Header
	 * @param valueMap Header 的 Map 形式
	 */
	public void putAll(Map<String, String> valueMap){
		headers.putAll(valueMap);
	}

	/**
	 * Header的个数
	 * @return header 元素的数量
	 */
	public int size(){
		return headers.size();
	}

	public Header copyFrom(Header otherHeader) {
		this.putAll(otherHeader.getHeaders());
		return this;
	}

	/**
	 * 清空头
	 */
	public void clear(){
		headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	}

	@Override
	public String toString(){
		StringBuilder headerContent = THREAD_STRING_BUILDER.get();
		headerContent.setLength(0);

		for(Entry<String,String> headerItemEntry : this.headers.entrySet()){
			String key = headerItemEntry.getKey();
			String value = headerItemEntry.getValue();
			if(!key.isEmpty()){
				headerContent.append(key);
				headerContent.append(HttpStatic.HEADER_SPLITER_STRING);
				headerContent.append(value);
				headerContent.append(HttpStatic.LINE_MARK_STRING);
			}
		}
		return headerContent.toString();
	}
}
